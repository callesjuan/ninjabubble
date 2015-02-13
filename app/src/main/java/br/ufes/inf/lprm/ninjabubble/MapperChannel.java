package br.ufes.inf.lprm.ninjabubble;

import android.app.Service;
import android.util.Log;

import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * Created by Juan on 10/02/2015.
 */
public class MapperChannel implements ChatMessageListener {

    private String TAG = "NinjaBubbleMagic/MapperListener";

    public NinjaBubbleMagic mService;
    public Chat mChat;

    public Object mLocker = new Object();
    public boolean mWaitingReply = false;
    public long mTimeout = 1000 * 30;

    public MapperChannel(NinjaBubbleMagic service) {
        mService = service;

        ChatManager chatManager = ChatManager.getInstanceFor(mService.mXmppConnection);
        mChat = chatManager.createChat(mService.mMapperJID, this);
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
        }
        catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * streamStatus
     */
    public void streamStatus() throws Exception {
        try {
            String msgBody = new JSONStringer()
                    .object()
                    .key("func").value("stream_status")
                    .key("args").object().endObject()
                    .endObject()
                    .toString();

            Log.i(TAG, msgBody);
            mChat.sendMessage(msgBody);

            synchronized (mLocker) {
                mWaitingReply = true;
                mLocker.wait(mTimeout);
                if (mWaitingReply) {
                    mWaitingReply = false;
                    throw new Exception("message timed out");
                }
            }
        }
        catch (Exception e) {
            Log.e(TAG, "trouble in streamStatus", e);
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
            mService.mStream = args.getJSONObject("stream");
            mService.mStreamStatusRequired = false;

            synchronized (mLocker) {
                mWaitingReply = false;
                mLocker.notify();
            }
        }
        catch (Exception e) {
            Log.e(TAG, "trouble in streamStatusReply", e);
        }
    }

    /**
     * streamInit
     */
    public void streamInit() {
        Log.i(TAG, "streamInit called");
    }

    public void streamInitReply() {

    }

    /**
     * streamPause
     */
    public void streamPause() {
        Log.i(TAG, "streamPause called");
    }

    public void streamPauseReply() {

    }

    /**
     * streamResume
     */
    public void streamResume() {
        Log.i(TAG, "streamResume called");
    }

    public void streamResumeReply() {

    }

    /**
     * streamClose
     */
    public void streamClose () {
        Log.i(TAG, "streamClose called");
    }

    public void streamCloseReply() {

    }
}
