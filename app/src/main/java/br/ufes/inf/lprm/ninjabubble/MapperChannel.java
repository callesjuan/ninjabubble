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

    private String TAG = "NinjaBubbleMagic::MapperListener";

    public NinjaBubbleMagic mService;
    public Chat mChat;

    public Object mLocker = new Object();
    public boolean mWaitingReply = false;

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

    /*
    REPLIES
     */
    public void streamStatusReply(JSONObject args) {
        try {
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


    /*
    MESSAGES
     */
    public void streamStatus() {
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
                while (mWaitingReply) {
                    mLocker.wait();
                }
            }
        }
        catch (Exception e) {
            Log.e(TAG, "trouble in streamStatus", e);
        }
    }

}
