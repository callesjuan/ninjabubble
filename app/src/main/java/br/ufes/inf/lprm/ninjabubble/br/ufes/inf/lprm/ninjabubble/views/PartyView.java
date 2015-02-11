package br.ufes.inf.lprm.ninjabubble.br.ufes.inf.lprm.ninjabubble.views;

import android.content.Context;
import android.widget.LinearLayout;

/**
 * Created by Juan on 09/02/2015.
 */
public class PartyView extends LinearLayout {
    public OverlayView mOverlayView;

    public HomeView vHome;
    public MinimapView vMinimap;
    public ChatView vChat;

    public PartyView(Context context) {
        super(context);
    }

    public void setFamily(OverlayView overlayView, HomeView homeView, MinimapView minimapView, ChatView chatView) {
        mOverlayView = overlayView;
        vHome = homeView;
        vMinimap = minimapView;
        vChat = chatView;
    }
}
