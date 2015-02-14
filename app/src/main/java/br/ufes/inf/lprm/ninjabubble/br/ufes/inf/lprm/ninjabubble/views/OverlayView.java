package br.ufes.inf.lprm.ninjabubble.br.ufes.inf.lprm.ninjabubble.views;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import br.ufes.inf.lprm.ninjabubble.NinjaBubbleMagic;
import br.ufes.inf.lprm.ninjabubble.R;

/**
 * Created by Juan on 09/02/2015.
 */
public class OverlayView {

    public final String TAG = "NinjaBubbleMagic/OverlayView";

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

        double overlayWidth = metrics.widthPixels * 0.90;
        double overlayHeight = metrics.heightPixels * 0.75;

        final int IM_NUMBER = 4;
        int imSize = (int) ((overlayWidth / IM_NUMBER) * 0.75);

        /*
        MENU
         */
        {
            mMenuLayout = new LinearLayout(mService);
            mMenuLayout.setGravity(Gravity.CENTER);

            int marginH = (int) (overlayWidth * 0.02);
            int marginV = (int) (overlayHeight * 0.02);
            LinearLayout.LayoutParams menuParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            menuParams.setMargins(marginH, marginV, marginH, marginV);

            Bitmap bmap = BitmapFactory.decodeResource(mService.getResources(), R.drawable.home_round);
            Bitmap bmpHome = Bitmap.createScaledBitmap(bmap, imSize, imSize, true);

            imHome = new ImageView(mService);
//            imHome.setImageResource(R.drawable.home_round);
            imHome.setImageBitmap(bmpHome);
            imHome.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.25f));
            imHome.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    vHome.show();
                }
            });
            mMenuLayout.addView(imHome);

            bmap = BitmapFactory.decodeResource(mService.getResources(), R.drawable.map_marker_round_new);
            Bitmap bmpMinimap = Bitmap.createScaledBitmap(bmap, imSize, imSize, true);

            imMinimap = new ImageView(mService);
//            imMinimap.setImageResource(R.drawable.map_marker_round);
            imMinimap.setImageBitmap(bmpMinimap);
            imMinimap.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.25f));
            imMinimap.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    vMinimap.show();
                }
            });
            mMenuLayout.addView(imMinimap);

            bmap = BitmapFactory.decodeResource(mService.getResources(), R.drawable.group_round_new);
            Bitmap bmpChat = Bitmap.createScaledBitmap(bmap, imSize, imSize, true);

            imChat = new ImageView(mService);
//            imChat.setImageResource(R.drawable.group_round);
            imChat.setImageBitmap(bmpChat);
            imChat.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.25f));
            imChat.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    vChat.show();
                }
            });
            mMenuLayout.addView(imChat);

            bmap = BitmapFactory.decodeResource(mService.getResources(), R.drawable.return_round);
            Bitmap bmpHide = Bitmap.createScaledBitmap(bmap, imSize, imSize, true);

            imHide = new ImageView(mService);
//            imHide.setImageResource(R.drawable.return_round);
            imHide.setImageBitmap(bmpHide);
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

            disableMenu();

            mParentLayout.addView(mMenuLayout, menuParams);

            View separator = new View(mService);
            separator.setBackgroundColor(0x88000000);
            int separatorHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, metrics);
            LinearLayout.LayoutParams separatorParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, separatorHeight);
            separatorParams.setMargins(marginH, 0, marginH, 0);
            mParentLayout.addView(separator, separatorParams);
        }

        /*
        CONTENT
         */
        {
            mContentLayout = new LinearLayout(mService);
            int contentWidth = (int) (overlayWidth);
            int contentHeight = (int) (overlayHeight);
            mContentLayout.setLayoutParams(new LinearLayout.LayoutParams(contentWidth, contentHeight));

            vHome = new HomeView(mService, this);
            vMinimap = new MinimapView(mService, this);
            vChat = new ChatView(mService, this);
            vParty = new PartyView(mService, this);

            vHome.setFamily(vMinimap, vChat, vParty);
            vMinimap.setFamily(vHome, vChat, vParty);
            vChat.setFamily(vHome, vMinimap, vParty);
            vParty.setFamily(vHome, vMinimap, vChat);

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

    public void enableMenu() {
        imMinimap.setEnabled(true);
        imChat.setEnabled(true);
//        imParty.setEnabled(true);
    }

    public void disableMenu() {
        imMinimap.setEnabled(false);
        imChat.setEnabled(false);
//        imParty.setEnabled(false);
    }
}
