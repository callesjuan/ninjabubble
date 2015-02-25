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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import br.ufes.inf.lprm.ninjabubble.br.ufes.inf.lprm.ninjabubble.views.PingMarker;

/**
 * Created by Juan on 11/02/2015.
 */
public class PartyChannel implements MessageListener {

    private String TAG = "NinjaBubbleMagic/PartyListener";

    public NinjaBubbleMagic mService;
    public MultiUserChat mChat;

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
            if (mChat.createOrJoin(mService.mStream.getString("jid"))) {
                mChat.sendConfigurationForm(new Form(DataForm.Type.submit));
            }
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
            //Log.e(TAG, e.getMessage(), e);
            chatMessageIn(message);
        }
    }

    public void chatMessageIn(final Message message) {
        if (mService.mOverlayView.vChat != null && mService.mOverlayView.vChat.mAdapter != null) {
            mService.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mService.mOverlayView.vChat.mAdapter.add(message.getBody());
                        mService.mOverlayView.vChat.mAdapter.notifyDataSetChanged();
                        mService.mOverlayView.vChat.mListView.setSelection(mService.mOverlayView.vChat.mListView.getCount()-1);
                    } catch (Exception e) {
                        Log.e(TAG, "chatMessageIn", e);
                    }
                }
            });
        }
    }

    public void chatMessageOut(String message) {
        try {
            Date now = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            mChat.sendMessage(mService.mSource.getString("jid")+" ("+sdf.format(now)+"):"+System.getProperty("line.separator")+"  "+message);
        } catch (Exception e) {
            Log.e(TAG, "chatMessageOut", e);
        }
    }

    public void pingTargetIn(JSONObject args) {
        Log.i(TAG, "pingTargetIn:"+args.toString());

        if(mService.mOverlayView.vMinimap != null) {
            try {
                final PingMarker ping = mService.mOverlayView.vMinimap.addPing(mService.mOverlayView.vMinimap.PING_TARGET, args.getJSONArray("target_latlng"), args.getString("details"), args.getString("stream_id"), args.getLong("stamp"));
                mService.mOverlayView.vMinimap.mMapView.getOverlays().add(ping);
                mService.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mService.mOverlayView.vMinimap.mMapView.invalidate();
                        ping.showInfoWindow();
                        mService.mOverlayView.imMinimap.setImageBitmap(mService.mOverlayView.mBmpMinimapNew);
                    }
                });
            } catch (Exception e) {}
        }
    }

    public void pingTargetOut(JSONArray latlng, String details) throws Exception {
        Log.i(TAG, "pingTargetOut");

        try {
            Date sinceDate = new Date(System.currentTimeMillis());
            SimpleDateFormat stampFormat = new SimpleDateFormat(mService.mMapperChannel.STAMP_PATTERN);
            String stamp = stampFormat.format(sinceDate);

            JSONObject msg = new JSONObject()
                .put("func", "ping_target")
                .put("args", new JSONObject()
                    .put("stream_id", mService.mStream.getString("stream_id"))
                    .put("target_latlng", latlng)
                    .put("details", details)
                    .put("author", mService.mNick)
                    .put("stamp", stamp)
                );

            Log.i(TAG, msg.toString());
            mChat.sendMessage(msg.toString());
        }
        catch (Exception e) {
            Log.e(TAG, "pingTargetOut", e);
            throw e;
        }
    }

    public void pingAssistIn(JSONObject args) {
        Log.i(TAG, args.toString());

        if(mService.mOverlayView.vMinimap != null) {
            try {
                final PingMarker ping = mService.mOverlayView.vMinimap.addPing(mService.mOverlayView.vMinimap.PING_ASSIST, args.getJSONArray("assist_latlng"), args.getString("details"), args.getString("stream_id"), args.getLong("stamp"));
                mService.mOverlayView.vMinimap.mMapView.getOverlays().add(ping);
                mService.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mService.mOverlayView.vMinimap.mMapView.invalidate();
                        ping.showInfoWindow();
                        mService.mOverlayView.imMinimap.setImageBitmap(mService.mOverlayView.mBmpMinimapNew);
                    }
                });
            } catch (Exception e) {}
        }
    }

    public void pingAssistOut(JSONArray latlng, String details) throws Exception {
        Log.i(TAG, "pingAssistOut");

        try {
            Date sinceDate = new Date(System.currentTimeMillis());
            SimpleDateFormat stampFormat = new SimpleDateFormat(mService.mMapperChannel.STAMP_PATTERN);
            String stamp = stampFormat.format(sinceDate);

            JSONObject msg = new JSONObject()
                    .put("func", "ping_assist")
                    .put("args", new JSONObject()
                                    .put("stream_id", mService.mStream.getString("stream_id"))
                                    .put("assist_latlng", latlng)
                                    .put("details", details)
                                    .put("author", mService.mNick)
                                    .put("stamp", stamp)
                    );

            Log.i(TAG, msg.toString());
            mChat.sendMessage(msg.toString());
        }
        catch (Exception e) {
            Log.e(TAG, "pingAssistOut", e);
            throw e;
        }
    }

    public void pingDangerIn(JSONObject args) {
        Log.i(TAG, args.toString());

        if(mService.mOverlayView.vMinimap != null) {
            try {
                final PingMarker ping = mService.mOverlayView.vMinimap.addPing(mService.mOverlayView.vMinimap.PING_DANGER, args.getJSONArray("danger_latlng"), args.getString("details"), args.getString("stream_id"), args.getLong("stamp"));
                mService.mOverlayView.vMinimap.mMapView.getOverlays().add(ping);
                mService.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mService.mOverlayView.vMinimap.mMapView.invalidate();
                        ping.showInfoWindow();
                        mService.mOverlayView.imMinimap.setImageBitmap(mService.mOverlayView.mBmpMinimapNew);
                    }
                });

            } catch (Exception e) {}
        }
    }

    public void pingDangerOut(JSONArray latlng, String details) throws Exception {
        Log.i(TAG, "pingDangerOut");

        try {
            Date sinceDate = new Date(System.currentTimeMillis());
            SimpleDateFormat stampFormat = new SimpleDateFormat(mService.mMapperChannel.STAMP_PATTERN);
            String stamp = stampFormat.format(sinceDate);

            JSONObject msg = new JSONObject()
                    .put("func", "ping_danger")
                    .put("args", new JSONObject()
                                    .put("stream_id", mService.mStream.getString("stream_id"))
                                    .put("danger_latlng", latlng)
                                    .put("details", details)
                                    .put("author", mService.mNick)
                                    .put("stamp", stamp)
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
