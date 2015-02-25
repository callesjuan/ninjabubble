package br.ufes.inf.lprm.ninjabubble.br.ufes.inf.lprm.ninjabubble.views;

import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.views.MapView;

/**
 * Created by Juan on 23/02/2015.
 */
public class PingMarker extends Marker {
    public long mStamp;
    public long mDismissTime;

    public PingMarker(MapView mapView) {
        super(mapView);
    }

    public void setStamp(long time) {
        mStamp = time;
    }

    public void setDismissTime(long time) {
        mDismissTime = time;
    }
}
