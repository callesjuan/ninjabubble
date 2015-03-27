package br.ufes.inf.lprm.ninjabubble.br.ufes.inf.lprm.ninjabubble.views;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.overlays.MapEventsOverlay;
import org.osmdroid.bonuspack.overlays.MapEventsReceiver;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.MarkerInfoWindow;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import br.ufes.inf.lprm.ninjabubble.R;

/**
 * Created by Juan on 09/02/2015.
 */
public class MinimapView extends ContentView {
    public final String TAG = "NinjaBubbleMagic/MinimapView";

    public final String PING_ASSIST = "ASSIST";
    public final String PING_DANGER = "DANGER";
    public final String PING_TARGET = "TARGET";

    public MapView mMapView;
    public IMapController mMapController;
    public Marker mSelf;

    public LinearLayout mOptions;

    public boolean mOverlaysInitiated = false;
    public ArrayList<Marker> mParty = new ArrayList<Marker>();
    public ArrayList<PingMarker> mPings = new ArrayList<PingMarker>();

    public MapEventsOverlay mMapEventsOverlay;
    public MyMapEventsReceiver mMapEventsReceiver;

    public Timer mTimer;
    public final long TIMER_RATE = 1000 * 10;

    public String mPing;

    public int mDefaultTextColor;
    public Button mLastPingButton;

    public long mLastGroupFetchMembers = 0;
    public long mLastGroupFetchPings = 0;

    public final long PING_LIFETIME = 1000 * 60 * 5;

