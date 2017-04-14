package com.lgzhuo.rct.amap;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.Polyline;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.lgzhuo.rct.amap.cluster.Cluster;
import com.lgzhuo.rct.amap.cluster.ClusterComputer;
import com.lgzhuo.rct.amap.cluster.ClusterPoint;
import com.lgzhuo.rct.amap.cluster.OnClusterListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Created by lgzhuo on 2017/3/8.
 */

class ReactAMapView extends MapView implements LocationSource, LifecycleEventListener, AMapLocationListener, AMap.OnMarkerClickListener, AMap.InfoWindowAdapter, AMap.OnInfoWindowClickListener, AMap.OnPolylineClickListener, AMap.OnCameraChangeListener, OnClusterListener, AMap.OnMapLoadedListener {

    private AMapLocationClient mLocationClient;
    private LocationSource.OnLocationChangedListener mLocationChangeListener;
    private List<AMapFeature> mFeatureList = new ArrayList<>();
    private Map<Marker, AMapMarker> mMarkerMap = new WeakHashMap<>();
    private Map<Polyline, AMapPolyline> mPolylineMap = new WeakHashMap<>();
    private ClusterComputer mClusterComputer = new ClusterComputer();

    public ReactAMapView(Context context) {
        super(context);
    }

    public ReactAMapView(Context context, AMapOptions aMapOptions) {
        super(context, aMapOptions);
    }

    public void setUp() {
        mClusterComputer.setOnClusterListener(this);
        AMap map = getMap();
        if (map != null) {
            map.setOnMarkerClickListener(this);
            map.setInfoWindowAdapter(this);
            map.setOnInfoWindowClickListener(this);
            map.setOnPolylineClickListener(this);
            map.setOnCameraChangeListener(this);
            map.setOnMapLoadedListener(this);
        }
    }

    public void setMyLocationEnabled(boolean enabled) {
        if (enabled && mLocationClient == null) {
            mLocationClient = new AMapLocationClient(getContext());
            mLocationClient.setLocationListener(this);
            AMapLocationClientOption locationOption = new AMapLocationClientOption();
            locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            mLocationClient.setLocationOption(locationOption);
        }
        AMap map = getMap();
        if (map != null) {
            map.setLocationSource(this);
            map.setMyLocationType(AMap.LOCATION_TYPE_LOCATE);
            map.setMyLocationEnabled(enabled);
        }
    }

    public void setCompassEnabled(boolean shows) {
        AMap map = getMap();
        if (map != null) {
            map.getUiSettings().setCompassEnabled(shows);
        }
    }

    public void setScaleControlsEnabled(boolean shows) {
        AMap map = getMap();
        if (map != null) {
            map.getUiSettings().setScaleControlsEnabled(shows);
        }
    }

    public void setZoomControlsEnabled(boolean shows) {
        AMap map = getMap();
        if (map != null) {
            map.getUiSettings().setZoomControlsEnabled(shows);
        }
    }

    public void setClusterPoints(List<ClusterPoint> points) {
        mClusterComputer.setPoints(points);
    }

    public void setClusterSize(float size) {
        mClusterComputer.setSize(size);
    }

    public void onDrop() {
        mClusterComputer.close();
        onHostDestroy();
        ((ReactContext) getContext()).removeLifecycleEventListener(this);
    }

    /*LifecycleEventListener*/

    @Override
    public void onHostResume() {
        onResume();
        AMap map = getMap();
        if (map != null && map.isMyLocationEnabled()) {
            mLocationClient.startLocation();
        }
    }

    @Override
    public void onHostPause() {
        onPause();
        if (mLocationClient.isStarted()) {
            mLocationClient.stopLocation();
        }
    }

    @Override
    public void onHostDestroy() {
        onDestroy();
        if (mLocationClient.isStarted()) {
            mLocationClient.stopLocation();
        }
        mLocationClient.onDestroy();
    }

