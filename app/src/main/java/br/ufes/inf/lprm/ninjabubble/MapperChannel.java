package br.ufes.inf.lprm.ninjabubble;

import android.app.Service;
import android.content.Intent;
import android.util.Log;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Juan on 10/02/2015.
 */
public class MapperChannel implements ChatMessageListener {

    private String TAG = "NinjaBubbleMagic/MapperListener";

    public NinjaBubbleMagic mService;
    public Chat mChat;

    public Object mLocker = new Object();
    public boolean mWaitingReply = false;
    public boolean mReplyError = false;
    public long mTimeout = 1000 * 30;

    final String DATE_PATTERN = "yyyy-MM-dd'T'hh:mm:ss.SSS";
    final String STAMP_PATTERN = "yyyyMMddhhmmss";

    public MapperChannel(NinjaBubbleMagic service) {
        mService = service;
    }

    public void connect() throws Exception {
        try {
            // Connect to XMPP server
            XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder();
            configBuilder.setUsernameAndPassword(mService.mNick, mService.mPWD);
            configBuilder.setServiceName(mService.mDomain);
            configBuilder.setResource("device");
            configBuilder.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);

            mService.mXmppConnection = new XMPPTCPConnection(configBuilder.build());
            mService.mXmppConnection.connect();
            mService.mXmppConnection.login();
            Log.i(TAG, "logged into XMPP server");

            ChatManager chatManager = ChatManager.getInstanceFor(mService.mXmppConnection);
            mChat = chatManager.createChat(mService.mMapperJID, this);

        } catch (Exception e) {
            throw e;
        }
    }

    public void disconnect() {
        try {
            mService.mXmppConnection.disconnect();
        } catch (Exception e) {
        }
    }

    public void lock() throws Exception {
        synchronized (mLocker) {
            mWaitingReply = true;
            try {
                mLocker.wait(mTimeout);
                if (mWaitingReply) {
                    mWaitingReply = false;
                    throw new Exception("message timed out");
                }
                if (mReplyError) {
                    mReplyError = false;
                    throw new Exception("trouble handling reply");
                }
            }
            catch (Exception e) {
                throw e;
            }
        }
    }

    private void unlock() {
        synchronized (mLocker) {
            mWaitingReply = false;
            mLocker.notify();
        }
    }

    private void unlock(boolean replyError) {
        if (replyError) {
            mReplyError = true;
        }
        unlock();
    }

    @Override
    public void processMessage(Chat chat, Message message) {
        try {
            JSONObject parsed = new JSONObject(message.getBody());
            JSONObject args = parsed.getJSONObject("args");
            args.put("from", message.getFrom());
            args.put("to", message.getTo());
            if (parsed.getString("func").equals("stream_status_reply")) {
                streamStatusReply(args);
            }
            if (parsed.getString("func").equals("stream_init_reply")) {
                streamInitReply(args);
            }
            if (parsed.getString("func").equals("stream_pause_reply")) {
                streamPauseReply(args);
            }
            if (parsed.getString("func").equals("stream_resume_reply")) {
                streamResumeReply(args);
            }
            if (parsed.getString("func").equals("stream_close_reply")) {
                streamCloseReply(args);
            }
        }
        catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * streamStatus
     */
    public void streamStatus() throws Exception {
        Log.i(TAG, "streamStatus");

        try {
            JSONObject msg = new JSONObject()
                .put("func", "stream_status")
                .put("args", new JSONObject());

            Log.i(TAG, msg.toString());
            mChat.sendMessage(msg.toString());

            lock();
        }
        catch (Exception e) {
            Log.e(TAG, "streamStatus", e);
            throw e;
        }
    }

    public void streamStatusReply(JSONObject args) {
        try {
            if (!mWaitingReply) {
                return;
            }

            Log.i(TAG, "streamStatusReply");
            Log.i(TAG, args.toString());
            mService.mSource = args.getJSONObject("source");
            if (args.has("stream")) {
                mService.mStream = args.getJSONObject("stream");
            }

            unlock();
        }
        catch (Exception e) {
            Log.e(TAG, "streamStatusReply", e);
            unlock(true);
        }
    }

    /**
     * streamInit
     */
    public void streamInit(String media, String groupJid)throws Exception {
        Log.i(TAG, "streamInit");

        try {
            Date now = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
            SimpleDateFormat stampFormat = new SimpleDateFormat(STAMP_PATTERN);

            String streamId = mService.mNick + "__" + stampFormat.format(now);
            if (groupJid == null) {
                groupJid = streamId + "__" + stampFormat.format(now);
            }
            String stamp = stampFormat.format(now);

            JSONObject msg = new JSONObject()
                .put("func", "stream_init")
                .put("args", new JSONObject()
                    .put("stream_id", streamId)
                    .put("group_jid", groupJid)
                    .put("hashtags", mService.mHashtags)
                    .put("latlng", mService.mLatlng)
                    .put("media", mService.mMedia)
                    .put("stamp", stamp)
                )
            ;

            Log.i(TAG, msg.toString());
            mChat.sendMessage(msg.toString());

            lock();
        }
        catch (Exception e) {
            Log.e(TAG, "streamInit", e);
            throw e;
        }
    }

    public void streamInitReply(JSONObject args) {
        try {
            mService.mStream = args.getJSONObject("stream");
            unlock();
        }
        catch (Exception e) {
            Log.e(TAG, "streamInitReply", e);
            unlock(true);
        }
    }

    /**
     * streamPause
     */
    public void streamPause() throws Exception {
        Log.i(TAG, "streamPause");

        try {
            JSONObject msg = new JSONObject()
                .put("func", "stream_pause")
                .put("args", new JSONObject()
                    .put("stream_id", mService.mStream.getString("stream_id"))
                )
            ;

            Log.i(TAG, msg.toString());
            mChat.sendMessage(msg.toString());

            lock();
        }
        catch (Exception e) {
            Log.e(TAG, "streamPause", e);
            throw e;
        }
    }

    public void streamPauseReply(JSONObject args) {
        try {
            mService.mStream = args.getJSONObject("stream");
            unlock();
        }
        catch (Exception e) {
            Log.e(TAG, "streamPauseReply", e);
            unlock(true);
        }
    }

    /**
     * streamResume
     */
    public void streamResume() throws Exception {
        Log.i(TAG, "streamResume");

        try {
            JSONObject msg = new JSONObject()
                .put("func", "stream_resume")
                .put("args", new JSONObject()
                    .put("stream_id", mService.mStream.getString("stream_id"))
                )
            ;

            Log.i(TAG, msg.toString());
            mChat.sendMessage(msg.toString());

            lock();
        }
        catch (Exception e) {
            Log.e(TAG, "streamResume", e);
            throw e;
        }
    }

    public void streamResumeReply(JSONObject args) {
        try {
            mService.mStream = args.getJSONObject("stream");
            unlock();
        }
        catch (Exception e) {
            Log.e(TAG, "streamResumeReply", e);
            unlock(true);
        }
    }

    /**
     * streamClose
     */
    public void streamClose () throws Exception{
        Log.i(TAG, "streamClose");

        try {
            JSONObject msg = new JSONObject()
                .put("func", "stream_close")
                .put("args", new JSONObject()
                    .put("stream_id", mService.mStream.getString("stream_id"))
                )
            ;

            Log.i(TAG, msg.toString());
            mChat.sendMessage(msg.toString());

            lock();
        }
        catch (Exception e) {
            Log.e(TAG, "streamClose", e);
            throw e;
        }
    }

    public void streamCloseReply(JSONObject args) {
        try {
            if (args.has("stream")) {
                mService.mStream = null;
                unlock();
            }
            else {
                throw new Exception();
            }
        }
        catch (Exception e) {
            Log.e(TAG, "streamCloseReply", e);
            unlock(true);
        }
    }

    /**
     * updateTwitcastingId
     */
    public void updateTwitcastingId(String twitcastingId) throws Exception {
        Log.i(TAG, "updateTwitcastingId");

        try {

            JSONObject msg = new JSONObject()
                .put("func", "update_twitcasting_id")
                .put("args", new JSONObject()
                    .put("twitcasting_id", twitcastingId)
                )
            ;

            Log.i(TAG, msg.toString());
            mChat.sendMessage(msg.toString());
        }
        catch (Exception e) {
            Log.e(TAG, "updateTwitcastingId", e);
            throw e;
        }
    }

    /**
     * groupMatch
     */
    public void groupMatch (double radius) {
        Log.i(TAG, "groupMatch");

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void groupMatchReply() {

    }
}
