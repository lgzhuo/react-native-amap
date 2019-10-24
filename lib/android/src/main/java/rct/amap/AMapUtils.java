package rct.amap;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.TextureView;

import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.navi.model.AMapNaviLink;
import com.amap.api.navi.model.AMapNaviPath;
import com.amap.api.navi.model.AMapNaviStep;
import com.amap.api.navi.model.NaviLatLng;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.poisearch.PoiResult;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.views.art.ARTSurfaceViewShadowNode;
import com.facebook.react.views.art.ARTVirtualNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import rct.amap.cluster.Cluster;
import rct.amap.cluster.ClusterPoint;

/**
 * Created by lgzhuo on 2017/3/16.
 */

class AMapUtils {

    static final Convert<NaviLatLng> NaviLatLngConvert = new SimpleConvert<NaviLatLng>() {
        @Override
        public WritableMap cnv(NaviLatLng latLng) {
            WritableMap latLngMap = Arguments.createMap();
            latLngMap.putDouble("latitude", latLng.getLatitude());
            latLngMap.putDouble("longitude", latLng.getLongitude());
            return latLngMap;
        }

        @Override
        public NaviLatLng cnv(ReadableMap map) {
            return new NaviLatLng(map.getDouble("latitude"), map.getDouble("longitude"));
        }
    };

    static final Convert<LatLng> LatLngConvert = new SimpleConvert<LatLng>() {
        @Override
        public WritableMap cnv(LatLng latLng) {
            WritableMap latLngMap = Arguments.createMap();
            latLngMap.putDouble("latitude", latLng.latitude);
            latLngMap.putDouble("longitude", latLng.longitude);
            return latLngMap;
        }

        @Override
        public LatLng cnv(ReadableMap map) {
            return new LatLng(map.getDouble("latitude"), map.getDouble("longitude"));
        }
    };

    static final Convert<LatLngBounds> LatLngBoundsConvert = new SimpleConvert<LatLngBounds>() {
        @Override
        public WritableMap cnv(LatLngBounds latLngBounds) {
            WritableMap boundsMap = Arguments.createMap();
            boundsMap.putMap("northeast", LatLngConvert.cnv(latLngBounds.northeast));
            boundsMap.putMap("southwest", LatLngConvert.cnv(latLngBounds.southwest));
            return boundsMap;
        }
    };

    static final Convert<AMapNaviLink> AMapNaviLinkConvert = new SimpleConvert<AMapNaviLink>() {
        @Override
        public WritableMap cnv(AMapNaviLink link) {
            WritableMap linkMap = Arguments.createMap();
            linkMap.putArray("coordinates", NaviLatLngConvert.cnvArr(link.getCoords()));
            linkMap.putInt("length", link.getLength());
            linkMap.putInt("time", link.getTime());
            linkMap.putString("roadName", link.getRoadName());
            linkMap.putInt("roadClass", link.getRoadClass());
            linkMap.putInt("roadType", link.getRoadType());
            linkMap.putBoolean("hasTrafficLights", link.getTrafficLights());
            return linkMap;
        }
    };

    static final Convert<AMapNaviStep> AMapNaviStepConvert = new SimpleConvert<AMapNaviStep>() {
        @Override
        public WritableMap cnv(AMapNaviStep step) {
            WritableMap stepMap = Arguments.createMap();
            stepMap.putArray("coordinates", NaviLatLngConvert.cnvArr(step.getCoords()));
            stepMap.putInt("length", step.getLength());
            stepMap.putInt("time", step.getTime());
            stepMap.putInt("chargeLength", step.getChargeLength());
            stepMap.putInt("trafficLightNumber", step.getTrafficLightNumber());
            stepMap.putArray("links", AMapNaviLinkConvert.cnvArr(step.getLinks()));
            return stepMap;
        }
    };

    static final Convert<AMapNaviPath> AMapNaviPathConvert = new SimpleConvert<AMapNaviPath>() {
        @Override
        public WritableMap cnv(AMapNaviPath path) {
            WritableMap pathMap = Arguments.createMap();
            pathMap.putInt("length", path.getAllLength());
            pathMap.putInt("time", path.getAllTime());
            pathMap.putMap("bounds", LatLngBoundsConvert.cnv(path.getBoundsForPath()));
            pathMap.putMap("center", NaviLatLngConvert.cnv(path.getCenterForPath()));
            pathMap.putMap("start", NaviLatLngConvert.cnv(path.getStartPoint()));
            pathMap.putMap("end", NaviLatLngConvert.cnv(path.getEndPoint()));
            pathMap.putArray("coordinates", NaviLatLngConvert.cnvArr(path.getCoordList()));
            pathMap.putInt("strategy", path.getStrategy());
            pathMap.putInt("tollCost", path.getTollCost());
            pathMap.putArray("steps", AMapNaviStepConvert.cnvArr(path.getSteps()));
            pathMap.putInt("stepsCount", path.getStepsCount());
            pathMap.putArray("wayPoints", NaviLatLngConvert.cnvArr(path.getWayPoint()));
            pathMap.putArray("wayPointIndexes", intArray(path.getWayPointIndex()));
            return pathMap;
        }
    };