    public MinimapView(Context context, OverlayView overlayView) {
        super(context, overlayView);

        mContentLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        mContentLayout.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    public void show() {
        if (mHasLoaded)
            super.showLoading();

        super.show();

        if (mHasLoaded) {
            try {
                mMapView.getTileProvider().detach();
            } catch (Exception e) {}

            mContentLayout.removeAllViews();
            mMapView = null;
            mMapController = null;

            if (mPing != null) {
                mPing = null;
                mLastPingButton.setTextColor(mDefaultTextColor);
                mLastPingButton = null;
            }
        }
        else {
            mHasLoaded = true;

            mMapEventsReceiver = new MyMapEventsReceiver();
            mMapEventsOverlay = new MapEventsOverlay(getContext(), mMapEventsReceiver);

            /**
             * MAP OPTIONS
             */
            mOptions = new LinearLayout(getContext());
            mOptions.setGravity(Gravity.CENTER);
            mOptions.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            Button bPan = new Button(getContext());
            bPan.setText("PAN");
            bPan.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOverlayView.mService.runConcurrentThread(new Runnable() {
                        @Override
                        public void run() {
                            prepareSelf();

                            mOverlayView.mService.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mMapController.setCenter(mSelf.getPosition());

                                    mMapView.invalidate();
                                }
                            });
                        }
                    });
                }
            });
            mOptions.addView(bPan);

            final Button bPingTarget = new Button(getContext());
            bPingTarget.setText(PING_TARGET);
            bPingTarget.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPing == null) {
                        mPing = PING_TARGET;
                        bPingTarget.setTextColor(Color.parseColor("red"));
                        mLastPingButton = bPingTarget;

                        mMapView.getOverlays().add(mMapEventsOverlay);
                    } else if (mPing.equals(PING_TARGET)) {
                        mPing = null;
                        bPingTarget.setTextColor(mDefaultTextColor);
                        mLastPingButton = null;

                        mMapView.getOverlays().remove(mMapEventsOverlay);
                    } else if (!mPing.equals(PING_TARGET)) {
                        mPing = PING_TARGET;
                        bPingTarget.setTextColor(Color.parseColor("red"));

                        mLastPingButton.setTextColor(mDefaultTextColor);
                        mLastPingButton = bPingTarget;
                    }
                }
            });
            mOptions.addView(bPingTarget);

            final Button bPingAssist = new Button(getContext());
            bPingAssist.setText(PING_ASSIST);
            bPingAssist.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPing == null) {
                        mPing = PING_ASSIST;
                        bPingAssist.setTextColor(Color.parseColor("red"));
                        mLastPingButton = bPingAssist;

                        mMapView.getOverlays().add(mMapEventsOverlay);
                    } else if (mPing.equals(PING_ASSIST)) {
                        mPing = null;
                        bPingAssist.setTextColor(mDefaultTextColor);
                        mLastPingButton = null;

                        mMapView.getOverlays().remove(mMapEventsOverlay);
                    } else if (!mPing.equals(PING_ASSIST)) {
                        mPing = PING_ASSIST;
                        bPingAssist.setTextColor(Color.parseColor("red"));

                        mLastPingButton.setTextColor(mDefaultTextColor);
                        mLastPingButton = bPingAssist;
                    }
                }
            });
            mOptions.addView(bPingAssist);

            final Button bPingDanger = new Button(getContext());
            bPingDanger.setText(PING_DANGER);
            bPingDanger.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPing == null) {
                        mPing = PING_DANGER;
                        bPingDanger.setTextColor(Color.parseColor("red"));
                        mLastPingButton = bPingDanger;

                        mMapView.getOverlays().add(mMapEventsOverlay);
                        mMapView.invalidate();
                    } else if (mPing.equals(PING_DANGER)) {
                        mPing = null;
                        bPingDanger.setTextColor(mDefaultTextColor);
                        mLastPingButton = null;

                        mMapView.getOverlays().remove(mMapEventsOverlay);
                        mMapView.invalidate();
                    } else if (!mPing.equals(PING_DANGER)) {
                        mPing = PING_DANGER;
                        bPingDanger.setTextColor(Color.parseColor("red"));

                        mLastPingButton.setTextColor(mDefaultTextColor);
                        mLastPingButton = bPingDanger;
                    }
                }
            });
            mOptions.addView(bPingDanger);

            mDefaultTextColor = bPan.getCurrentTextColor();
        }

        mMapView = new MapView(getContext(), 10);
        mOverlayView.mService.runConcurrentThread(new Runnable() {
            @Override
            public void run() {
                if (!mOverlaysInitiated) {
                    initPings();
                    initParty();
                    prepareSelf();
                    mOverlaysInitiated = true;
                } else {
                    migrateMarkers();
                }

                final int contentWidth = (int) (mOverlayView.mWidth);
                final int contentHeight = (int) (mOverlayView.mHeight * 0.8);

                mOverlayView.mService.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MinimapView.super.showLoaded();

                        /**
                         * MAP VIEW
                         */
                        mMapView.setTileSource(TileSourceFactory.MAPNIK);
                        mMapView.setBuiltInZoomControls(true);
                        mMapView.setMultiTouchControls(true);
                        mMapView.setLayoutParams(new LinearLayout.LayoutParams(contentWidth, contentHeight));

                        mMapController = mMapView.getController();
                        mMapController.setZoom(19);
                        mMapController.setCenter(mSelf.getPosition());

                        mContentLayout.addView(mMapView);
                        // mMapView.invalidate();
                        mContentLayout.addView(mOptions);

                        mMapView.getOverlays().addAll(mPings);
                        mMapView.getOverlays().addAll(mParty);
                        mMapView.getOverlays().add(mSelf);
                    }
                });
            }
        });

        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long millis = System.currentTimeMillis();


                mMapView.getOverlays().removeAll(mParty);
                initParty();
                mMapView.getOverlays().addAll(mParty);


                mMapView.getOverlays().removeAll(mPings);
                initPings();
                mMapView.getOverlays().addAll(mPings);


                mMapView.getOverlays().remove(mSelf);
                prepareSelf();
                mMapView.getOverlays().add(mSelf);

                mOverlayView.mService.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mMapView.invalidate();
                    }
                });
            }
        }, TIMER_RATE, TIMER_RATE);

    }

    public void prepareSelf() {
        try {
            Drawable drawable;

            if (mOverlayView.mService.mSensorEventListener.mCurrentDegree != null) {
                Bitmap bmOriginal = BitmapFactory.decodeResource(getResources(), R.drawable.self);
                Bitmap bmResult = Bitmap.createBitmap(bmOriginal.getWidth(), bmOriginal.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas tempCanvas = new Canvas(bmResult);
                tempCanvas.rotate(mOverlayView.mService.mSensorEventListener.mCurrentDegree, bmOriginal.getWidth() / 2, bmOriginal.getHeight() / 2);
                tempCanvas.drawBitmap(bmOriginal, 0, 0, null);

                drawable = new BitmapDrawable(getResources(), bmResult);
                Log.i(TAG, "rotated");
            }
            else {
                drawable = getResources().getDrawable(R.drawable.self);
            }

            double lat = mOverlayView.mService.mLatlng.getDouble(1);
            double lng = mOverlayView.mService.mLatlng.getDouble(0);
            GeoPoint myLatlng = new GeoPoint(lat, lng);

            if (mSelf == null) {
                mSelf = new Marker(mMapView);
                mSelf.setTitle("Self");
                try {
                    mSelf.setSnippet(mOverlayView.mService.mStream.getString("hashtags"));
                } catch (Exception e) {}
            }
            mSelf.setPosition(myLatlng);
            mSelf.setIcon(drawable);

        } catch (Exception e) {
            Log.e(TAG, "could not retrieve self", e);
        }
    }



    public void initParty() {

        try {
            mOverlayView.mService.mMapperChannel.groupFetchMembers();
        } catch (Exception e) {
            Log.e(TAG, "loadParty", e);
            Toast.makeText(getContext(), R.string.error_groupfetchmembers, Toast.LENGTH_SHORT).show();
        }

        mParty.clear();
        JSONArray members = mOverlayView.mService.mParty;
        if (members == null) {
            return;
        }
        for (int i = 0; i < members.length(); i++) {
            try {
                JSONObject member = members.getJSONObject(i);
                if (!member.getString("status").equals("streaming")) {
                    continue;
                }

                Marker marker = new Marker(mMapView);

                marker.setTitle(member.getString("jid"));
                marker.setSnippet(member.getString("hashtags"));

                JSONArray latlng = member.getJSONArray("latlng");
                GeoPoint geoPoint = new GeoPoint(latlng.getDouble(1), latlng.getDouble(0));
                marker.setPosition(geoPoint);
                marker.setIcon(getResources().getDrawable(R.drawable.others));

                mParty.add(marker);
            } catch (Exception e) {}
        }

        mLastGroupFetchMembers = System.currentTimeMillis();
        Log.i(TAG, "LOADED_MEMBERS:"+mParty.size());
    }

    public void initPings() {

        try {
            mOverlayView.mService.mMapperChannel.groupFetchPings(PING_LIFETIME);
        } catch (Exception e) {
            Log.e(TAG, "loadParty", e);
            Toast.makeText(getContext(), R.string.error_groupfetchpings, Toast.LENGTH_SHORT).show();
        }

        mPings.clear();
        JSONArray pings = mOverlayView.mService.mPings;
        if (pings == null) {
            return;
        }
        for (int i = 0; i < pings.length(); i++) {
            try {
                JSONObject ping = pings.getJSONObject(i);
                addPing(ping.getString("type"), ping.getJSONArray("latlng"), ping.getString("details"), ping.getString("stream_id"), ping.getLong("stamp"));
            } catch (Exception e) {}
        }

        mLastGroupFetchPings = System.currentTimeMillis();
        Log.i(TAG, "LOADED_PINGS:"+mPings.size());
    }

    public PingMarker addPing(String pingType, JSONArray latlng, String details, String streamId, long stamp) {
        Drawable drawable = null;

        if (pingType.equals(PING_TARGET)) {
            drawable = getResources().getDrawable(R.drawable.ping_target);
        } else if (pingType.equals(PING_ASSIST)) {
            drawable = getResources().getDrawable(R.drawable.ping_assist);
        } else if (pingType.equals(PING_DANGER)) {
            drawable = getResources().getDrawable(R.drawable.ping_danger);
        }

        long dismiss = stamp + PING_LIFETIME;

        GeoPoint geoPoint = null;
        try {
            geoPoint = new GeoPoint(latlng.getDouble(1), latlng.getDouble(0));
        } catch (Exception e) {}

        PingMarker ping = new PingMarker(mMapView);
        ping.setPosition(geoPoint);
        ping.setIcon(drawable);
        ping.setTitle(pingType);
        ping.setSnippet(streamId.split("__")[0] + " (" + stamp + ")");
        ping.setSubDescription(details);
        ping.setInfoWindow(new MarkerInfoWindow(R.layout.bonuspack_bubble, mMapView));
        ping.setStamp(stamp);
        ping.setDismissTime(dismiss);

        mPings.add(ping);

        return ping;
    }

    public ArrayList<PingMarker> dismissPings() {
        long now = System.currentTimeMillis();
        ArrayList<PingMarker> removable = new ArrayList<PingMarker>();

        for (int i = 0; i < mPings.size(); i++) {
            PingMarker ping = mPings.get(i);
            if (now > ping.mDismissTime) {
                removable.add(ping);
            }
        }

        mParty.removeAll(removable);
        return removable;
    }

    public void migrateMarkers() {
        Marker self = new Marker(mMapView);
        self.setTitle(mSelf.getTitle());
        self.setSnippet(mSelf.getSnippet());
        self.setPosition(mSelf.getPosition());
        self.setIcon(getResources().getDrawable(R.drawable.self));
        mSelf = self;

        ArrayList<Marker> party = new ArrayList<Marker>();
        for (int i = 0; i < mParty.size(); i++) {
            Marker prev = mParty.get(i);
            Marker next = new Marker(mMapView);

            next.setTitle(prev.getTitle());
            next.setSnippet(prev.getSnippet());
            next.setPosition(prev.getPosition());
            next.setIcon(getResources().getDrawable(R.drawable.others));

            party.add(next);
        }
        mParty.clear();
        mParty.addAll(party);

        ArrayList<PingMarker> pings = new ArrayList<PingMarker>();
        for (int i = 0; i < mPings.size(); i++) {
            PingMarker prev = mPings.get(i);
            PingMarker next = new PingMarker(mMapView);

            next.setPosition(prev.getPosition());
            next.setTitle(prev.getTitle());
            next.setSnippet(prev.getSnippet());
            next.setSubDescription(prev.getSubDescription());
            next.setInfoWindow(new MarkerInfoWindow(R.layout.bonuspack_bubble, mMapView));
            next.setStamp(prev.mStamp);
            next.setDismissTime(prev.mDismissTime);

            Drawable drawable = null;
            if (next.getTitle().equals(PING_TARGET)) {
                drawable = getResources().getDrawable(R.drawable.ping_target);
            } else if (next.getTitle().equals(PING_ASSIST)) {
                drawable = getResources().getDrawable(R.drawable.ping_assist);
            } else if (next.getTitle().equals(PING_DANGER)) {
                drawable = getResources().getDrawable(R.drawable.ping_danger);
            }
            next.setIcon(drawable);

            pings.add(next);
        }
        mPings.clear();
        mPings.addAll(pings);
    }

    public class MyMapEventsReceiver implements MapEventsReceiver {
        @Override
        public boolean singleTapConfirmedHelper(final GeoPoint geoPoint) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(mPing);
            builder.setMessage("Obs.:");

            final EditText txtObs = new EditText(getContext());
            builder.setView(txtObs);

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.i(TAG, String.format("%s~%f+%f~%s", mPing, geoPoint.getLatitude(), geoPoint.getLongitude(), txtObs.getText().toString()));
                    final String pingString = mPing;

                    mPing = null;
                    mLastPingButton.setTextColor(mDefaultTextColor);
                    mLastPingButton = null;
                    mMapView.getOverlays().remove(mMapEventsOverlay);

                    final JSONArray latlng = new JSONArray();

                    try {
                        latlng.put(geoPoint.getLongitude());
                        latlng.put(geoPoint.getLatitude());
                    } catch (Exception e) {}

                    mOverlayView.mService.runConcurrentThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (pingString.equals(PING_TARGET)) {
                                    mOverlayView.mService.mPartyChannel.pingTargetOut(latlng, txtObs.getText().toString());
                                } else if (pingString.equals(PING_ASSIST)) {
                                    mOverlayView.mService.mPartyChannel.pingAssistOut(latlng, txtObs.getText().toString());
                                } else if (pingString.equals(PING_DANGER)) {
                                    mOverlayView.mService.mPartyChannel.pingDangerOut(latlng, txtObs.getText().toString());
                                }
                            } catch (Exception e) {
                                Toast.makeText(getContext(), R.string.error_ping, Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                    });
                }
            });
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    return;
                }
            });

            Dialog dialog = builder.create();
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            dialog.show();

            return false;
        }

        @Override
        public boolean longPressHelper(GeoPoint geoPoint) {
            return false;
        }
    };
}
