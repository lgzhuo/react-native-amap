package rct.amap;

import android.content.Context;

import com.amap.api.maps.AMap;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import java.util.List;

/**
 * Created by lgzhuo on 2017/3/16.
 */

public class AMapPolyline extends AMapFeature {

    private PolylineOptions polylineOptions = new PolylineOptions();
    private Polyline polyline;

    public AMapPolyline(Context context) {
        super(context);
    }

    public void setPoints(List<LatLng> points) {
        polylineOptions.setPoints(points);
        if (polyline != null) {
            polyline.setPoints(points);
        }
    }

    public void setColor(int color) {
        polylineOptions.color(color);
        if (polyline != null) {
            polyline.setColor(color);
        }
    }

    public void setWidth(float width) {
        polylineOptions.width(width);
        if (polyline != null) {
            polyline.setWidth(width);
        }
    }

    public void setZIndex(float zIndex) {
        polylineOptions.zIndex(zIndex);
        if (polyline != null) {
            polyline.setZIndex(zIndex);
        }
    }

    public void setGeodesic(boolean geodesic) {
        polylineOptions.geodesic(geodesic);
        if (polyline != null) {
            polyline.setGeodesic(geodesic);
        }
    }

    public void setDottedLine(boolean dottedLine) {
        polylineOptions.setDottedLine(dottedLine);
        if (polyline != null) {
            polyline.setDottedLine(dottedLine);
        }
    }

    @Override
    public void addToMap(AMap map) {
        polyline = map.addPolyline(polylineOptions);
    }

    @Override
    public void removeFromMap(AMap map) {
        polyline.remove();
    }

    @Override
    public Polyline getFeature() {
        return polyline;
    }

    /* Events */
    public void pushEvent(String eventName, WritableMap event) {
        ReactContext reactContext = (ReactContext) getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(getId(), eventName, event);
    }

    public void pushOnPressEvent() {
        pushEvent("onPress", null);
    }
}
