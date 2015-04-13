package br.ufes.inf.lprm.ninjabubble;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.android.AndroidSmackInitializer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import br.ufes.inf.lprm.ninjabubble.br.ufes.inf.lprm.ninjabubble.views.OverlayView;

public class NinjaBubbleMagic extends Service {

    public String TAG = "NinjaBubbleMagic";

    public final String CHAT_FILENAME = "chat.json";

    public int mNotificationId = 1985;
    public int mNotificationIdMember = 1986;
    public int mNotificationIdCorrelation = 1987;

    public HandlerThread mHandlerThread;
    public Looper mServiceLooper;
    public ServiceHandler mServiceHandler;

    public HandlerThread mHandlerParallel;
    public Looper mParallelLooper;

//    public Handler mConcurrentHandler;

    public LocationManager mLocationManager;
    public MyLocationListener mLocationListener;
    public final int mMinTimeUpdate = 0;
    public final float mMinDistanceUpdate = 0;

    public SensorManager mSensorManager;
    public Sensor mSensor;
    public Sensor mAccelerometer;
    public Sensor mMagnetometer;
    public MySensorEventListener mSensorEventListener;

    public WindowManager mWindowManager;
    public OverlayView mOverlayView;
    public ProgressBar mLoading;

    public AbstractXMPPConnection mXmppConnection;
    public AndroidSmackInitializer mAndroidInitializer;
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
    public JSONArray mMatchedGroups;
    public JSONArray mParty;
    public JSONArray mPings;

    public JSONArray mLatlng;
    public String mHashtags;

    public int mPartyCount;

    public MyBroadcastReceiver mBroadcastReceiver = new MyBroadcastReceiver();

    private final static String ACTION_ONSTARTCOMMAND = "ACTION_ONSTARTCOMMAND";
    private final static String ACTION_ONDESTROY = "ACTION_ONDESTROY";

    private final static String NOTIFY_CORRELATION = "br.ufes.inf.lprm.ninjabubble.NOTIFIY_CORRELATION";

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
                    mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

