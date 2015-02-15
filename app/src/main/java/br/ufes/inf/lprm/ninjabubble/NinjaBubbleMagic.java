package br.ufes.inf.lprm.ninjabubble;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import br.ufes.inf.lprm.ninjabubble.br.ufes.inf.lprm.ninjabubble.views.OverlayView;

public class NinjaBubbleMagic extends Service {

    public String TAG = "NinjaBubbleMagic";

    public int mNotificationId = 1985;

    public HandlerThread mHandlerThread;
    public Looper mServiceLooper;
    public ServiceHandler mServiceHandler;

    public HandlerThread mHandlerParallel;
    public Looper mParallelLooper;

//    public Handler mConcurrentHandler;

    public WindowManager mWindowManager;
    public OverlayView mOverlayView;
    public ProgressBar mLoading;

    public AbstractXMPPConnection mXmppConnection;
    public MapperChannel mMapperChannel;
    public PartyChannel mPartyChannel;

    public String mJID;
    public String mPWD;
    public String mChannel;
    public String mMedia;

    public String mFullJID;
    public String mNick;
    public String mDomain;

    public String mMapperJID;
    public String mMucJID;

    public JSONObject mSource;
    public JSONObject mStream;
    public JSONArray mParty;
    public JSONArray mMatchedGroups;

    public JSONArray mLatlng;
    public String mHashtags;

    private final static String ACTION_ONSTARTCOMMAND = "ACTION_ONSTARTCOMMAND";
    private final static String ACTION_ONDESTROY = "ACTION_ONDESTROY";

    public NinjaBubbleMagic() {
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.getData().getString("action").equals(ACTION_ONSTARTCOMMAND)) {
                /*
                check sensors (internet, gps, compass)
                 */
                try {
                    mLatlng = new JSONArray().put(0.1).put(0.1);
                } catch (Exception e) {
                    Log.e(TAG, "bad latlng");
                }

                /*
                handle input
                 */
                mJID = msg.getData().getString("jid");
                mPWD = msg.getData().getString("pwd");
                mChannel = msg.getData().getString("channel");
                mMedia = msg.getData().getString("media");

                String[] splittedJID = mJID.split("@");
                mNick = splittedJID[0];
                mDomain = splittedJID[1];
                mFullJID = mJID + "/device";

                mMapperJID = String.format("mapper@%s", mDomain);
                Log.i(TAG, String.format("mapper %s set", mMapperJID));

                try {
                    // Connect to XMPP server
                    mMapperChannel = new MapperChannel(NinjaBubbleMagic.this);
                    mPartyChannel = new PartyChannel(NinjaBubbleMagic.this);

                    // Check stream status
                    mMapperChannel.connect();
                    mMapperChannel.streamStatus();

                    if (mStream != null && mStream.getString("status").equals("streaming")) {
                        mMapperChannel.streamPause();
                    }

//                    if (mMedia.equals("Twitcasting") && (mSource.getString("twitcasting_id").isEmpty() || mSource.getString("twitcasting_id").equals(""))) {
//                        mMapperChannel.updateTwitcastingId(mChannel);
//                    }

                    if (mSource.getString("twitcasting_id").equals("null")) {
                        mMapperChannel.updateTwitcastingId(mChannel);
                        mSource.put("twitcasting_id", mChannel);
                    }

                    mMapperChannel.disconnect();

                } catch (Exception e) {
                    Intent intent = new Intent(getBaseContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("flag", MainActivity.ERROR_WHILE_STARTING);
                    getApplication().startActivity(intent);

                    Log.e(TAG, "Error while establishing XMPPConnection", e);
                    Intent errorXmppConnection = new Intent(MainActivity.ERROR_XMPP_CONNECTION);
                    errorXmppConnection.putExtra("value", e.getMessage());
                    sendBroadcast(errorXmppConnection);

                    mWindowManager.removeView(mLoading);

                    stopSelf();
                    return;
                }

                // Starting overlay UI
                mOverlayView = new OverlayView(NinjaBubbleMagic.this, mWindowManager);
                mOverlayView.start();

                mWindowManager.removeView(mLoading);
            } else if (msg.getData().getString("action").equals(ACTION_ONDESTROY)) {
                try {
                    mOverlayView.finish();
                    Log.i(TAG, "OverlayView succesfully removed");
                }
                catch (Exception e) {
                    Log.w(TAG, "views were not added to windowmanager");
                }

                mPartyChannel.leave();
                mMapperChannel.disconnect();
            }
        }
    }


    @Override
    public void onCreate() {

        super.onCreate();

        mHandlerThread = new HandlerThread("ServiceHandler",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mHandlerThread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = mHandlerThread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        // Parallel looper
//        mConcurrentHandler = new Handler();
        mHandlerParallel = new HandlerThread("ParallelHandler",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mHandlerParallel.start();

        mParallelLooper = mHandlerParallel.getLooper();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "NinjaBubble starting", Toast.LENGTH_SHORT).show();

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mLoading = new ProgressBar(this);
        mLoading.setIndeterminate(true);
        mWindowManager.addView(mLoading, new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
        );

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        Bundle bundle = intent.getExtras();
        bundle.putString("action", ACTION_ONSTARTCOMMAND);
        msg.setData(bundle);
        mServiceHandler.sendMessage(msg);

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

        Message msg = mServiceHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("action", ACTION_ONDESTROY);
        msg.setData(bundle);
        mServiceHandler.sendMessage(msg);

        stopForeground(true);
        stopSelf();
        Toast.makeText(this, "NinjaBubble stopped", Toast.LENGTH_SHORT).show();
    }

    public void runOnUiThread(Runnable runnable) {
        Handler handler = new Handler(mServiceLooper);
        handler.post(runnable);
    }

    public void runConcurrentThread(Runnable runnable) {
//        mConcurrentHandler.post(runnable);
        Handler handler = new Handler(mParallelLooper);
        handler.post(runnable);
    }
}
