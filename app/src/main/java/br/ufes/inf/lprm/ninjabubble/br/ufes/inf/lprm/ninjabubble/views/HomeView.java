package br.ufes.inf.lprm.ninjabubble.br.ufes.inf.lprm.ninjabubble.views;

import android.content.Context;
import android.widget.LinearLayout;

/**
 * Created by Juan on 09/02/2015.
 */
public class HomeView extends LinearLayout {
    public OverlayView mOverlayView;

    public MinimapView vMinimap;
    public PartyView vParty;

    public HomeView(Context context) {
        super(context);
    }

    public void setFamily(OverlayView overlayView, MinimapView minimapView, PartyView partyView) {
        mOverlayView = overlayView;
        vMinimap = minimapView;
        vParty = partyView;
    }
}
