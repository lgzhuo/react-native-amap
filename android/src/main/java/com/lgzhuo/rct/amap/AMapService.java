package com.lgzhuo.rct.amap;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.AMapNaviListener;
import com.amap.api.navi.model.AMapLaneInfo;
import com.amap.api.navi.model.AMapNaviCross;
import com.amap.api.navi.model.AMapNaviInfo;
import com.amap.api.navi.model.AMapNaviLocation;
import com.amap.api.navi.model.AMapNaviPath;
import com.amap.api.navi.model.AMapNaviStaticInfo;
import com.amap.api.navi.model.AMapNaviTrafficFacilityInfo;
import com.amap.api.navi.model.AimLessModeCongestionInfo;
import com.amap.api.navi.model.AimLessModeStat;
import com.amap.api.navi.model.NaviInfo;
import com.amap.api.navi.model.NaviLatLng;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.autonavi.tbt.NaviStaticInfo;
import com.autonavi.tbt.TrafficFacilityInfo;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.lgzhuo.rct.amap.helper.ReadableMapWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    /* calculateNaviDriveRoute */

    @ReactMethod
    public void calculateNaviDriveRoute(ReadableMap props, Promise promise) {
        if (mNaviRoutePromise != null) {
            promise.reject("-1", "上次导航线路规划未结束，不能同步调用");
            return;
        }
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
        boolean resolve;
        if (from != null) {
            resolve = getNavi().calculateDriveRoute(Collections.singletonList(from), Collections.singletonList(to), wayPoints, strategyFlag);
        } else {
            resolve = getNavi().calculateDriveRoute(Collections.singletonList(to), wayPoints, strategyFlag);
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

    /* AMapNaviListener */

    @Override
    public void onCalculateRouteSuccess() {
        if (mNaviRoutePromise != null) {
            AMapNaviPath path = mNavi.getNaviPath();
            WritableArray array = Arguments.createArray();
            array.pushMap(AMapUtils.cnvAMapNaviPath(path, -1));
            mNaviRoutePromise.resolve(array);
            mNaviRoutePromise = null;
        }
    }

    @Override
    public void onCalculateMultipleRoutesSuccess(int[] ints) {
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
    public void onArriveDestination(NaviStaticInfo naviStaticInfo) {

    }

    @Override
    public void onArriveDestination(AMapNaviStaticInfo aMapNaviStaticInfo) {

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
