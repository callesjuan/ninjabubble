package br.ufes.inf.lprm.ninjabubble.br.ufes.inf.lprm.ninjabubble.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import android.widget.LinearLayout;

import br.ufes.inf.lprm.ninjabubble.R;

/**
 * Created by Juan on 09/02/2015.
 */
public class MinimapView extends ContentView {

    public HomeView vHome;
    public ChatView vChat;
    public PartyView vParty;

    public ImageView imSelf;

    public MinimapView(Context context, OverlayView overlayView) {
        super(context, overlayView);

        imSelf = new ImageView(getContext());
        imSelf.setImageResource(R.drawable.self);

        mContentLayout.addView(imSelf);
    }

    public void setFamily(HomeView homeView, ChatView chatView, PartyView partyView) {
        vHome = homeView;
        vChat = chatView;
        vParty = partyView;
    }
}
