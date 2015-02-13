package br.ufes.inf.lprm.ninjabubble.br.ufes.inf.lprm.ninjabubble.views;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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

    public HomeView(Context context, OverlayView overlayView) {
        super(context);
        mOverlayView = overlayView;

        Log.i(TAG, "created");

        setGravity(Gravity.CENTER);
        setOrientation(VERTICAL);

        setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        bStreamInit = new Button(getContext());
        bStreamInit.setText(R.string.btn_stream_init);
        bStreamInit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mOverlayView.mService.mMapperChannel.streamInit();

                    bStreamInit.setVisibility(GONE);
                    bStreamPause.setVisibility(VISIBLE);
                    bStreamClose.setVisibility(VISIBLE);

                    mOverlayView.enableMenu();
                }
                catch (Exception e) {
                    Log.e(TAG, "streamInit", e);
                }
            }
        });
        addView(bStreamInit);

        bStreamPause = new Button(getContext());
        bStreamPause.setText(R.string.btn_stream_pause);
        bStreamPause.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mOverlayView.mService.mMapperChannel.streamPause();

                    bStreamPause.setVisibility(GONE);
                    bStreamResume.setVisibility(VISIBLE);

                    mOverlayView.disableMenu();
                }
                catch (Exception e) {
                    Log.e(TAG, "streamPause", e);
                }
            }
        });
        addView(bStreamPause);

        bStreamResume = new Button(getContext());
        bStreamResume.setText(R.string.btn_stream_resume);
        bStreamResume.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mOverlayView.mService.mMapperChannel.streamResume();

                    bStreamPause.setVisibility(VISIBLE);
                    bStreamResume.setVisibility(GONE);

                    mOverlayView.enableMenu();
                }
                catch (Exception e) {
                    Log.e(TAG, "streamResume", e);
                }
            }
        });
        addView(bStreamResume);

        bStreamClose = new Button(getContext());
        bStreamClose.setText(R.string.btn_stream_close);
        bStreamClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mOverlayView.mService.mMapperChannel.streamClose();

                    bStreamInit.setVisibility(VISIBLE);
                    bStreamPause.setVisibility(GONE);
                    bStreamResume.setVisibility(GONE);
                    bStreamClose.setVisibility(GONE);

                    mOverlayView.disableMenu();
                }
                catch (Exception e) {
                    Log.e(TAG, "streamClose", e);
                }
            }
        });
        addView(bStreamClose);

        Log.i(TAG, mOverlayView.mService.mStream.toString());

        if (mOverlayView.mService.mStream.has("stream_id")) {
            bStreamInit.setVisibility(GONE);
            bStreamPause.setVisibility(GONE);
            bStreamResume.setVisibility(VISIBLE);
            bStreamClose.setVisibility(VISIBLE);
        }
        else {
            bStreamInit.setVisibility(VISIBLE);
            bStreamPause.setVisibility(GONE);
            bStreamResume.setVisibility(GONE);
            bStreamClose.setVisibility(GONE);
        }
    }

    public void setFamily(MinimapView minimapView, ChatView chatView, PartyView partyView) {
        vMinimap = minimapView;
        vChat = chatView;
        vParty = partyView;
    }
}
