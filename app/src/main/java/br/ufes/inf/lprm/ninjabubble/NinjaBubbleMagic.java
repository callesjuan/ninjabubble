package br.ufes.inf.lprm.ninjabubble;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.premnirmal.Magnet.IconCallback;
import com.premnirmal.Magnet.Magnet;

public class NinjaBubbleMagic extends Service {

    private String TAG = "NinjaBubbleMagic";

    private int mNotificationId = 1985;

    private Context mContext = this;
    private Magnet mMagnet;

    public NinjaBubbleMagic() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "NinjaBubble starting", Toast.LENGTH_SHORT).show();

        // Connect to XMPP server

        // Starting Magnet based UI

        ImageView iconView = new ImageView(this);
        iconView.setImageResource(R.drawable.ic_launcher);
        mMagnet = new Magnet.Builder(this)
                .setIconView(iconView) // required
                .setIconCallback(new IconCallback() {
                    @Override
                    public void onFlingAway() {
                    }

                    @Override
                    public void onMove(float v, float v2) {
                    }

                    @Override
                    public void onIconClick(View view, float v, float v2) {
                        mMagnet.destroy();
                    }

                    @Override
                    public void onIconDestroyed() {
                    }
                })
                .setShouldFlingAway(false)
                .setShouldStickToWall(true)
                .setRemoveIconShouldBeResponsive(false)
                .build();
        mMagnet.show();

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
            mMagnet.destroy();
        }
        catch(IllegalArgumentException e) {
            Log.e(TAG, "attempting to destroy magnet that does not exist anymore");
        }
        stopForeground(false);
        stopSelf();
        Toast.makeText(this, "NinjaBubble stopped", Toast.LENGTH_SHORT).show();
    }
}
