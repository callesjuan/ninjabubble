package br.ufes.inf.lprm.ninjabubble.br.ufes.inf.lprm.ninjabubble.views;

import android.content.Context;
import android.widget.LinearLayout;

/**
 * Created by Juan on 09/02/2015.
 */
public class PartyView extends ContentView {

    public HomeView vHome;
    public MinimapView vMinimap;
    public ChatView vChat;

    public PartyView(Context context, OverlayView overlayView) {
        super(context, overlayView);
    }

    public void setFamily(HomeView homeView, MinimapView minimapView, ChatView chatView) {
        vHome = homeView;
        vMinimap = minimapView;
        vChat = chatView;
    }
}
