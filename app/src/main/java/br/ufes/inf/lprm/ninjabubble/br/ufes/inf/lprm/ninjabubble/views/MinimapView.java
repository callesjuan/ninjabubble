package br.ufes.inf.lprm.ninjabubble.br.ufes.inf.lprm.ninjabubble.views;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
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
import android.widget.TextView;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.overlays.MapEventsOverlay;
import org.osmdroid.bonuspack.overlays.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.util.ArrayList;

import br.ufes.inf.lprm.ninjabubble.R;

/**
 * Created by Juan on 09/02/2015.
 */
public class MinimapView extends ContentView {
    public final String TAG = "NinjaBubbleMagic/MinimapView";

    public final String PING_ASSIST = "ASSIST";
    public final String PING_DANGER = "DANGER";

    public MapView mMapView;
    public IMapController mMapController;
    public OverlayItem mSelf;

    public ArrayList<OverlayItem> mMyPings = new ArrayList<OverlayItem>();
    public ArrayList<OverlayItem> mPartyPings = new ArrayList<OverlayItem>();
    public ArrayList<OverlayItem> mParty = new ArrayList<OverlayItem>();

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
            showLoaded();
            return;
        }
        else {
            mHasLoaded = true;
        }

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

            ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();
            items.add(mSelf);

            Overlay overlay = new ItemizedIconOverlay<OverlayItem>(getContext(), items,
                    new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                        @Override
                        public boolean onItemSingleTapUp(int index, OverlayItem item) {
                            return false;
                        }

                        @Override
                        public boolean onItemLongPress(int index, OverlayItem item) {
                            return false;
                        }
                    });

            mMapView.getOverlays().add(overlay);

            mMapController.setCenter(mSelf.getPoint());

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
                loadSelf();
                mMapController.setCenter(mSelf.getPoint());
                mMapView.invalidate();
            }
        });
        mOptions.addView(bPan);

        mDefaultTextColor = bPan.getCurrentTextColor();

        final Button bPingAssist = new Button(getContext());
        bPingAssist.setText(PING_ASSIST);
        bPingAssist.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPing == null) {
                    mPing = PING_ASSIST;
                    bPingAssist.setTextColor(0xff0000);
                    mLastPingButton = bPingAssist;

                    mMapView.getOverlays().add(mMapEventsOverlay);
                } else if (mPing.equals(PING_ASSIST)) {
                    mPing = null;
                    bPingAssist.setTextColor(mDefaultTextColor);
                    mLastPingButton = null;

                    mMapView.getOverlays().remove(mMapEventsOverlay);
                } else if (!mPing.equals(PING_ASSIST)) {
                    mPing = PING_ASSIST;
                    bPingAssist.setTextColor(0xff0000);

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
                    bPingDanger.setTextColor(0xFF0000);
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
                    bPingDanger.setTextColor(0xFF0000);

                    mLastPingButton.setTextColor(mDefaultTextColor);
                    mLastPingButton = bPingDanger;
                }
            }
        });
        mOptions.addView(bPingDanger);

        showLoaded();
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
                mSelf = new OverlayItem("self", "current position", myLatlng);
            }
            mSelf.setMarker(drawable);

        } catch (Exception e) {
            Log.e(TAG, "could not retrieve self", e);
        }
    }

    public class MyMapEventsReceiver implements MapEventsReceiver {
        @Override
        public boolean singleTapConfirmedHelper(GeoPoint geoPoint) {
            return false;
        }

        @Override
        public boolean longPressHelper(final GeoPoint geoPoint) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(mPing);
            builder.setMessage("Obs.:");

            final EditText txtObs = new EditText(getContext());
            builder.setView(txtObs);

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.i(TAG, String.format("%s~%f+%f~%s", mPing, geoPoint.getLatitude(), geoPoint.getLongitude(), txtObs.getText().toString()));
                    mPing = null;
                    mLastPingButton.setTextColor(mDefaultTextColor);
                    mLastPingButton = null;
                    mMapView.getOverlays().remove(mMapEventsOverlay);

                    OverlayItem ping = new OverlayItem(mPing, txtObs.getText().toString(), geoPoint);
                    Drawable drawable = getResources().getDrawable(android.R.drawable.ic_menu_myplaces);
                    ping.setMarker(drawable);

                    ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();
                    items.add(ping);

                    Overlay overlay = new ItemizedIconOverlay<OverlayItem>(getContext(), items,
                        new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                            @Override
                            public boolean onItemSingleTapUp(int index, OverlayItem item) {
                                Log.i(TAG, item.getSnippet());
                                return false;
                            }

                            @Override
                            public boolean onItemLongPress(int index, OverlayItem item) {
                                return false;
                            }
                        });

                    mMapView.getOverlays().add(overlay);
                    mMapView.invalidate();
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
    };
}
