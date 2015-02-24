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
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.util.ArrayList;
import java.util.List;

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

    public ArrayList<Marker> mParty = new ArrayList<Marker>();
    public ArrayList<PingMarker> mPings = new ArrayList<PingMarker>();

    public long mLastPan;
    public final long PARTY_INTERVAL = 1000 * 10;
    public final long PING_INTERVAL = 1000 * 10;

    public MapEventsOverlay mMapEventsOverlay;
    public MyMapEventsReceiver mMapEventsReceiver;

    public String mPing;

    public int mDefaultTextColor;
    public Button mLastPingButton;

    public LinearLayout mOptions;

    public MinimapView(Context context, OverlayView overlayView) {
        super(context, overlayView);

        mContentLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        mContentLayout.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    public void show() {
        super.show();

        if (mHasLoaded) {
            super.showLoaded();
            return;
        }
        else {
            mHasLoaded = true;
        }

        mOverlayView.mService.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int contentWidth = (int) (mOverlayView.mWidth);
                int contentHeight = (int) (mOverlayView.mHeight * 0.8);

                mMapView = new MapView(getContext(), 10);
                mMapView.setTileSource(TileSourceFactory.MAPNIK);
                mMapView.setBuiltInZoomControls(true);
                mMapView.setMultiTouchControls(true);
                mMapView.setLayoutParams(new LinearLayout.LayoutParams(contentWidth, contentHeight));
                mContentLayout.addView(mMapView);

                mMapController = mMapView.getController();
                mMapController.setZoom(19);

                mMapEventsReceiver = new MyMapEventsReceiver();
                mMapEventsOverlay = new MapEventsOverlay(getContext(), mMapEventsReceiver);
//        mMapView.getOverlays().add(eventsOverlay);

                try {
                    loadSelf();
                    mMapController.setCenter(mSelf.getPosition());

                } catch (Exception e) {
                    Log.e(TAG, "could not retrieve current latlng");
                }

                mOptions = new LinearLayout(getContext());
                mOptions.setGravity(Gravity.CENTER);
                mOptions.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                mContentLayout.addView(mOptions);

                Button bPan = new Button(getContext());
                bPan.setText("PAN");
                bPan.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mOverlayView.mService.runConcurrentThread(new Runnable() {
                            @Override
                            public void run() {
                                loadSelf();
                                loadParty();
                                loadPings();
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

                mDefaultTextColor = bPan.getCurrentTextColor();

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

                MinimapView.super.showLoaded();

                mOverlayView.mService.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            loadParty();
                            loadPings();
                            mMapView.invalidate();
                        } catch (Exception e) {}
                    }
                });
            }
        });
    }

    public void loadSelf() {
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
                mSelf.setSnippet(mOverlayView.mService.mHashtags);

                mMapView.getOverlays().add(mSelf);
            }
            mSelf.setPosition(myLatlng);
            mSelf.setIcon(drawable);

        } catch (Exception e) {
            Log.e(TAG, "could not retrieve self", e);
        }
    }

    public void loadParty() {
        if (System.currentTimeMillis() - mLastPan < PARTY_INTERVAL) {
            return;
        }

        mMapView.getOverlays().removeAll(mParty);
        mParty.clear();

        try {
            mOverlayView.mService.mMapperChannel.groupFetchMembers();
        } catch (Exception e) {
            Log.e(TAG, "loadParty", e);
            Toast.makeText(getContext(), R.string.error_groupfetchmembers, Toast.LENGTH_SHORT).show();
        }

        mParty = new ArrayList<Marker>();

        JSONArray members = mOverlayView.mService.mParty;
        for (int i = 0; i < members.length(); i++) {
            Marker marker = new Marker(mMapView);
            try {
                JSONObject member = members.getJSONObject(i);
                marker.setTitle(member.getString("jid"));
                marker.setSnippet(member.getString("hashtags"));

                JSONArray latlng = member.getJSONArray("latlng");
                GeoPoint geoPoint = new GeoPoint(latlng.getDouble(1), latlng.getDouble(0));
                marker.setPosition(geoPoint);
                marker.setIcon(getResources().getDrawable(R.drawable.others));

                mParty.add(marker);
                mMapView.getOverlays().add(marker);
            } catch (Exception e) {}
        }
    }

    public void loadPing(String pingType, JSONArray latlng, String details, String streamId, long dismiss) {
        Drawable drawable = null;

        if (pingType.equals(PING_TARGET)) {
            drawable = getResources().getDrawable(R.drawable.ping_target);
        } else if (pingType.equals(PING_ASSIST)) {
            drawable = getResources().getDrawable(R.drawable.ping_assist);
        } else if (pingType.equals(PING_DANGER)) {
            drawable = getResources().getDrawable(R.drawable.ping_danger);
        }

        GeoPoint geoPoint = null;
        try {
            geoPoint = new GeoPoint(latlng.getDouble(1), latlng.getDouble(0));
        } catch (Exception e) {}

        PingMarker ping = new PingMarker(mMapView);
        ping.setPosition(geoPoint);
        ping.setIcon(drawable);
        ping.setTitle(pingType);
        ping.setSnippet(streamId.split("__")[0]);
        ping.setSubDescription(details);
        ping.setInfoWindow(new MarkerInfoWindow(R.layout.bonuspack_bubble, mMapView));
        ping.setDismissTime(dismiss);

        mPings.add(ping);
        mMapView.getOverlays().add(ping);
    }

    public void loadPings() {
        if (System.currentTimeMillis() - mLastPan < PING_INTERVAL) {
            return;
        }

        long now = System.currentTimeMillis();
        ArrayList<PingMarker> removable = new ArrayList<PingMarker>();

        for (int i = 0; i < mPings.size(); i++) {
            PingMarker ping = mPings.get(i);
            if (now > ping.mDismissTime) {
                removable.add(ping);
            }
        }
        mMapView.getOverlays().removeAll(removable);
        mParty.removeAll(removable);
        removable.clear();
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
