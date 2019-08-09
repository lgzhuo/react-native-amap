package rct.amap;

import android.content.Context;
import android.location.Location;
import android.view.View;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.TextureMapView;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polyline;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import rct.amap.cluster.Cluster;
import rct.amap.cluster.ClusterComputer;
import rct.amap.cluster.ClusterPoint;
import rct.amap.cluster.OnClusterListener;

/**
 * Created by lgzhuo on 2017/3/8.
 * <p>
 * 2017/4/30 将继承由MapView 改为TextureMapView解决当有多个AMapView instance时第一个叠加在后一个上的问题
 * 见react-native-maps issues https://github.com/airbnb/react-native-maps/issues/453
 */

class ReactAMapView extends TextureMapView implements LifecycleEventListener, AMap.OnMarkerClickListener, AMap.InfoWindowAdapter, AMap.OnInfoWindowClickListener, AMap.OnPolylineClickListener, AMap.OnCameraChangeListener, OnClusterListener, AMap.OnMapLoadedListener, AMap.OnMyLocationChangeListener {

    private List<AMapFeature> mFeatureList = new ArrayList<>();
    private Map<Marker, AMapMarker> mMarkerMap = new WeakHashMap<>();
    private Map<Polyline, AMapPolyline> mPolylineMap = new WeakHashMap<>();
    private ClusterComputer mClusterComputer = new ClusterComputer();
    private boolean mMoveOnMarkerPress;
    private LatLngBounds boundsToMove;
    private MyLocationStyle mLocationStyle;

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
            map.setOnMyLocationChangeListener(this);
        }
    }

    public void setMyLocationEnabled(boolean enabled) {
        AMap map = getMap();
        if (map != null) {
            if (mLocationStyle == null) {
                mLocationStyle = new MyLocationStyle();
                mLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
            }
            map.setMyLocationStyle(mLocationStyle);
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

    /*LifecycleEventListener*/

    @Override
    public void onHostResume() {
        onResume();
    }

    @Override
    public void onHostPause() {
        onPause();
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

    public Location getMyLastKnownLocation() {
        AMap map = getMap();
        return map == null ? null : map.getMyLocation();
    }

    public LatLng getMyLastKnownLatLng() {
        Location location = getMyLastKnownLocation();
        return location == null ? null : new LatLng(location.getLatitude(), location.getLongitude());
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

    /* AMap.OnMyLocationChangeListener */

    @Override
    public void onMyLocationChange(Location loc) {
        WritableMap event = Arguments.createMap();
        WritableMap location = Arguments.createMap();
        location.putDouble("latitude", loc.getLatitude());
        location.putDouble("longitude", loc.getLongitude());
        location.putDouble("altitude", loc.getAltitude());
        location.putDouble("accuracy", loc.getAccuracy());
        location.putDouble("heading", loc.getBearing());
        location.putDouble("speed", loc.getSpeed());
        location.putDouble("bearing", loc.getBearing());
        location.putDouble("timestamp", loc.getTime());
        event.putMap("location", location);
        pushEvent("onLocationUpdate", event);
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
