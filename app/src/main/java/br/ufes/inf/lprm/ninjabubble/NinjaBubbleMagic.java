package br.ufes.inf.lprm.ninjabubble;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONStringer;

import br.ufes.inf.lprm.ninjabubble.br.ufes.inf.lprm.ninjabubble.views.OverlayView;

public class NinjaBubbleMagic extends Service {

    public String TAG = "NinjaBubbleMagic";

    public int mNotificationId = 1985;

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    public WindowManager mWindowManager;
    public OverlayView mOverlayView;

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

    public boolean mStreamStatusRequired = true;
    public JSONObject mSource;
    public JSONObject mStream;
    public JSONArray mParty;

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
                    XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder();
                    configBuilder.setUsernameAndPassword(mNick, mPWD);
                    configBuilder.setServiceName(mDomain);
                    configBuilder.setResource("device");
                    configBuilder.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);

                    mXmppConnection = new XMPPTCPConnection(configBuilder.build());
                    mXmppConnection.connect();
                    Log.i(TAG, "connected to XMPP server");
                    mXmppConnection.login();
                    Log.i(TAG, "logged into XMPP server");

                    mMapperChannel = new MapperChannel(NinjaBubbleMagic.this);
                    mPartyChannel = new PartyChannel(NinjaBubbleMagic.this);

                    // Check stream status
                    mMapperChannel.streamStatus();
                    Log.i(TAG, "streamStatus obtained");

                } catch (Exception e) {
                    Log.e(TAG, "Error while establishing XMPPConnection", e);
                    Intent errorXmppConnection = new Intent(MainActivity.ERROR_XMPP_CONNECTION);
                    errorXmppConnection.putExtra("value", e.getMessage());
                    sendBroadcast(errorXmppConnection);

                    stopSelf();
                    return;
                }

                // Starting overlay UI
                mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                mOverlayView = new OverlayView(NinjaBubbleMagic.this, mWindowManager);
                mOverlayView.start();
            } else if (msg.getData().getString("action").equals(ACTION_ONDESTROY)) {
                try {
                    mOverlayView.finish();
                    Log.i(TAG, "OverlayView succesfully removed");
                }
                catch (Exception e) {
                    Log.w(TAG, "views were not added to windowmanager");
                }
                try {
                    mXmppConnection.disconnect();
                    Log.i(TAG, "XMPPConnection succesfully terminated");
                }
                catch  (Exception e) {
                    Log.w(TAG, "XMPPConnection probably did not exist");
                }
            }
        }
    }


    @Override
    public void onCreate() {

        super.onCreate();

        HandlerThread thread = new HandlerThread("ServiceHandler",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "NinjaBubble starting", Toast.LENGTH_SHORT).show();

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
}
