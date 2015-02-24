package br.ufes.inf.lprm.ninjabubble;

import android.util.Log;

import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Juan on 11/02/2015.
 */
public class PartyChannel implements MessageListener {

    private String TAG = "NinjaBubbleMagic/PartyListener";

    public NinjaBubbleMagic mService;
    public MultiUserChat mChat;

    public final int PING_DURATION_SECONDS = 60 * 5;
    public final long PING_DURATION_MILLIS = 1000 * 60 * 5;

    public PartyChannel (NinjaBubbleMagic service) {
        mService = service;
    }

    public void join(String group_jid) throws Exception {

        leave();

        String group_fulljid = String.format("%s@conference.%s", group_jid, mService.mDomain);

        MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(mService.mXmppConnection);
        mChat = manager.getMultiUserChat(group_fulljid);
        mChat.addMessageListener(this);

        try {
            DiscussionHistory history = new DiscussionHistory();
            history.setSeconds(PING_DURATION_SECONDS);

            if (mChat.createOrJoin(mService.mStream.getString("jid"), null, history, SmackConfiguration.getDefaultPacketReplyTimeout())) {
                mChat.sendConfigurationForm(new Form(DataForm.Type.submit));
            }

            mService.runConcurrentThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Message old = mChat.nextMessage();
                        while (old != null) {
                            try {
                                processMessage(old);
                            } catch (Exception e) {
                            }
                            old = mChat.nextMessage();
                        }
                    } catch (Exception e) {}
                }
            });
        }
        catch (Exception e) {
            Log.e(TAG, String.format("trouble joining muc room %s", group_fulljid), e);
            throw e;
        }
    }

    public void leave() {
        if(mChat != null) {
            try {
                mChat.leave();
            }
            catch (Exception e) {
            }
            mChat = null;
        }
    }

    @Override
    public void processMessage(Message message) {
        DelayInformation delay = null;
        try {
            delay = (DelayInformation)message.getExtension("x", "jabber:x:delay");
        } catch (Exception e) {}

        try {
            JSONObject parsed = new JSONObject(message.getBody());
            JSONObject args = parsed.getJSONObject("args");
            args.put("from", message.getFrom());
            args.put("to", message.getTo());

            if(parsed.getString("func").contains("ping")) {
                long dismissTime;
                if (delay != null) {
                    long delayDiff = System.currentTimeMillis() - delay.getStamp().getTime();
                    if (delayDiff > PING_DURATION_MILLIS || delayDiff < 0) {
                        return;
                    } else {
                        dismissTime = System.currentTimeMillis() + (PING_DURATION_MILLIS - delayDiff);
                    }
                } else {
                    dismissTime = System.currentTimeMillis() + PING_DURATION_MILLIS;
                }
                args.put("dismiss", dismissTime);
            }

            if (parsed.getString("func").equals("ping_target")) {
                pingTargetIn(args);
            }
            else if (parsed.getString("func").equals("ping_assist")) {
                pingAssistIn(args);
            }
            else if (parsed.getString("func").equals("ping_danger")) {
                pingDangerIn(args);
            }
        }
        catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public void pingTargetIn(JSONObject args) {
        Log.i(TAG, args.toString());

        if(mService.mOverlayView.vMinimap != null) {
            try {
                mService.mOverlayView.vMinimap.loadPing(mService.mOverlayView.vMinimap.PING_TARGET, args.getJSONArray("target_latlng"), args.getString("details"), args.getString("stream_id"), args.getLong("dismiss"));
                mService.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mService.mOverlayView.vMinimap.mMapView.invalidate();
                        mService.mOverlayView.imMinimap.setImageBitmap(mService.mOverlayView.mBmpMinimapNew);
                    }
                });
            } catch (Exception e) {}
        }
    }
    public void pingAssistIn(JSONObject args) {
        Log.i(TAG, args.toString());

        if(mService.mOverlayView.vMinimap != null) {
            try {
                mService.mOverlayView.vMinimap.loadPing(mService.mOverlayView.vMinimap.PING_ASSIST, args.getJSONArray("assist_latlng"), args.getString("details"), args.getString("stream_id"), args.getLong("dismiss"));
                mService.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mService.mOverlayView.vMinimap.mMapView.invalidate();
                        mService.mOverlayView.imMinimap.setImageBitmap(mService.mOverlayView.mBmpMinimapNew);
                    }
                });
            } catch (Exception e) {}
        }
    }
    public void pingDangerIn(JSONObject args) {
        Log.i(TAG, args.toString());

        if(mService.mOverlayView.vMinimap != null) {
            try {
                mService.mOverlayView.vMinimap.loadPing(mService.mOverlayView.vMinimap.PING_DANGER, args.getJSONArray("danger_latlng"), args.getString("details"), args.getString("stream_id"), args.getLong("dismiss"));
                mService.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mService.mOverlayView.vMinimap.mMapView.invalidate();
                        mService.mOverlayView.imMinimap.setImageBitmap(mService.mOverlayView.mBmpMinimapNew);
                    }
                });

            } catch (Exception e) {}
        }
    }

    public void pingTargetOut(JSONArray latlng, String details) throws Exception {
        Log.i(TAG, "pingTargetOut");

        try {

            JSONObject msg = new JSONObject()
                .put("func", "ping_target")
                .put("args", new JSONObject()
                    .put("stream_id", mService.mStream.getString("stream_id"))
                    .put("target_latlng", latlng)
                    .put("details", details)
                );

            Log.i(TAG, msg.toString());
            mChat.sendMessage(msg.toString());
        }
        catch (Exception e) {
            Log.e(TAG, "pingTargetOut", e);
            throw e;
        }
    }

    public void pingAssistOut(JSONArray latlng, String details) throws Exception {
        Log.i(TAG, "pingAssistOut");

        try {

            JSONObject msg = new JSONObject()
                    .put("func", "ping_assist")
                    .put("args", new JSONObject()
                                    .put("stream_id", mService.mStream.getString("stream_id"))
                                    .put("assist_latlng", latlng)
                                    .put("details", details)
                    );

            Log.i(TAG, msg.toString());
            mChat.sendMessage(msg.toString());
        }
        catch (Exception e) {
            Log.e(TAG, "pingAssistOut", e);
            throw e;
        }
    }

    public void pingDangerOut(JSONArray latlng, String details) throws Exception {
        Log.i(TAG, "pingDangerOut");

        try {

            JSONObject msg = new JSONObject()
                    .put("func", "ping_danger")
                    .put("args", new JSONObject()
                                    .put("stream_id", mService.mStream.getString("stream_id"))
                                    .put("danger_latlng", latlng)
                                    .put("details", details)
                    );

            Log.i(TAG, msg.toString());
            mChat.sendMessage(msg.toString());
        }
        catch (Exception e) {
            Log.e(TAG, "pingDangerOut", e);
            throw e;
        }
    }
}
