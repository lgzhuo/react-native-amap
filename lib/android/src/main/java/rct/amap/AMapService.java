package rct.amap;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.AMapNaviListener;
import com.amap.api.navi.model.AMapLaneInfo;
import com.amap.api.navi.model.AMapModelCross;
import com.amap.api.navi.model.AMapNaviCameraInfo;
import com.amap.api.navi.model.AMapNaviCross;
import com.amap.api.navi.model.AMapNaviInfo;
import com.amap.api.navi.model.AMapNaviLocation;
import com.amap.api.navi.model.AMapNaviPath;
import com.amap.api.navi.model.AMapNaviTrafficFacilityInfo;
import com.amap.api.navi.model.AMapServiceAreaInfo;
import com.amap.api.navi.model.AimLessModeCongestionInfo;
import com.amap.api.navi.model.AimLessModeStat;
import com.amap.api.navi.model.NaviInfo;
import com.amap.api.navi.model.NaviLatLng;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.autonavi.tbt.TrafficFacilityInfo;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rct.amap.helper.ReadableMapWrapper;

/**
 * Created by lgzhuo on 2017/3/16.
 */

class AMapService extends ReactContextBaseJavaModule implements AMapNaviListener, AMapLocationListener {

    private static final String REACT_NAME = "AMapService";

    private AMapNavi mNavi;
    private Promise mNaviRoutePromise;

    private AMapLocationClient mLocationClient;
    private Promise mLocationPromise;
    private ExecutorService executorService = Executors.newFixedThreadPool(3);

    AMapService(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return REACT_NAME;
    }

