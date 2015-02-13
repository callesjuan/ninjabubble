package br.ufes.inf.lprm.ninjabubble.br.ufes.inf.lprm.ninjabubble.views;

import android.content.Context;
import android.widget.LinearLayout;

/**
 * Created by Juan on 11/02/2015.
 */
public class ChatView extends LinearLayout {
    public OverlayView mOverlayView;

    public HomeView vHome;
    public MinimapView vMinimap;
    public PartyView vParty;

    public ChatView(Context context, OverlayView overlayView) {
        super(context);
        mOverlayView = overlayView;
    }

    public void setFamily(HomeView homeView, MinimapView minimapView, PartyView partyView) {
        vHome = homeView;
        vMinimap = minimapView;
        vParty = partyView;
    }

    public void show() {
        mOverlayView.mContentLayout.removeAllViews();
        mOverlayView.mContentLayout.addView(this);
    }
}
