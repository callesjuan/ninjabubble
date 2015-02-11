package br.ufes.inf.lprm.ninjabubble.br.ufes.inf.lprm.ninjabubble.views;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import br.ufes.inf.lprm.ninjabubble.R;

/**
 * Created by Juan on 09/02/2015.
 */
public class HomeView extends LinearLayout {
    public final String TAG = "NinjaBubbleMagic/HomeView";

    public OverlayView mOverlayView;

    public MinimapView vMinimap;
    public ChatView vChat;
    public PartyView vParty;

    public Button bStreamInit;
    public Button bStreamPause;
    public Button bStreamResume;
    public Button bStreamClose;

    public Button bGroupMatch;  // works both for streamInit (new streams) and groupJoin (running streams)
    public Button bGroupLeave;

    public HomeView(Context context) {
        super(context);

        Log.i(TAG, "created");

        bStreamInit = new Button(getContext());
        bStreamInit.setText(R.string.btn_stream_init);
        bStreamInit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mOverlayView.mService.mMapperChannel.streamInit();
                }
                catch (Exception e) {

                }
            }
        });
        addView(bStreamInit);

        bStreamPause = new Button(getContext());
        bStreamPause.setText(R.string.btn_stream_pause);

        bStreamResume = new Button(getContext());
        bStreamResume.setText(R.string.btn_stream_resume);

        bStreamClose = new Button(getContext());
        bStreamClose.setText(R.string.btn_stream_close);
    }

    public void setFamily(OverlayView overlayView, MinimapView minimapView, ChatView chatView, PartyView partyView) {
        mOverlayView = overlayView;
        vMinimap = minimapView;
        vChat = chatView;
        vParty = partyView;
    }
}