    @Override
    public void onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy();
        if (this.mNavi != null) {
            this.mNavi.removeAMapNaviListener(this);
            this.mNavi.destroy();
            this.mNavi = null;
        }
    }

    /* calculateNaviDriveRoute */

    @ReactMethod
    public void calculateNaviDriveRoute(ReadableMap props, Promise promise) {
        if (props == null || !props.hasKey("to")) {
            promise.reject("-2", "参数不完整");
            return;
        }
        boolean avoidCongestion = props.hasKey("avoidCongestion") && props.getBoolean("avoidCongestion"),
                avoidHighway = props.hasKey("avoidHighway") && props.getBoolean("avoidHighway"),
                avoidCost = props.hasKey("avoidCost") && props.getBoolean("avoidCost"),
                prioritiseHighway = props.hasKey("prioritiseHighway") && props.getBoolean("prioritiseHighway"),
                multipleRoute = props.hasKey("multipleRoute") && props.getBoolean("multipleRoute");
        if (avoidHighway && prioritiseHighway) {
            promise.reject("-3", "不走高速与高速优先不能同时为true.");
            return;
        }
        if (avoidCost && prioritiseHighway) {
            promise.reject("-4", "高速优先与避免收费不能同时为true.");
            return;
        }

        NaviLatLng to = AMapUtils.NaviLatLngConvert.cnv(props.getMap("to"));
        NaviLatLng from = null;
        if (props.hasKey("from")) {
            from = AMapUtils.NaviLatLngConvert.cnv(props.getMap("from"));
        }
        List<NaviLatLng> wayPoints;
        if (props.hasKey("wayPoints")) {
            wayPoints = AMapUtils.NaviLatLngConvert.cnvArr(props.getArray("wayPoints"));
        } else {
            wayPoints = new ArrayList<>();
        }
        int strategyFlag = 0;
        try {
            strategyFlag = getNavi().strategyConvert(avoidCongestion, avoidHighway, avoidCost, prioritiseHighway, multipleRoute);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //取消前一个路径规划
        if (mNaviRoutePromise != null) {
            mNaviRoutePromise.reject("-1", "有新的导航规划请求，当前路径规划已取消");
            mNaviRoutePromise = null;
        }
        boolean resolve;
        if (from != null) {
            resolve = getNavi().calculateDriveRoute(Collections.singletonList(from), Collections.singletonList(to), wayPoints, strategyFlag);
        } else {
            getNavi().startGPS();
            resolve = getNavi().calculateDriveRoute(Collections.singletonList(to), wayPoints, strategyFlag);
        }
        if (resolve) {
            mNaviRoutePromise = promise;
        } else {
            promise.reject("-5", "导航线路规划失败");
        }
    }

    @ReactMethod
    public void calculateNaviWalkRoute(ReadableMap props, Promise promise) {
        if (props == null || !props.hasKey("to")) {
            promise.reject("-2", "参数不完整");
            return;
        }
        NaviLatLng to = AMapUtils.NaviLatLngConvert.cnv(props.getMap("to"));
        NaviLatLng from = null;
        if (props.hasKey("from")) {
            from = AMapUtils.NaviLatLngConvert.cnv(props.getMap("from"));
        }
        //取消前一个路径规划
        if (mNaviRoutePromise != null) {
            mNaviRoutePromise.reject("-1", "有新的导航规划请求，当前路径规划已取消");
            mNaviRoutePromise = null;
        }
        boolean resolve;
        if (from != null) {
            resolve = getNavi().calculateWalkRoute(from, to);
        } else {
            resolve = getNavi().calculateWalkRoute(to);
        }
        if (resolve) {
            mNaviRoutePromise = promise;
        } else {
            promise.reject("-5", "导航线路规划失败");
        }
    }

    private AMapNavi getNavi() {
        if (mNavi == null) {
            synchronized (this) {
                if (mNavi == null) {
                    mNavi = AMapNavi.getInstance(getReactApplicationContext());
                    mNavi.addAMapNaviListener(this);
                }
            }
        }
        return mNavi;
    }

    @ReactMethod
    public void startNavi(ReadableMap props) {
        Intent gpsintent = new Intent(getReactApplicationContext(), RouteNaviActivity.class);
        gpsintent.putExtra("gps", true);
        if (props.hasKey("id"))
            this.getNavi().selectRouteId(props.getInt("id"));
        gpsintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getReactApplicationContext().startActivity(gpsintent);
    }

    /* getCurrentLocation */

    @ReactMethod
    public void getCurrentPosition(ReadableMap props, final Promise promise) {
        if (mLocationPromise != null) {
            promise.reject("-1", "上次定位未结束");
        } else {
            mLocationPromise = promise;
            props = ReadableMapWrapper.wrap(props);
            AMapLocationClientOption option = new AMapLocationClientOption();
            option.setOnceLocationLatest(true);
            option.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            option.setMockEnable(true);//
            option.setNeedAddress(true);
            AMapLocationClient client = getLocationClient();
            client.setLocationOption(option);
            client.startLocation();
        }
    }

    private AMapLocationClient getLocationClient() {
        if (mLocationClient == null) {
            synchronized (this) {
                if (mLocationClient == null) {
                    mLocationClient = new AMapLocationClient(getReactApplicationContext());
                    mLocationClient.setLocationListener(this);
                }
            }
        }
        return mLocationClient;
    }

    /* poiSearch */
    @ReactMethod
    public void poiSearch(ReadableMap props, final Promise promise) {
        props = ReadableMapWrapper.wrap(props);
        String keyWord = props.getString("keyWord");
        String city = props.getString("city");
        int pageSize = props.getInt("pageSize");
        int pageNum = props.getInt("pageNum");
        boolean cityLimit = props.getBoolean("cityLimit");
        PoiSearch.Query query = new PoiSearch.Query(keyWord, "", city);
        query.setPageSize(pageSize);
        query.setPageNum(pageNum);
        query.setCityLimit(cityLimit);
        final PoiSearch search = new PoiSearch(getReactApplicationContext(), query);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    PoiResult result = search.searchPOI();
                    promise.resolve(AMapUtils.PoiResultConvert.cnv(result));
                } catch (AMapException e) {
                    e.printStackTrace();
                    promise.reject(e);
                }
            }
        });
    }


    private static String[] amapRequiredParameters = new String[]{"sourceApplication", "dlat", "dlon", "dev", "t"};
    private static String[] amapOptionalParameters = new String[]{"sid", "slat", "slon", "sname", "did", "dname"};

    /**
     * 调起高德地图进行路径规划，未安装高德app时唤起H5页面
     *
     * @param props 参数同http://lbs.amap.com/api/amap-mobile/guide/android/route
     */
    @ReactMethod
    public void callAMapRoute(ReadableMap props) {
        if (AMapUtils.isAppInstalled(getReactApplicationContext(), "com.autonavi.minimap")) {
            Uri.Builder dataBuilder = new Uri.Builder().scheme("amapuri").authority("route").appendPath("plan");
            dataBuilder.appendQueryParameter("sourceApplication", props.getString("sourceApplication"));
            for (String parameter : amapRequiredParameters) {
                if (!props.hasKey(parameter)) {
                    Log.w("AMapService", "调起高德地图必须参数" + parameter + "未设置");
                    return;
                }
                dataBuilder.appendQueryParameter(parameter, props.getString(parameter));
            }
            for (String parameter : amapOptionalParameters) {
                if (!props.hasKey(parameter)) {
                    continue;
                }
                dataBuilder.appendQueryParameter(parameter, props.getString(parameter));
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(dataBuilder.build());
            intent.setPackage("com.autonavi.minimap");
            getReactApplicationContext().startActivity(intent);
        } else {
            Uri.Builder urlBuilder = new Uri.Builder().scheme("http").authority("uri.amap.com").path("navigation");
            for (String parameter : amapRequiredParameters) {
                if (!props.hasKey(parameter)) {
                    Log.w("AMapService", "调起高德地图必须参数" + parameter + "未设置");
                    return;
                }
            }
            if (props.hasKey("slat") && props.hasKey("slon")) {
                StringBuilder from = new StringBuilder(props.getString(props.getString("slon"))).append(",").append(props.getString("slat"));
                if (props.hasKey("sname")) {
                    from.append(",").append(props.getString("sname"));
                }
                urlBuilder.appendQueryParameter("from", from.toString());
            }
            StringBuilder to = new StringBuilder(props.getString(props.getString("dlon"))).append(",").append(props.getString("dlat"));
            if (props.hasKey("dname")) {
                to.append(",").append(props.getString("dname"));
            }
            urlBuilder.appendQueryParameter("to", to.toString());
            int dev = props.getInt("dev");
            urlBuilder.appendQueryParameter("coordinate", dev == 0 ? "gaode" : "wgs84");
            int t = props.getInt("t");
            String mode = null;
            switch (t) {
                case 0:
                    mode = "car";
                    break;
                case 1:
                    mode = "bus";
                    break;
                case 2:
                    mode = "walk";
                    break;
                case 3:
                    mode = "ride";
                    break;
            }
            if (mode != null) {
                urlBuilder.appendQueryParameter("mode", mode);
            }
            urlBuilder.appendQueryParameter("src", props.getString("sourceApplication"));
            try {
                getReactApplicationContext().startActivity(Intent.parseUri(urlBuilder.toString(), 0));
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    private static String[] baiduMapOptionalParameters = new String[]{"destination", "origin", "mode", "region", "origin_region", "destination_region", "sy", "index", "target", "coord_type", "zoom", "src"};

    /**
     * 调起百度地图进行路径规划，未安装百度app时唤起H5页面
     *
     * @param props 同http://lbsyun.baidu.com/index.php?title=uri/api/android 和 http://lbsyun.baidu.com/index.php?title=uri/api/web
     */
    @ReactMethod
    public void callBaiduMapRoute(ReadableMap props) {
        Uri.Builder uriBuilder;
        if (AMapUtils.isAppInstalled(getReactApplicationContext(), "com.baidu.BaiduMap")) {
            uriBuilder = new Uri.Builder().scheme("baidumap").authority("map").path("direction");
        } else {
            uriBuilder = new Uri.Builder().scheme("http").authority("api.map.baidu.com").path("direction").appendQueryParameter("mode", "driving").appendQueryParameter("output", "html");
        }
        if (props.hasKey("destination") && props.hasKey("origin")) {
            Log.w("AMapService", "调起百度地图route必须设置起点或终点");
            return;
        }
        for (String parameter : baiduMapOptionalParameters) {
            if (!props.hasKey(parameter)) {
                continue;
            }
            uriBuilder.appendQueryParameter(parameter, props.getString(parameter));
        }
        Intent intent = new Intent();
        intent.setData(uriBuilder.build());
        getReactApplicationContext().startActivity(intent);
    }

    /* AMapNaviListener */

    @Override
    public void onCalculateRouteSuccess(int[] ints) {
        if (mNaviRoutePromise != null) {
            HashMap<Integer, AMapNaviPath> paths = mNavi.getNaviPaths();
            WritableArray array = Arguments.createArray();
            for (Map.Entry<Integer, AMapNaviPath> entry : paths.entrySet()) {
                array.pushMap(AMapUtils.cnvAMapNaviPath(entry.getValue(), entry.getKey()));
            }
            mNaviRoutePromise.resolve(array);
            mNaviRoutePromise = null;
        }
    }

    @Override
    public void onCalculateRouteFailure(int i) {
        if (mNaviRoutePromise != null) {
            mNaviRoutePromise.reject("" + i, "导航线路规划失败");
            mNaviRoutePromise = null;
        }
    }

    @Override
    public void onInitNaviFailure() {

    }

    @Override
    public void onInitNaviSuccess() {

    }

    @Override
    public void onStartNavi(int i) {

    }

    @Override
    public void onTrafficStatusUpdate() {

    }

    @Override
    public void onLocationChange(AMapNaviLocation aMapNaviLocation) {

    }

    @Override
    public void onGetNavigationText(int i, String s) {

    }

    @Override
    public void onEndEmulatorNavi() {

    }

    @Override
    public void onArriveDestination() {

    }

    @Override
    public void onReCalculateRouteForYaw() {

    }

    @Override
    public void onReCalculateRouteForTrafficJam() {

    }

    @Override
    public void onArrivedWayPoint(int i) {

    }

    @Override
    public void onGpsOpenStatus(boolean b) {

    }

    @Override
    public void onNaviInfoUpdated(AMapNaviInfo aMapNaviInfo) {

    }

    @Override
    public void onNaviInfoUpdate(NaviInfo naviInfo) {

    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo aMapNaviTrafficFacilityInfo) {

    }

    @Override
    public void OnUpdateTrafficFacility(TrafficFacilityInfo trafficFacilityInfo) {

    }

    @Override
    public void showCross(AMapNaviCross aMapNaviCross) {

    }

    @Override
    public void hideCross() {

    }

    @Override
    public void showLaneInfo(AMapLaneInfo[] aMapLaneInfos, byte[] bytes, byte[] bytes1) {

    }

    @Override
    public void hideLaneInfo() {

    }

    @Override
    public void notifyParallelRoad(int i) {

    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo[] aMapNaviTrafficFacilityInfos) {

    }

    @Override
    public void updateAimlessModeStatistics(AimLessModeStat aimLessModeStat) {

    }

    @Override
    public void updateAimlessModeCongestionInfo(AimLessModeCongestionInfo aimLessModeCongestionInfo) {

    }

    @Override
    public void onGetNavigationText(String s) {

    }

    @Override
    public void updateCameraInfo(AMapNaviCameraInfo[] aMapNaviCameraInfos) {

    }

    @Override
    public void onServiceAreaUpdate(AMapServiceAreaInfo[] aMapServiceAreaInfos) {

    }

    @Override
    public void showModeCross(AMapModelCross aMapModelCross) {

    }

    @Override
    public void hideModeCross() {

    }

    @Override
    public void onPlayRing(int i) {

    }

    /* AMapLocationListener */

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (mLocationPromise != null) {
            if (aMapLocation.getErrorCode() != AMapLocation.LOCATION_SUCCESS) {
                mLocationPromise.reject("" + aMapLocation.getErrorCode(), aMapLocation.getErrorInfo());
            } else {
                WritableMap result = Arguments.createMap();
                result.putDouble("accuracy", aMapLocation.getAccuracy());
                result.putString("adCode", aMapLocation.getAdCode());
                result.putString("address", aMapLocation.getAddress());
                result.putDouble("altitude", aMapLocation.getAltitude());
                result.putString("aoiName", aMapLocation.getAoiName());
                result.putDouble("bearing", aMapLocation.getBearing());
                result.putString("buildingId", aMapLocation.getBuildingId());
                result.putString("city", aMapLocation.getCity());
                result.putString("cityCode", aMapLocation.getCityCode());
                result.putString("country", aMapLocation.getCountry());
                result.putString("district", aMapLocation.getDistrict());
                result.putString("floor", aMapLocation.getFloor());
                result.putDouble("longitude", aMapLocation.getLongitude());
                result.putDouble("latitude", aMapLocation.getLatitude());
                result.putInt("gpsAccuracyStatus", aMapLocation.getGpsAccuracyStatus());
                result.putString("locationDetail", aMapLocation.getLocationDetail());
                result.putInt("locationType", aMapLocation.getLocationType());
                result.putString("poiName", aMapLocation.getPoiName());
                result.putString("provider", aMapLocation.getProvider());
                result.putString("province", aMapLocation.getProvince());
                result.putDouble("speed", aMapLocation.getSpeed());
                result.putString("street", aMapLocation.getStreet());
                result.putString("streetNum", aMapLocation.getStreetNum());
                mLocationPromise.resolve(result);
            }
            mLocationPromise = null;
        }
    }

}
