package com.lgzhuo.rct.amap;

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
import com.autonavi.tbt.NaviStaticInfo;
import com.autonavi.tbt.TrafficFacilityInfo;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lgzhuo on 2017/3/16.
 */

class AMapService extends ReactContextBaseJavaModule implements AMapNaviListener {

    private static final String REACT_NAME = "AMapService";

    private AMapNavi mNavi;
    private Promise mNaviRoutePromise;

    AMapService(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return REACT_NAME;
    }

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
}
