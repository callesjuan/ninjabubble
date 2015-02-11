package br.ufes.inf.lprm.ninjabubble;

import android.util.Log;

import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.packet.DataForm;

/**
 * Created by Juan on 11/02/2015.
 */
public class PartyChannel {

    private String TAG = "NinjaBubbleMagic/MapperListener";

    public NinjaBubbleMagic mService;
    public MultiUserChat mChat;

    public PartyChannel (NinjaBubbleMagic service) {
        mService = service;
    }

    public void join(String group_jid) throws Exception {
        MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(mService.mXmppConnection);
        mChat = manager.getMultiUserChat(group_jid);

        try {
            if (mChat.createOrJoin(mService.mStream.getString("jid"))) {
                mChat.sendConfigurationForm(new Form(DataForm.Type.submit));
            }
        }
        catch (Exception e) {
            Log.e(TAG, "trouble joining muc room", e);
            throw e;
        }
    }
}
