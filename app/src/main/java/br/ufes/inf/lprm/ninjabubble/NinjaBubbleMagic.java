package br.ufes.inf.lprm.ninjabubble;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;

public class NinjaBubbleMagic extends Service {

    private String TAG = "NinjaBubbleMagic";

    private int mNotificationId = 1985;

    private AbstractXMPPConnection xmppConnection;

    private String mJID;
    private String mPWD;
    private String mChannel;
    private String mMedia;

    private String mFullJID;
    private String mNick;
    private String mDomain;
    private String mMUC;

    private WindowManager mWindowManager;
    private LinearLayout mParentLayout;
    private LinearLayout mMenuLayout;
    private LinearLayout mContentLayout;
    private ImageView mNinjaHead;

    private Button bGroupMatch;
    private Button bStreamInit;
    private Button bHide;

    public NinjaBubbleMagic() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "NinjaBubble starting", Toast.LENGTH_SHORT).show();

        mJID = intent.getStringExtra("jid");
        mPWD = intent.getStringExtra("pwd");
        mChannel = intent.getStringExtra("channel");
        mMedia = intent.getStringExtra("media");

        String []splittedJID = mJID.split("@");
        mNick = splittedJID[0];
        mDomain = splittedJID[1];
        mFullJID = mJID + "/device";

        // Connect to XMPP server
        try {
            Log.i(TAG, String.format("Trying to XMPPConnection with (%s, %s, %s)", mNick, mPWD, mDomain));
            xmppConnection = new XMPPTCPConnection(mNick, mPWD, mDomain);
            xmppConnection.connect();
            if(xmppConnection.isConnected()) {
                Log.i(TAG, "XMPPConnection established");
            }
            else {
                Log.e(TAG, "XMPPConnection was not established");
                throw new Exception();
            }
        }
        catch(Exception e) {
            Log.e(TAG, "Error while establishing XMPPConnection");
            stopSelf();
            return START_STICKY;
        }

        // Starting overlay UI
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        final WindowManager.LayoutParams paramsNinjaHead = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        final WindowManager.LayoutParams paramsParentLayout = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        mNinjaHead = new ImageView(this);
        mNinjaHead.setImageResource(R.drawable.ic_launcher);
        mNinjaHead.setOnTouchListener(new View.OnTouchListener() {
            private int TOUCH_TIME_THRESHOLD = 200;
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private long lastTouchDown;

            @Override public boolean onTouch(View v, MotionEvent event) {
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

        mParentLayout = new LinearLayout(this);
        mParentLayout.setOrientation(LinearLayout.VERTICAL);
        mParentLayout.setBackgroundColor(0x88ff0000);
        mParentLayout.setBackgroundResource(R.drawable.shape);
        mParentLayout.setVisibility(View.GONE);

        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);

        mMenuLayout = new LinearLayout(this);
        mMenuLayout.setGravity(Gravity.CENTER);
        mParentLayout.addView(mMenuLayout);

        mContentLayout = new LinearLayout(this);
        int contentWidth = (int)(metrics.widthPixels * 0.9);
        int contentHeight = (int)(metrics.heightPixels * 0.6);
        mContentLayout.setLayoutParams(new LinearLayout.LayoutParams(contentWidth, contentHeight));
        mParentLayout.addView(mContentLayout);

        bGroupMatch = new Button(this);
        bGroupMatch.setText(R.string.btn_group_match);
        bGroupMatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mContentLayout.removeAllViews();
                TextView tv = new TextView(NinjaBubbleMagic.this);
                tv.setText("groupmatch");
                mContentLayout.addView(tv);
            }
        });
        //mMenuLayout.addView(bGroupMatch);

        bStreamInit = new Button(this);
        bStreamInit.setText(R.string.btn_stream_init);
        bStreamInit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mContentLayout.removeAllViews();
                TextView tv = new TextView(NinjaBubbleMagic.this);
                tv.setText("streaminit");
                mContentLayout.addView(tv);
            }
        });
        //mMenuLayout.addView(bStreamInit);

        bHide = new Button(this);
        //bHide.setText(R.string.btn_hide);
        bHide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mParentLayout.setVisibility(View.GONE);
                mNinjaHead.setVisibility(View.VISIBLE);
            }
        });
        //mMenuLayout.addView(bHide);

        ImageView imHome = new ImageView(this);
        imHome.setImageResource(R.drawable.home_50);
        imHome.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.25f));
        mMenuLayout.addView(imHome);

        ImageView imMinimap = new ImageView(this);
        imMinimap.setImageResource(R.drawable.map_marker_50);
        imMinimap.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.25f));
        mMenuLayout.addView(imMinimap);

        ImageView imGroup = new ImageView(this);
        imGroup.setImageResource(R.drawable.group_50);
        imGroup.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.25f));
        mMenuLayout.addView(imGroup);

        ImageView imHide = new ImageView(this);
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

        mWindowManager.addView(mNinjaHead, paramsNinjaHead);
        mWindowManager.addView(mParentLayout, paramsParentLayout);

        // Sending service to the foreground
        Intent notificationIntent = new Intent(this,
                MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getText(R.string.service_start))
                .setContentIntent(pendingIntent)
                .build();

        startForeground(mNotificationId, notification);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        try {
            mWindowManager.removeView(mNinjaHead);
            mWindowManager.removeView(mParentLayout);
        }
        catch (NullPointerException e) {
            Log.e(TAG, "views were not added to windowmanager");
        }
        stopForeground(false);
        stopSelf();
        Toast.makeText(this, "NinjaBubble stopped", Toast.LENGTH_SHORT).show();
    }
}
