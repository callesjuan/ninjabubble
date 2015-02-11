package br.ufes.inf.lprm.ninjabubble.br.ufes.inf.lprm.ninjabubble.views;

import android.content.Context;
import android.widget.LinearLayout;

/**
 * Created by Juan on 09/02/2015.
 */
public class MinimapView extends LinearLayout {
    public OverlayView mOverlayView;

    public HomeView vHome;
    public ChatView vChat;
    public PartyView vParty;

    public MinimapView(Context context) {
        super(context);
    }

    public void setFamily(OverlayView overlayView, HomeView homeView, ChatView chatView, PartyView partyView) {
        mOverlayView = overlayView;
        vHome = homeView;
        vChat = chatView;
        vParty = partyView;
    }
}