    public LatLngBounds getMarkerBounds() {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : mMarkerMap.keySet()) {
            builder.include(marker.getPosition());
        }
        return builder.build();
    }

    public AMapLocation getMyLastKnownLocation() {
        return mLocationClient.getLastKnownLocation();
    }

    public LatLng getMyLastKnownLatLng() {
        AMapLocation location = getMyLastKnownLocation();
        if (location != null && location.getErrorCode() == 0) {
            return new LatLng(location.getLatitude(), location.getLongitude());
        } else {
            return null;
        }
    }

    public void addFeature(View child, int index) {
        if (child instanceof AMapFeature) {
            ((AMapFeature) child).addToMap(getMap());
            mFeatureList.add(index, (AMapFeature) child);

            if (child instanceof AMapMarker) {
                AMapMarker aMarker = (AMapMarker) child;
                mMarkerMap.put(aMarker.getFeature(), aMarker);
            } else if (child instanceof AMapPolyline) {
                AMapPolyline aPolyline = (AMapPolyline) child;
                mPolylineMap.put(aPolyline.getFeature(), aPolyline);
            }
        }
    }

    public void removeFeatureAt(int index) {
        AMapFeature feature = mFeatureList.remove(index);
        feature.removeFromMap(getMap());
        if (feature instanceof AMapMarker) {
            mMarkerMap.remove(((AMapMarker) feature).getFeature());
        } else if (feature instanceof AMapPolyline) {
            mPolylineMap.remove(((AMapPolyline) feature).getFeature());
        }
    }

    public int getFeatureCount() {
        return mFeatureList.size();
    }

    public AMapFeature getFeatureAt(int index) {
        return mFeatureList.get(index);
    }

     /* LocationSource */

    @Override
    public void activate(LocationSource.OnLocationChangedListener onLocationChangedListener) {
        mLocationChangeListener = onLocationChangedListener;
        mLocationClient.startLocation();
    }

    @Override
    public void deactivate() {
        mLocationClient.stopLocation();
    }

    /* AMapLocationListener */

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (mLocationChangeListener != null && aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                mLocationChangeListener.onLocationChanged(aMapLocation);
            } else {
                String errText = "定位失败," + aMapLocation.getErrorCode() + ": " + aMapLocation.getErrorInfo();
                Log.e("AmapErr", errText);
            }
        }
    }

    /* AMap.OnMarkerClickListener */

    @Override
    public boolean onMarkerClick(Marker marker) {
        AMapMarker aMarker = mMarkerMap.get(marker);
        if (aMarker != null) {
            aMarker.pushOnPressEvent();
        }
        return false;
    }

    /* AMap.InfoWindowAdapter */

    @Override
    public View getInfoWindow(Marker marker) {
        AMapMarker aMarker = mMarkerMap.get(marker);
        return aMarker == null ? null : aMarker.getInfoWindow();
    }

    @Override
    public View getInfoContents(Marker marker) {
        AMapMarker aMarker = mMarkerMap.get(marker);
        return aMarker == null ? null : aMarker.getInfoContents();
    }

    /* AMap.OnInfoWindowClickListener */

    @Override
    public void onInfoWindowClick(Marker marker) {
        AMapMarker aMarker = mMarkerMap.get(marker);
        if (aMarker != null) {
            aMarker.pushOnCalloutPressEvent();
        }
    }

    /* AMap.OnPolylineClickListener */

    @Override
    public void onPolylineClick(Polyline polyline) {
        AMapPolyline aPolyline = mPolylineMap.get(polyline);
        if (aPolyline != null) {
            aPolyline.pushOnPressEvent();
        }
    }

    /* AMap.OnCameraChangeListener */

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {

    }

    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {
        AMap map = getMap();
        mClusterComputer.setScaleAndBounds(map.getScalePerPixel(), map.getProjection().getVisibleRegion().latLngBounds);
    }

    /* OnClusterListener */

    @Override
    public void onClusterComplete(List<Cluster> clusterList) {
        WritableMap event = Arguments.createMap();
        event.putArray("clustered", AMapUtils.ClusterConvert.cnvArr(clusterList));
        pushEvent("onCluster", event);
    }

    /* Events */
    public void pushEvent(String eventName, WritableMap event) {
        ReactContext reactContext = (ReactContext) getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(getId(), eventName, event);
    }

    /* AMap.OnMapLoadedListener */

    @Override
    public void onMapLoaded() {
        mClusterComputer.setScaleAndBounds(getMap().getScalePerPixel(), getMap().getProjection().getVisibleRegion().latLngBounds);
    }
}
