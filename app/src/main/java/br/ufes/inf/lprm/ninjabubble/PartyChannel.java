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

        leave();

        String group_fulljid = String.format("%s@conference.%s", group_jid, mService.mDomain);

        MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(mService.mXmppConnection);
        mChat = manager.getMultiUserChat(group_fulljid);

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
}
