package br.ufes.inf.lprm.ninjabubble.br.ufes.inf.lprm.ninjabubble.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

    public MapView mMapView;
    public IMapController mMapController;
    public OverlayItem mSelf;

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

        mOptions = new LinearLayout(getContext());
        mOptions.setGravity(Gravity.CENTER);
        mOptions.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mContentLayout.addView(mOptions);

        mMapController = mMapView.getController();
        mMapController.setZoom(19);

        MapEventsReceiver eventsReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint geoPoint) {
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint geoPoint) {
                Log.i(TAG, String.format("longPressHelper %s %s", geoPoint.getLatitude(), geoPoint.getLongitude()));
                return false;
            }
        };
        MapEventsOverlay eventsOverlay = new MapEventsOverlay(getContext(), eventsReceiver);
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

        Button bPan = new Button(getContext());
        bPan.setText("<o>");
        bPan.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                loadSelf();
                mMapController.setCenter(mSelf.getPoint());
                mMapView.invalidate();
            }
        });
        mOptions.addView(bPan);

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
}
