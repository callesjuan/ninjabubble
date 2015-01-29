package br.ufes.inf.lprm.sensorninja;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class ServiceNinja extends Service {
	static final String TAG = "NINJA";

	private final IBinder mBinder = new LocalBinder();

	/*
	 * Parâmetros fornecidos por ActivityPreferencias
	 */
	private int pLocationFreq;
	private String pTwitter = null;
	private String pUrl = "http://localhost:8000/ninja/send";

	/*
	 * Variáveis p/ notificação
	 */
	private int mNotificationID = 1988;
	private NotificationManager mNotificationManager;
	private NotificationCompat.Builder mNotificationBuilder;

	/*
	 * Variáveis p/ localização
	 */
	private LocationManager mLocationManager;
	private LocationListener mLocationListener;
	private Location mLocation = null;

	/*
	 * Variáveis p/ servidor HTTP
	 */
	private long mTimestamp;
	private Timer mTimer;

	public class LocalBinder extends Binder {
		ServiceNinja getService() {
			return ServiceNinja.this;
		}
	}

	public ServiceNinja() {
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	/*
	 * Inicia serviço NINJA
	 */
	@Override
	public void onCreate() {
		super.onCreate();

		// mNotificationManager = (NotificationManager)
		// getSystemService(NOTIFICATION_SERVICE);

		/*
		 * Callback para ActivitiPreferencias
		 */
		Intent notificationIntent = new Intent(ServiceNinja.this,
				ActivityPreferencias.class);
		PendingIntent intent = PendingIntent.getActivity(ServiceNinja.this, 0,
				notificationIntent, 0);

		/*
		 * Notifica inicio de serviço NINJA
		 */
		mNotificationBuilder = new NotificationCompat.Builder(ServiceNinja.this)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle(getText(R.string.app_name))
				.setContentText(getText(R.string.notification_inicio))
				.setContentIntent(intent);

		// mNotificationManager.notify(mNotificationID,
		// mNotificationBuilder.build());

		/*
		 * Inicia serviço NINJA
		 */
		startForeground(mNotificationID, mNotificationBuilder.build());
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		Bundle extras = intent.getExtras();

		pLocationFreq = extras.getInt("locationFreq");
		pTwitter = extras.getString("twitter");
		pUrl = extras.getString("url");

		Toast.makeText(
				ServiceNinja.this,
				getText(R.string.notification_inicio) + "\nFreq:"
						+ pLocationFreq + "\nTwitter:" + pTwitter,
				Toast.LENGTH_SHORT).show();

		iniciarLocalizacao();

		iniciarComunicacao();

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		mLocationManager.removeUpdates(mLocationListener);
		mTimer.cancel();

		stopForeground(false);

		// mNotificationManager.cancel(mNotificationID);
		stopSelf();
	}

	public void iniciarLocalizacao() {
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		mLocationListener = new LocationListener() {
			@Override
			public void onLocationChanged(Location arg0) {
				// Enviar localização p/ servidor
				// String text = "Latitude: " + arg0.getLatitude()
				// + "\nLongitude: " + arg0.getLongitude()
				// + "\nProvedor: " + arg0.getProvider();
				// Toast.makeText(ServiceNinja.this, text,
				// Toast.LENGTH_LONG).show();
				if (isBetterLocation(arg0, mLocation)) {
					mLocation = arg0;
				}
			}

			@Override
			public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
			}

			@Override
			public void onProviderEnabled(String arg0) {
			}

			@Override
			public void onProviderDisabled(String arg0) {
			}
		};

		if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			mLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, pLocationFreq, 50,
					mLocationListener);
			/**
			 * LocationManager.GPS_PROVIDER, pLocationFreq, 0,
			 * mLocationListener);
			 */
		}
		if (mLocationManager
				.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			mLocationManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, pLocationFreq, 50,
					mLocationListener);
		}

		mLocation = mLocationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	}

	public void iniciarComunicacao() {
		if (mTimer != null) {
			mTimer.cancel();
		}

		mTimer = new Timer();
		mTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				mTimestamp = System.currentTimeMillis();
				if (pUrl.isEmpty())
					persisteLocalizacao();
				else
					enviarLocalizacao();
			}
		}, 5 * 1000, pLocationFreq);
	}

	public void persisteLocalizacao() {
		SQLiteHelper db = new SQLiteHelper(this);

		Date data = new Date(mTimestamp);
		SimpleDateFormat sdfData = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		db.checkpoint(mLocation.getLatitude(), mLocation.getLongitude(),
				sdfData.format(data));
	}

	public void enviarLocalizacao() {

		if (pTwitter == null || mLocation == null) {
			return;
		}

		HttpClient httpClient = new DefaultHttpClient();

		Date data = new Date(mTimestamp);
		SimpleDateFormat sdfData = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm:ss");
		String url = pUrl + "/" + pTwitter + "/" + mLocation.getLatitude()
				+ "/" + mLocation.getLongitude() + "/" + sdfData.format(data)
				+ "/" + sdfHora.format(data) + "/" + pLocationFreq;

		HttpGet httpGet = new HttpGet(url);

		httpGet.getParams().setParameter("twitter", pTwitter);
		httpGet.getParams().setDoubleParameter("longitude",
				mLocation.getLongitude());
		httpGet.getParams().setDoubleParameter("latitude",
				mLocation.getLatitude());
		httpGet.getParams().setLongParameter("tempo", mLocation.getTime());

		HttpResponse httpResponse = null;
		try {
			httpResponse = httpClient.execute(httpGet);

			// for logging
			Log.v("url", url);
		} catch (Exception e) {

		}
	}

	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix
	 * 
	 * @param location
	 *            The new Location that you want to evaluate
	 * @param currentBestLocation
	 *            The current Location fix, to which you want to compare the new
	 *            one
	 */
	protected boolean isBetterLocation(Location location,
			Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > pLocationFreq;
		boolean isSignificantlyOlder = timeDelta < -pLocationFreq;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
				.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}
}
