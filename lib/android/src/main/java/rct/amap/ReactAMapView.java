package rct.amap;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.TextureMapView;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.Polyline;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import rct.amap.cluster.Cluster;
import rct.amap.cluster.ClusterComputer;
import rct.amap.cluster.ClusterPoint;
import rct.amap.cluster.OnClusterListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Created by lgzhuo on 2017/3/8.
 * <p>
 * 2017/4/30 将继承由MapView 改为TextureMapView解决当有多个AMapView instance时第一个叠加在后一个上的问题
 * 见react-native-maps issues https://github.com/airbnb/react-native-maps/issues/453
 */

class ReactAMapView extends TextureMapView implements LocationSource, LifecycleEventListener, AMapLocationListener, AMap.OnMarkerClickListener, AMap.InfoWindowAdapter, AMap.OnInfoWindowClickListener, AMap.OnPolylineClickListener, AMap.OnCameraChangeListener, OnClusterListener, AMap.OnMapLoadedListener {

    private AMapLocationClient mLocationClient;
    private LocationSource.OnLocationChangedListener mLocationChangeListener;
    private List<AMapFeature> mFeatureList = new ArrayList<>();
    private Map<Marker, AMapMarker> mMarkerMap = new WeakHashMap<>();
    private Map<Polyline, AMapPolyline> mPolylineMap = new WeakHashMap<>();
    private ClusterComputer mClusterComputer = new ClusterComputer();
    private boolean mMoveOnMarkerPress;
    private LatLngBounds boundsToMove;

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
            map.setOnMapLoadedListener(this);
            map.setOnMarkerClickListener(this);
            map.setInfoWindowAdapter(this);
            map.setOnInfoWindowClickListener(this);
            map.setOnPolylineClickListener(this);
            map.setOnCameraChangeListener(this);
            map.setOnMapLoadedListener(this);
        }
    }

    public void setMyLocationEnabled(boolean enabled) {
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

    public void setMoveOnMarkerPress(boolean moveOnPress) {
        this.mMoveOnMarkerPress = moveOnPress;
    }

    public void setRegion(ReadableMap region) {
        if (region == null) return;

        Double lng = region.getDouble("longitude");
        Double lat = region.getDouble("latitude");
        Double lngDelta = region.getDouble("longitudeDelta");
        Double latDelta = region.getDouble("latitudeDelta");
        LatLngBounds bounds = new LatLngBounds(
                new LatLng(lat - latDelta / 2, lng - lngDelta / 2), // southwest
                new LatLng(lat + latDelta / 2, lng + lngDelta / 2)  // northeast
        );
        if (super.getHeight() <= 0 || super.getWidth() <= 0) {
            // in this case, our map has not been laid out yet, so we save the bounds in a local
            // variable, and make a guess of zoomLevel 10. Not to worry, though: as soon as layout
            // occurs, we will move the camera to the saved bounds. Note that if we tried to move
            // to the bounds now, it would trigger an exception.
            getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 10));
            boundsToMove = bounds;
        } else {
            getMap().moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0));
            boundsToMove = null;
        }
    }

    public void onDrop() {
        mClusterComputer.close();
        ((ReactContext) getContext()).removeLifecycleEventListener(this);
        destroyLocation();
        onDestroy();
    }

    public void updateExtraData(Object extraData) {
        // if boundsToMove is not null, we now have the MapView's width/height, so we can apply
        // a proper camera move
        if (boundsToMove != null) {
            HashMap<String, Float> data = (HashMap<String, Float>) extraData;
            float width = data.get("width");
            float height = data.get("height");
            getMap().moveCamera(
                    CameraUpdateFactory.newLatLngBounds(
                            boundsToMove,
                            (int) width,
                            (int) height,
                            0
                    )
            );
            boundsToMove = null;
        }
    }

    private AMapLocationClient getLocationClient() {
        if (mLocationClient == null) {
            mLocationClient = new AMapLocationClient(getContext());
            mLocationClient.setLocationListener(this);
            AMapLocationClientOption locationOption = new AMapLocationClientOption();
            locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            mLocationClient.setLocationOption(locationOption);
        }
        return mLocationClient;
    }

    private void startLocation() {
        getLocationClient().startLocation();
    }

    private void stopLocation() {
        if (mLocationClient != null && mLocationClient.isStarted()) {
            mLocationClient.stopLocation();
        }
    }

    private void destroyLocation() {
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
            mLocationClient = null;
        }
    }

    /*LifecycleEventListener*/

    @Override
    public void onHostResume() {
        onResume();
        AMap map = getMap();
        if (map != null && map.isMyLocationEnabled()) {
            startLocation();
        }
    }

    @Override
    public void onHostPause() {
        onPause();
        stopLocation();
    }

    @Override
    public void onHostDestroy() {
        onDrop();
    }

    public LatLngBounds getMarkerBounds() {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : mMarkerMap.keySet()) {
            builder.include(marker.getPosition());
        }
        return builder.build();
    }

    public AMapLocation getMyLastKnownLocation() {
        return getLocationClient().getLastKnownLocation();
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
        startLocation();
    }

    @Override
    public void deactivate() {
        stopLocation();
    }

    /* AMapLocationListener */

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (mLocationChangeListener != null && aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                mLocationChangeListener.onLocationChanged(aMapLocation);

                WritableMap event = Arguments.createMap();
                WritableMap location = Arguments.createMap();
                location.putDouble("latitude", aMapLocation.getLatitude());
                location.putDouble("longitude", aMapLocation.getLongitude());
                location.putDouble("altitude", aMapLocation.getAltitude());
                location.putDouble("accuracy", aMapLocation.getAccuracy());
                location.putDouble("heading", aMapLocation.getBearing());
                location.putDouble("speed", aMapLocation.getSpeed());
                location.putDouble("bearing", aMapLocation.getBearing());
                location.putDouble("timestamp", aMapLocation.getTime());
                event.putMap("location", location);
                pushEvent("onLocationUpdate", event);
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
        if (mMoveOnMarkerPress) {
            return false;
        } else {
            marker.showInfoWindow();
            return true;
        }
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
        LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
        mClusterComputer.setScaleAndBounds(map.getScalePerPixel(), bounds);
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

        pushEvent("onMapReady", new WritableNativeMap());
    }
}
