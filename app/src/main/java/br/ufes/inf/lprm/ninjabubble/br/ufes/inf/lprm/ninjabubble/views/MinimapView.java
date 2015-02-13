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

    public MinimapView(Context context, OverlayView overlayView) {
        super(context);
        mOverlayView = overlayView;
    }

    public void setFamily(HomeView homeView, ChatView chatView, PartyView partyView) {
        vHome = homeView;
        vChat = chatView;
        vParty = partyView;
    }

    public void show() {
        mOverlayView.mContentLayout.removeAllViews();
        mOverlayView.mContentLayout.addView(this);
    }
}
