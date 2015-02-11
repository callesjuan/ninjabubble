package br.ufes.inf.lprm.ninjabubble.br.ufes.inf.lprm.ninjabubble.views;

import android.app.Service;
import android.graphics.PixelFormat;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import br.ufes.inf.lprm.ninjabubble.NinjaBubbleMagic;
import br.ufes.inf.lprm.ninjabubble.R;

/**
 * Created by Juan on 09/02/2015.
 */
public class OverlayView {

    public NinjaBubbleMagic mService;

    public WindowManager mWindowManager;
    public LinearLayout mParentLayout;
    public LinearLayout mMenuLayout;
    public LinearLayout mContentLayout;
    public ImageView mNinjaHead;

    public ImageView imHome;
    public ImageView imMinimap;
    public ImageView imChat;
    public ImageView imParty;
    public ImageView imHide;

    public HomeView vHome;
    public MinimapView vMinimap;
    public ChatView vChat;
    public PartyView vParty;

    public OverlayView(NinjaBubbleMagic service, WindowManager windowManager) {
        mService = service;
        mWindowManager = windowManager;
    }

    public void start() {
        // Starting overlay UI
        final WindowManager.LayoutParams paramsNinjaHead;
        final WindowManager.LayoutParams paramsParentLayout;

        /*
        NINJA HEAD
         */
        {
            paramsNinjaHead = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
            );

            mNinjaHead = new ImageView(mService);
            mNinjaHead.setImageResource(R.drawable.ic_launcher);
            mNinjaHead.setOnTouchListener(new View.OnTouchListener() {
                private int TOUCH_TIME_THRESHOLD = 200;
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;
                private long lastTouchDown;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = paramsNinjaHead.x;
                            initialY = paramsNinjaHead.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            lastTouchDown = System.currentTimeMillis();
                            return true;
                        case MotionEvent.ACTION_UP:
                            if (System.currentTimeMillis() - lastTouchDown < TOUCH_TIME_THRESHOLD) {
                                mNinjaHead.setVisibility(View.GONE);
                                mParentLayout.setVisibility(View.VISIBLE);
                            }
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            paramsNinjaHead.x = initialX + (int) (event.getRawX() - initialTouchX);
                            paramsNinjaHead.y = initialY + (int) (event.getRawY() - initialTouchY);
                            mWindowManager.updateViewLayout(mNinjaHead, paramsNinjaHead);
                            return true;
                    }
                    return false;
                }
            });
        }

        mWindowManager.addView(mNinjaHead, paramsNinjaHead);

        /*
        PARENT LAYOUT
         */
        {
            paramsParentLayout = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
            );

            mParentLayout = new LinearLayout(mService);
            mParentLayout.setOrientation(LinearLayout.VERTICAL);
            mParentLayout.setBackgroundColor(0x88ff0000);
            mParentLayout.setBackgroundResource(R.drawable.shape);
            mParentLayout.setVisibility(View.GONE);
        }

        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);

        /*
        MENU
         */
        {
            mMenuLayout = new LinearLayout(mService);
            mMenuLayout.setGravity(Gravity.CENTER);

            imHome = new ImageView(mService);
            imHome.setImageResource(R.drawable.home_50);
            imHome.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.25f));
            imHome.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mContentLayout.removeAllViews();
                    mContentLayout.addView(vHome);
                }
            });
            mMenuLayout.addView(imHome);

            imMinimap = new ImageView(mService);
            imMinimap.setImageResource(R.drawable.map_marker_50);
            imMinimap.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.25f));
            imMinimap.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mContentLayout.removeAllViews();
                    mContentLayout.addView(vMinimap);
                }
            });
            mMenuLayout.addView(imMinimap);

            imChat = new ImageView(mService);
            imChat.setImageResource(R.drawable.group_50);
            imChat.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.25f));
            imChat.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mContentLayout.removeAllViews();
                    mContentLayout.addView(vChat);
                }
            });
            mMenuLayout.addView(imChat);

            imHide = new ImageView(mService);
            imHide.setImageResource(R.drawable.return_50);
            imHide.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.25f));
            imHide.setClickable(true);
            imHide.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mParentLayout.setVisibility(View.GONE);
                    mNinjaHead.setVisibility(View.VISIBLE);
                }
            });
            mMenuLayout.addView(imHide);

            mParentLayout.addView(mMenuLayout);
        }

        /*
        CONTENT
         */
        {
            mContentLayout = new LinearLayout(mService);
            int contentWidth = (int) (metrics.widthPixels * 0.90);
            int contentHeight = (int) (metrics.heightPixels * 0.75);
            mContentLayout.setLayoutParams(new LinearLayout.LayoutParams(contentWidth, contentHeight));

            vHome = new HomeView(mService);
            vMinimap = new MinimapView(mService);
            vChat = new ChatView(mService);
            vParty = new PartyView(mService);

            vHome.setFamily(this, vMinimap, vChat, vParty);
            vMinimap.setFamily(this, vHome, vChat, vParty);
            vChat.setFamily(this, vHome, vMinimap, vParty);
            vParty.setFamily(this, vHome, vMinimap, vChat);

            mContentLayout.addView(vHome);

            mParentLayout.addView(mContentLayout);
        }

        mWindowManager.addView(mParentLayout, paramsParentLayout);
    }

    public void finish() throws Exception {
        try {
            mWindowManager.removeView(mNinjaHead);
            mWindowManager.removeView(mParentLayout);
        }
        catch (Exception e) {
            throw e;
        }
    }
}