                    Log.i(TAG, "GPS enabled:" + mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));

                    if (!(mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) || !(mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))) {
                        throw new Exception("GPS and Network Provider must be enabled");
                    }

                    mLocationListener = new MyLocationListener();
                } catch (Exception e) {

                    Log.e(TAG, "Error while retrieving GPS", e);
                    Intent errorGPS = new Intent(MainActivity.ERROR_XMPP_CONNECTION);
                    errorGPS.putExtra("value", e.getMessage());
                    sendBroadcast(errorGPS);

                    mWindowManager.removeView(mLoading);

                    stopSelf();
                    return;
                }

                try {
                    mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//                    mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
                    mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                    mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

                    mSensorEventListener = new MySensorEventListener();
                } catch (Exception e) {
                    Log.e(TAG, "Orientation sensors", e);
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

                    mMapperChannel.disconnect();

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

                try {
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
                    intentFilter.addAction(NOTIFY_CORRELATION);
                    registerReceiver(mBroadcastReceiver, intentFilter);
                } catch (Exception e) {}

            } else if (msg.getData().getString("action").equals(ACTION_ONDESTROY)) {

                try {
                    unregisterReceiver(mBroadcastReceiver);
                } catch (Exception e) {}

                try {
                    stopLocationListener();
                } catch (Exception e) {
                    Log.w(TAG, "while releasing gps");
                }
                try {
                    stopOrientationListener();
                } catch (Exception e) {
                    Log.w(TAG, "while releasing accelerometer and geometer");
                }

                if (mPartyChannel != null) {
                    mPartyChannel.leave();
                }

                if (mMapperChannel != null) {
                    mMapperChannel.disconnect();
                }

                try {
                    mOverlayView.finish();
                    Log.i(TAG, "OverlayView succesfully removed");
                } catch (Exception e) {
                    Log.w(TAG, "views were not added to windowmanager");
                }
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
                .setSmallIcon(R.drawable.my_launcher)
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

    public void getLastKnownLocation() {
        try {
            Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) {
                location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (location != null) {
                JSONArray latlng = new JSONArray();
                latlng.put(location.getLongitude());
                latlng.put(location.getLatitude());
                mLatlng = latlng;
                Log.i(TAG, "location set on lat:" + location.getLatitude() + " and lng:" + location.getLongitude());
            }
            Log.i(TAG, "lcation set, or not?");
        } catch (Exception e) {
        }
    }

    public void startLocationListener() throws Exception {
        try {
            if (mLocationListener == null) {
                throw new Exception("locationListener is null");
            }
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
            Log.i(TAG, "GPS...");
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
            Log.i(TAG, "NETWORK...");
        } catch (Exception e) {
            throw e;
        }
    }

    public void stopLocationListener() {
        try {
            mLocationManager.removeUpdates(mLocationListener);
        } catch (Exception e) {
        }
    }

    public void startOrientationListener() throws Exception {
        try {
            mSensorManager.registerListener(mSensorEventListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(mSensorEventListener, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        } catch (Exception e) {
            throw e;
        }
    }

    public void stopOrientationListener() {
        try {
            mSensorManager.unregisterListener(mSensorEventListener);
        } catch (Exception e) {
        }
    }

    public class MyLocationListener implements LocationListener {

        private static final int THRESHOLD = 1000 * 60 * 2;

        Location mCurrentBestLocation;

        /**
         * Determines whether one Location reading is better than the current Location fix
         *
         * @param location            The new Location that you want to evaluate
         * @param currentBestLocation The current Location fix, to which you want to compare the new one
         */
        protected boolean isBetterLocation(Location location, Location currentBestLocation) {
            if (currentBestLocation == null) {
                // A new location is always better than no location
                return true;
            }

            // Check whether the new location fix is newer or older
            long timeDelta = location.getTime() - currentBestLocation.getTime();
            boolean isSignificantlyNewer = timeDelta > THRESHOLD;
            boolean isSignificantlyOlder = timeDelta < -THRESHOLD;
            boolean isNewer = timeDelta > 0;

            // If it's been more than two minutes since the current location, use the new location
            // because the user has likely moved
            if (isSignificantlyNewer) {
                return true;
                // If the new location is more than two minutes older, it must be worse
            } else if (isSignificantlyOlder) {
                return false;
            }

            // Check whether the new location fix is more or less accurate
            int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
            boolean isLessAccurate = accuracyDelta > 0;
            boolean isMoreAccurate = accuracyDelta < 0;
            boolean isSignificantlyLessAccurate = accuracyDelta > 200;

            // Check if the old and new location are from the same provider
            boolean isFromSameProvider = isSameProvider(location.getProvider(),
                    currentBestLocation.getProvider());

            // Determine location quality using a combination of timeliness and accuracy
            if (isMoreAccurate) {
                return true;
            } else if (isNewer && !isLessAccurate) {
                return true;
            } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
                return true;
            }
            return false;
        }

        /**
         * Checks whether two providers are the same
         */
        private boolean isSameProvider(String provider1, String provider2) {
            if (provider1 == null) {
                return provider2 == null;
            }
            return provider1.equals(provider2);
        }

        @Override
        public void onLocationChanged(final Location location) {
//            Log.i(TAG, "update:false provider:" + location.getProvider() + " lat:"+location.getLatitude()+" lng:"+location.getLongitude());
            if (isBetterLocation(location, mCurrentBestLocation)) {
                mCurrentBestLocation = location;
//                Log.i(TAG, "isBetterLocation");

                try {
                    JSONArray latlng = new JSONArray();
                    latlng.put(location.getLongitude());
                    latlng.put(location.getLatitude());
                    mLatlng = latlng;

                    if (mStream != null && mStream.getString("status").equals("streaming")) {
                        runConcurrentThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    mMapperChannel.updateLatlng(mLatlng);
//                                    Toast.makeText(NinjaBubbleMagic.this, "update:true provider:" + location.getProvider() + " lat:"+location.getLatitude()+" lng:"+location.getLongitude(), Toast.LENGTH_SHORT).show();
                                    Log.i(TAG, "update:true provider:" + location.getProvider() + " lat:" + location.getLatitude() + " lng:" + location.getLongitude());
                                } catch (Exception e) {
                                    Log.e(TAG, "onLocationChanged", e);
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "onLocationChanged", e);
//                    Toast.makeText(NinjaBubbleMagic.this, R.string.error_locationchanged, Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.i(TAG, provider + " went on");
            Toast.makeText(NinjaBubbleMagic.this, provider + " went on", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.i(TAG, provider + " went off");
            Toast.makeText(NinjaBubbleMagic.this, provider + " went off", Toast.LENGTH_SHORT).show();
        }
    }

    public class MySensorEventListener implements SensorEventListener {

        public float[] mGravity;
        public float[] mGeomagnetic;

        public Float mAzimut;
        public Float mAzimutInDegress;
        public Float mCurrentDegree = 0f;

        @Override
        public void onSensorChanged(SensorEvent event) {
            try {
                if (mStream != null && mStream.getString("status").equals("streaming") && mOverlayView.vMinimap != null && mOverlayView.vMinimap.mHasLoaded) {
                    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                        mGravity = event.values;
                    } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                        mGeomagnetic = event.values;
                    }
                    if (mGravity != null && mGeomagnetic != null) {
                        float R[] = new float[9];
                        float I[] = new float[9];
                        boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
                        if (success) {
                            float orientation[] = new float[3];
                            SensorManager.getOrientation(R, orientation);
                            mAzimut = orientation[0]; // orientation contains: azimut, pitch and roll
                            mAzimutInDegress = (float) Math.toDegrees(mAzimut);
//                            mAzimutInDegress = -mAzimut*360/(2*3.14159f);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
//                                    float degrees = 90;
//                                    Bitmap source = BitmapFactory.decodeResource(getResources(), R.drawable.self);
//                                    Matrix matrix = new Matrix();
//                                    matrix.postRotate(degrees);
//                                    Bitmap rotated = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
//                                    mOverlayView.vMinimap.imSelf.setImageBitmap(rotated);

//                                    RotateAnimation ra = new RotateAnimation(
//                                            mCurrentDegree,
//                                            -mAzimutInDegress,
//                                            Animation.RELATIVE_TO_SELF, 0.5f,
//                                            Animation.RELATIVE_TO_SELF,
//                                            0.5f);
//                                    ra.setDuration(250);
//                                    ra.setFillAfter(true);
//                                    mOverlayView.vMinimap.imSelf.startAnimation(ra);
//                                    mCurrentDegree = -mAzimutInDegress;

//                                    RotateAnimation ra = new RotateAnimation(
//                                            mCurrentDegree,
//                                            mAzimutInDegress,
//                                            Animation.RELATIVE_TO_SELF, 0.5f,
//                                            Animation.RELATIVE_TO_SELF,
//                                            0.5f);
//                                    ra.setDuration(250);
//                                    ra.setFillAfter(true);
//                                    mOverlayView.vMinimap.imSelf.startAnimation(ra);
//                                    mCurrentDegree = mAzimutInDegress;

                                    mCurrentDegree = mAzimutInDegress;
                                }
                            });
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "onSensorChanged", e);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    public List<String> retrieveChatHistory() {
        ArrayList<String> history = new ArrayList<>();

        try {
            InputStream inputStream = openFileInput(CHAT_FILENAME);

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();

                JSONArray jsonArray = new JSONArray(stringBuilder.toString());

                for (int i = 0; i < jsonArray.length(); i++) {
                    history.add(jsonArray.getString(i));
                }
            }
        } catch (Exception e) {

        }

        //getExternalCacheDir();

        return history;
    }

    public void persistChatHistory(ArrayAdapter<String> history) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < history.getCount(); i++) {
                jsonArray.put(history.getItem(i));
            }

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(openFileOutput(CHAT_FILENAME, Context.MODE_PRIVATE));
            outputStreamWriter.write(jsonArray.toString());
            outputStreamWriter.close();
        } catch (Exception e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public void deleteChatHistory() {
        try {
            Log.i(TAG, "deleteChatHistory");
            File file = new File(getFilesDir().getPath() + "/" + CHAT_FILENAME);
            boolean deleted = file.delete();
            Log.i(TAG, "deleteChatHistory:" + deleted);
        } catch (Exception e) {
            Log.e(TAG, "deleteChatHistory", e);
        }
    }

    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(NOTIFY_CORRELATION)) {
                final String groupJid = intent.getStringExtra("group_jid");

                AlertDialog.Builder builder = new AlertDialog.Builder(NinjaBubbleMagic.this);
                builder.setTitle(R.string.alert_group_join);
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mOverlayView.vHome.groupMatchGroupJoin(groupJid);
                            }
                        });
                    }
                });
                builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                Dialog dialog = builder.create();
                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                dialog.show();
            }
        }
    }

    public void notifyMember(String title, String text) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.my_launcher)
                        .setContentTitle(title)
                        .setContentText(text);

        mBuilder.setContentIntent(null);
        mBuilder.setAutoCancel(true);
        mBuilder.setTicker(text);
        // mBuilder.setVibrate(new long[] { 1000, 1000});
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(mNotificationIdMember, mBuilder.build());

        mNotificationManager.cancel(mNotificationIdMember);
    }

    public void notifyCorrelation(JSONObject args) {

        try {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.my_launcher)
                            .setContentTitle("Group suggestion")
                            .setContentText(String.format("Join %s (%d)", args.getString("hashtags"), args.getInt("num_members")));

            Intent intent = new Intent(NOTIFY_CORRELATION);
            intent.putExtra("group_jid", args.getString("group_jid"));
            intent.putExtra("hashtags", args.getString("hashtags"));
            intent.putExtra("num_members", args.getInt("num_members"));
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

            mBuilder.setContentIntent(pendingIntent);
            mBuilder.setAutoCancel(true);
            mBuilder.setTicker("Group suggestion");
            // mBuilder.setVibrate(new long[]{1000, 1000});
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            // mId allows you to update the notification later on.
            mNotificationManager.notify(mNotificationIdCorrelation, mBuilder.build());
        } catch (Exception e) {
            Log.e(TAG, "notifyCorrelation", e);
        }
    }
}
