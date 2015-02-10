package br.ufes.inf.lprm.ninjabubble;

import android.app.Service;
import android.util.Log;

import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.json.JSONObject;

/**
 * Created by Juan on 10/02/2015.
 */
public class MapperListener implements ChatMessageListener {

    private String TAG = "NinjaBubbleMagic::MapperListener";

    public Service mService;

    public MapperListener(Service service) {
        mService = service;
    }

    @Override
    public void processMessage(Chat chat, Message message) {
        try {
            JSONObject parsed = new JSONObject(message.getBody());
            Log.i(TAG, parsed.getString("func"));
        }
        catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }


}