    static final Convert<ClusterPoint> ClusterPointConvert = new SimpleConvert<ClusterPoint>() {
        @Override
        public WritableMap cnv(ClusterPoint point) {
            WritableMap pointMap = Arguments.createMap();
            pointMap.putDouble("latitude", point.latitude);
            pointMap.putDouble("longitude", point.longitude);
            pointMap.putString("id", point.id);
            pointMap.putInt("idx", point.idx);
            return pointMap;
        }
    };

    static final Convert<Cluster> ClusterConvert = new SimpleConvert<Cluster>() {
        @Override
        public WritableMap cnv(Cluster cluster) {
            WritableMap clusterMap = Arguments.createMap();
            clusterMap.putMap("coordinate", LatLngConvert.cnv(cluster.getCoordinate()));
            clusterMap.putArray("points", ClusterPointConvert.cnvArr(cluster.getPoints()));
            return clusterMap;
        }
    };

    static final Convert<LatLonPoint> LatLonPointConvert = new SimpleConvert<LatLonPoint>() {
        @Override
        public WritableMap cnv(LatLonPoint latLonPoint) {
            if (latLonPoint == null) return null;
            WritableMap latLngMap = Arguments.createMap();
            latLngMap.putDouble("latitude", latLonPoint.getLatitude());
            latLngMap.putDouble("longitude", latLonPoint.getLongitude());
            return latLngMap;
        }
    };

    static final Convert<PoiItem> PoiItemConvert = new SimpleConvert<PoiItem>() {
        @Override
        public WritableMap cnv(PoiItem poiItem) {
            if (poiItem == null) return null;
            WritableMap itemMap = Arguments.createMap();
            itemMap.putString("adCode", poiItem.getAdCode());
            itemMap.putString("adName", poiItem.getAdName());
            itemMap.putString("businessAres", poiItem.getBusinessArea());
            itemMap.putString("cityCode", poiItem.getCityCode());
            itemMap.putString("cityName", poiItem.getCityName());
            itemMap.putString("direction", poiItem.getDirection());
            itemMap.putInt("distance", poiItem.getDistance());
            itemMap.putString("email", poiItem.getEmail());
            itemMap.putMap("enter", LatLonPointConvert.cnv(poiItem.getEnter()));
            itemMap.putMap("exit", LatLonPointConvert.cnv(poiItem.getExit()));
            itemMap.putMap("location", LatLonPointConvert.cnv(poiItem.getLatLonPoint()));
            itemMap.putString("title", poiItem.getTitle());
            itemMap.putString("address", poiItem.getSnippet());
            return itemMap;
        }
    };

    static final Convert<PoiResult> PoiResultConvert = new SimpleConvert<PoiResult>() {
        @Override
        public WritableMap cnv(PoiResult poiResult) {
            WritableMap resultMap = Arguments.createMap();
            resultMap.putArray("pois", PoiItemConvert.cnvArr(poiResult.getPois()));
            return resultMap;
        }
    };

    static WritableMap cnvAMapNaviPath(AMapNaviPath path, Integer id) {
        WritableMap pathMap = AMapNaviPathConvert.cnv(path);
        if (id != null)
            pathMap.putInt("id", id);
        return pathMap;
    }

    static WritableArray intArray(int[] arr) {
        if (arr == null) return null;
        WritableArray war = Arguments.createArray();
        for (int i : arr) {
            war.pushInt(i);
        }
        return war;
    }


    abstract static class SimpleConvert<T> implements Convert<T> {

        @Override
        public WritableMap cnv(T t) {
            throw new IllegalStateException("Convert method cnv not realize");
        }

        @Override
        public T cnv(ReadableMap map) {
            throw new IllegalStateException("Convert method cnv not realize");
        }

        @Override
        public List<T> cnvArr(ReadableArray array) {
            List<T> list = new ArrayList<>(array.size());
            for (int i = 0; i < array.size(); i++) {
                list.add(cnv(array.getMap(i)));
            }
            return list;
        }

        @Override
        public WritableArray cnvArr(Collection<T> collection) {
            if (collection == null) return null;
            WritableArray array = Arguments.createArray();
            for (T t : collection) {
                array.pushMap(cnv(t));
            }
            return array;
        }
    }

    static boolean drawSurfaceView(Canvas canvas, TextureView surfaceView) {
        TextureView.SurfaceTextureListener listener = surfaceView.getSurfaceTextureListener();
        if (listener != null && listener instanceof ARTSurfaceViewShadowNode) {
            ARTSurfaceViewShadowNode shadowNode = (ARTSurfaceViewShadowNode) listener;
            Paint paint = new Paint();
            for (int i = 0; i < shadowNode.getChildCount(); i++) {
                ARTVirtualNode virtualNode = (ARTVirtualNode) shadowNode.getChildAt(i);
                virtualNode.draw(canvas, paint, 1f);
            }
            return true;
        }
        return false;
    }

    interface Convert<T> {
        WritableMap cnv(T t);

        T cnv(ReadableMap map);

        WritableArray cnvArr(Collection<T> collection);

        List<T> cnvArr(ReadableArray array);
    }

    static boolean isAppInstalled(Context context, String packageName) {
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            return info != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
