package br.ufes.inf.lprm.ninjabubble.br.ufes.inf.lprm.ninjabubble.views;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

/**
 * Created by Juan on 14/02/2015.
 */
public class ContentView extends LinearLayout {
    public OverlayView mOverlayView;

    public LinearLayout mContentLayout;
    public LinearLayout mLoadingLayout;

    public ProgressBar mLoadingAnimation;

    public ContentView(Context context, OverlayView overlayView) {
        super(context);

        mOverlayView = overlayView;

        setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mLoadingAnimation = new ProgressBar(mOverlayView.mService);
        mLoadingAnimation.setIndeterminate(true);

        mLoadingLayout = new LinearLayout(getContext());
        mLoadingLayout.setGravity(Gravity.CENTER);
        mLoadingLayout.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mLoadingLayout.addView(mLoadingAnimation);

        mContentLayout = new LinearLayout(getContext());
        mContentLayout.setGravity(Gravity.CENTER);
        mContentLayout.setOrientation(VERTICAL);
        mContentLayout.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addView(mContentLayout);
    }

    public void showLoading() {
        removeAllViews();
        addView(mLoadingLayout);
    }

    public void showLoaded() {
        removeAllViews();
        addView(mContentLayout);
    }

    public void show() {
        mOverlayView.mContentLayout.removeAllViews();
        mOverlayView.mContentLayout.addView(this);
    }
}
