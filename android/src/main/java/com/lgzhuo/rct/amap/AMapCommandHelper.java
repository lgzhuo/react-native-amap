package com.lgzhuo.rct.amap;

import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;

/**
 * Created by lgzhuo on 2017/3/8.
 */

class AMapCommandHelper {
    static void centerCoordinate(ReactAMapView view, LatLng latLng, boolean animate) {
        CameraUpdate cameraUpdate = CameraUpdateFactory.changeLatLng(latLng);
        updateCamera(view, cameraUpdate, animate);
    }

    static void centerUserLocation(ReactAMapView view, boolean animate) {
        LatLng latLng = view.getMyLastKnownLatLng();
        CameraUpdate cameraUpdate = CameraUpdateFactory.changeLatLng(latLng);
        updateCamera(view, cameraUpdate, animate);
    }

    static void fitAnnotations(ReactAMapView view, int padding, boolean animate) {
        LatLngBounds bounds = view.getMarkerBounds();
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        updateCamera(view, cameraUpdate, animate);
    }

    static void fitRegion(ReactAMapView view, LatLngBounds bounds, boolean animate, int duration) {
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 0);
        updateCamera(view, cameraUpdate, animate, duration);
    }

    static void updateCamera(ReactAMapView view, CameraUpdate cameraUpdate, boolean animate) {
        updateCamera(view, cameraUpdate, animate, 0);
    }

    static void updateCamera(ReactAMapView view, CameraUpdate cameraUpdate, boolean animate, int duration) {
        if (animate) {
            if (duration > 0) {
                view.getMap().animateCamera(cameraUpdate, duration, null);
            } else {
                view.getMap().animateCamera(cameraUpdate);
            }
        } else {
            view.getMap().moveCamera(cameraUpdate);
        }
    }
}
