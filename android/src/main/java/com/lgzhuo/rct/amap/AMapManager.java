package com.lgzhuo.rct.amap;

import android.view.View;

import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.LayoutShadowNode;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.lgzhuo.rct.amap.cluster.ClusterPoint;
import com.lgzhuo.rct.amap.helper.ReadableArrayWrapper;
import com.lgzhuo.rct.amap.helper.ReadableMapWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Created by lgzhuo on 2017/3/7.
 */

class AMapManager extends ViewGroupManager<ReactAMapView> {

    private static final String REACT_NAME = "AMap";
    private static final int CENTER_COORDINATE = 1;
    private static final int CENTER_USER_LOCATION = 2;
    private static final int FIT_ANNOTATIONS = 3;
    private static final int FIT_REGION = 4;
    private static final int FIT_COORDINATES = 5;

    @Override
    public String getName() {
        return REACT_NAME;
    }

    @Override
    protected ReactAMapView createViewInstance(ThemedReactContext reactContext) {
        ReactAMapView view = new ReactAMapView(reactContext);
        view.onCreate(null);
        view.setUp();
        reactContext.addLifecycleEventListener(view);
        return view;
    }

    @Override
    public void onDropViewInstance(ReactAMapView view) {
        view.onDrop();
        super.onDropViewInstance(view);
    }

    @ReactProp(name = "showsUserLocation")
    public void setShowsUserLocation(ReactAMapView view, boolean shows) {
        view.setMyLocationEnabled(shows);
    }

    @ReactProp(name = "showsCompass")
    public void setShowsCompass(ReactAMapView view, boolean shows) {
        view.setCompassEnabled(shows);
    }

    @ReactProp(name = "showsScale")
    public void setShowsScale(ReactAMapView view, boolean shows) {
        view.setScaleControlsEnabled(shows);
    }

    @ReactProp(name = "showsZoomControl")
    public void setShowsZoomControl(ReactAMapView view, boolean shows) {
        view.setZoomControlsEnabled(shows);
    }

    @ReactProp(name = "clusterData")
    public void setClusterData(ReactAMapView view, ReadableArray array) {
        if (array != null) {
            List<ClusterPoint> points = new ArrayList<>(array.size());
            for (int i = 0; i < array.size(); i++) {
                ReadableMap map = array.getMap(i);
                String id = map.hasKey("id") ? map.getString("id") : null;
                points.add(new ClusterPoint(map.getDouble("latitude"), map.getDouble("longitude"), id, i));
            }
            view.setClusterPoints(points);
        } else {
            view.setClusterPoints(null);
        }
    }

    @ReactProp(name = "clusterSize")
    public void setClusterSize(ReactAMapView view, float clusterSize) {
        view.setClusterSize(clusterSize);
    }

    @ReactProp(name = "moveOnMarkerPress", defaultBoolean = true)
    public void setMoveOnMarkerPress(ReactAMapView view, boolean moveOnMarkerPress) {
        view.setMoveOnMarkerPress(moveOnMarkerPress);
    }

    @ReactProp(name = "region")
    public void setRegion(ReactAMapView view, ReadableMap region) {
        view.setRegion(region);
    }

    @Override
    @Nullable
    public Map getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.of(
                "onCluster", MapBuilder.of("registrationName", "onCluster"),
                "onMapReady", MapBuilder.of("registrationName", "onMapReady")
        );
    }

    @Nullable
    @Override
    public Map<String, Integer> getCommandsMap() {
        return MapBuilder.of("centerCoordinate", CENTER_COORDINATE,
                "centerUserLocation", CENTER_USER_LOCATION,
                "fitAnnotations", FIT_ANNOTATIONS,
                "fitRegion", FIT_REGION,
                "fitCoordinates", FIT_COORDINATES
        );
    }

    @Override
    public void receiveCommand(ReactAMapView root, int commandId, @Nullable ReadableArray args) {
        switch (commandId) {
            case CENTER_COORDINATE:
                if (args != null && !args.isNull(0)) {
                    ReadableMap map = args.getMap(0);
                    boolean animate = args.getBoolean(1);
                    if (map.hasKey("latitude") && map.hasKey("longitude")) {
                        LatLng latLng = new LatLng(map.getDouble("latitude"), map.getDouble("longitude"));
                        AMapCommandHelper.centerCoordinate(root, latLng, animate);
                    }
                }
                break;
            case CENTER_USER_LOCATION: {
                boolean animate = args == null || args.getBoolean(0);
                AMapCommandHelper.centerUserLocation(root, animate);
            }
            break;
            case FIT_ANNOTATIONS: {
                int argSize = args == null ? 0 : args.size();
                int padding = argSize > 0 ? (int) args.getDouble(0) : 50;
                boolean animate = argSize <= 1 || args.getBoolean(1);
                AMapCommandHelper.fitAnnotations(root, padding, animate);
            }
            break;
            case FIT_REGION: {
                args = ReadableArrayWrapper.wrap(args);
                ReadableMap region = args.getMap(0);
                if (region == null)
                    break;
                boolean animate = args.getBoolean(1);
                int duration = args.getInt(2);
                double lng = region.getDouble("longitude");
                double lat = region.getDouble("latitude");
                double lngDelta = region.getDouble("longitudeDelta");
                double latDelta = region.getDouble("latitudeDelta");
                LatLngBounds bounds = new LatLngBounds(
                        new LatLng(lat - latDelta / 2, lng - lngDelta / 2), // southwest
                        new LatLng(lat + latDelta / 2, lng + lngDelta / 2)  // northeast
                );
                AMapCommandHelper.fitRegion(root, bounds, animate, duration);
            }
            break;
            case FIT_COORDINATES: {
                args = ReadableArrayWrapper.wrap(args);
                ReadableArray coordinates = args.getArray(0);
                if (coordinates == null || coordinates.size() == 0)
                    break;
                LatLngBounds.Builder boundBuilder = new LatLngBounds.Builder();
                for (int i = 0; i < coordinates.size(); i++) {
                    ReadableMap map = ReadableMapWrapper.wrap(coordinates.getMap(i));
                    double latitude = map.getDouble("latitude");
                    double longitude = map.getDouble("longitude");
                    boundBuilder.include(new LatLng(latitude, longitude));
                }
                ReadableMap edgePadding = ReadableMapWrapper.wrap(args.getMap(1));
                boolean animate = args.getBoolean(2);
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(boundBuilder.build(), 50);
                root.setPadding(edgePadding.getInt("left"), edgePadding.getInt("top"), edgePadding.getInt("right"), edgePadding.getInt("bottom"));
                AMapCommandHelper.updateCamera(root, cameraUpdate, animate);
                root.setPadding(0, 0, 0, 0);
            }
            break;
            default:
                super.receiveCommand(root, commandId, args);
                break;
        }
    }

    @Override
    public void addView(ReactAMapView parent, View child, int index) {
        parent.addFeature(child, index);
    }

    @Override
    public void removeViewAt(ReactAMapView parent, int index) {
        parent.removeFeatureAt(index);
    }

    @Override
    public int getChildCount(ReactAMapView parent) {
        return parent.getFeatureCount();
    }

    @Override
    public View getChildAt(ReactAMapView parent, int index) {
        return parent.getFeatureAt(index);
    }

    @Override
    public LayoutShadowNode createShadowNodeInstance() {
        // A custom shadow node is needed in order to pass back the width/height of the map to the
        // view manager so that it can start applying camera moves with bounds.
        return new SizeReportingShadowNode();
    }

    @Override
    public void updateExtraData(ReactAMapView view, Object extraData) {
        view.updateExtraData(extraData);
    }
}
