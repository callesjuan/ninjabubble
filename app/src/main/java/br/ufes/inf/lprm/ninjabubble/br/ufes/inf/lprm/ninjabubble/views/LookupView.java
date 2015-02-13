package br.ufes.inf.lprm.ninjabubble.br.ufes.inf.lprm.ninjabubble.views;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import br.ufes.inf.lprm.ninjabubble.R;

/**
 * Created by Juan on 13/02/2015.
 */
public class LookupView extends LinearLayout {

    public final String TAG = "NinjaBubbleMagic/LookupView";

    public OverlayView mOverlayView;

    public ListView mListView;
    public Button mCancel;

    public LookupView(Context context, OverlayView overlayView) {
        super(context);

        mOverlayView = overlayView;

        Log.i(TAG, "created");

        setGravity(Gravity.CENTER_HORIZONTAL);
        setOrientation(VERTICAL);

        setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));


    }

    public void show () {
        mListView = new ListView(getContext());

        JSONArray matchedGroups = mOverlayView.mService.mMatchedGroups;
        for (int i = 0; i < matchedGroups.length(); i ++) {
            try {
                JSONObject group = matchedGroups.getJSONObject(i);

            }
            catch (Exception e) {
                Log.e(TAG, "error while retrieving matched group", e);
            }
        }
    }

    public void show (boolean cached) {

    }

    public void addCancelButton () {
        mCancel = new Button(getContext());
        mCancel.setText(R.string.btn_cancel);
        mCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mOverlayView.mContentLayout.removeAllViews();
                mOverlayView.mContentLayout.addView(mOverlayView.vHome);
            }
        });
    }
}
