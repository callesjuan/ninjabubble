package br.ufes.inf.lprm.ninjabubble.br.ufes.inf.lprm.ninjabubble.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.ImageView;

import org.osmdroid.api.IMap;
import org.osmdroid.api.IMapController;
import org.osmdroid.api.Marker;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
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

    public HomeView vHome;
    public ChatView vChat;
    public PartyView vParty;

    public MapView mMapView;
    public IMap mMap;
    public IMapController mMapController;

    public ImageView imSelf;
    public Drawable dwSelf;

    public OverlayItem mSelf;

    public MinimapView(Context context, OverlayView overlayView) {
        super(context, overlayView);

        imSelf = new ImageView(getContext());
        imSelf.setImageResource(R.drawable.self);

        dwSelf = getResources().getDrawable(R.drawable.self);

        mMapView = new MapView(getContext(), 10);
        mMapView.setTileSource(TileSourceFactory.MAPNIK);
        mMapView.setBuiltInZoomControls(true);
        mMapView.setMultiTouchControls(true);

        mMapController = mMapView.getController();
        mMapController.setZoom(19);

        try {
            double lat = mOverlayView.mService.mLatlng.getDouble(1);
            double lng = mOverlayView.mService.mLatlng.getDouble(0);
            GeoPoint myLatlng = new GeoPoint(lat, lng);


            mSelf = new OverlayItem("Here", "Current position", myLatlng);
            Drawable drawable = getResources().getDrawable(R.drawable.self);
            mSelf.setMarker(drawable);

            ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();
            items.add(mSelf);

            Overlay overlay = new ItemizedIconOverlay<OverlayItem>(getContext(), items,
                    new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                        @Override
                        public boolean onItemSingleTapUp(int index, OverlayItem item) {
                            Log.i(TAG, "lat:"+item.getPoint().getLatitude()+";lng:"+item.getPoint().getLongitude());
                            return false;
                        }

                        @Override
                        public boolean onItemLongPress(int index, OverlayItem item) {
                            return false;
                        }
                    });

            mMapView.getOverlays().add(overlay);

            mMapController.setCenter(myLatlng);

        } catch (Exception e) {
            Log.e(TAG, "could not retrieve current latlng");
        }

        mContentLayout.addView(mMapView);

//        mContentLayout.addView(imSelf);
    }

    public void setFamily(HomeView homeView, ChatView chatView, PartyView partyView) {
        vHome = homeView;
        vChat = chatView;
        vParty = partyView;
    }
}
