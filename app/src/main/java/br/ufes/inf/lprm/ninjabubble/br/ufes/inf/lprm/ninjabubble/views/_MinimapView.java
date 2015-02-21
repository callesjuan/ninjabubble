package br.ufes.inf.lprm.ninjabubble.br.ufes.inf.lprm.ninjabubble.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

import br.ufes.inf.lprm.ninjabubble.R;

/**
 * Created by Juan on 09/02/2015.
 */
public class _MinimapView extends ContentView {
    public final String TAG = "NinjaBubbleMagic/MinimapView";

//    public MapView mMapView;
//    public IMapController mMapController;
//    public OverlayItem mSelf;

    public _MinimapView(Context context, OverlayView overlayView) {
        super(context, overlayView);
    }

    @Override
    public void show() {
//        super.show();
//
//        if (mHasLoaded) {
//            showLoaded();
//            return;
//        }
//        else {
//            mHasLoaded = true;
//        }
//
//        mMapView = new MapView(getContext(), 10);
//        mMapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
//        mMapView.setBuiltInZoomControls(true);
//        mMapView.setMultiTouchControls(true);
//        mContentLayout.addView(mMapView);
//
//        mMapView.setOnTouchListener(new OnTouchListener() {
//            private int TOUCH_TIME_THRESHOLD = 200;
//            private long lastTouchDown;
//
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                switch (event.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//                        lastTouchDown = System.currentTimeMillis();
//                        return true;
//                    case MotionEvent.ACTION_UP:
//                        if (System.currentTimeMillis() - lastTouchDown < TOUCH_TIME_THRESHOLD) {
//                            Projection proj = mMapView.getProjection();
//                            GeoPoint p = (GeoPoint) proj.fromPixels((int) event.getX(), (int) event.getY());
//                            Log.i(TAG, "lat:" + p.getLatitude() + ";lng:" + p.getLongitude());
//                            return true;
//                        }
//                }
//                return false;
//            }
//        });
//
//        mMapController = mMapView.getController();
//        mMapController.setZoom(19);
//
//        try {
//            double lat = mOverlayView.mService.mLatlng.getDouble(1);
//            double lng = mOverlayView.mService.mLatlng.getDouble(0);
//            GeoPoint myLatlng = new GeoPoint(lat, lng);
//
//            mSelf = new OverlayItem("self", "Current position", myLatlng);
//            Drawable drawable = getResources().getDrawable(R.drawable.self);
//            mSelf.setMarker(drawable);
//
//            ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();
//            items.add(mSelf);
//
//            Overlay overlay = new ItemizedIconOverlay<OverlayItem>(getContext(), items,
//                    new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
//                        @Override
//                        public boolean onItemSingleTapUp(int index, OverlayItem item) {
//                            Log.i(TAG, "lat:"+item.getPoint().getLatitude()+";lng:"+item.getPoint().getLongitude());
//                            if (item.getTitle().equals("self")) {
//                                Bitmap bmOriginal = BitmapFactory.decodeResource(getResources(), R.drawable.self);
//                                Bitmap bmResult = Bitmap.createBitmap(bmOriginal.getWidth(), bmOriginal.getHeight(), Bitmap.Config.ARGB_8888);
//                                Canvas tempCanvas = new Canvas(bmResult);
//                                tempCanvas.rotate(mOverlayView.mService.mSensorEventListener.mCurrentDegree, bmOriginal.getWidth() / 2, bmOriginal.getHeight() / 2);
//                                tempCanvas.drawBitmap(bmOriginal, 0, 0, null);
//
//                                Drawable drawable = new BitmapDrawable(getResources(), bmResult);
//
//                                mOverlayView.vMinimap.mSelf.setMarker(drawable);
//                                mOverlayView.vMinimap.mMapView.invalidate();
//                            }
//                            return false;
//                        }
//
//                        @Override
//                        public boolean onItemLongPress(int index, OverlayItem item) {
//                            return false;
//                        }
//                    });
//
//            mMapView.getOverlays().add(overlay);
//
//            mMapController.setCenter(myLatlng);
//
//        } catch (Exception e) {
//            Log.e(TAG, "could not retrieve current latlng");
//        }
//
////        mContentLayout.addView(imSelf);
//
//        showLoaded();
    }
}
