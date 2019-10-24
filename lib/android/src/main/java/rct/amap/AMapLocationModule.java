package rct.amap;

import android.os.Handler;
import android.text.TextUtils;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.SystemClock;
import com.facebook.react.modules.core.DeviceEventManagerModule;

/**
 * Created by lgzhuo on 2017/8/7.
 */

public class AMapLocationModule extends ReactContextBaseJavaModule {

    private final AMapLocationListener mLocationListener = new AMapLocationListener() {

        @Override
        public void onLocationChanged(AMapLocation aMapLocation) {
            if (aMapLocation.getErrorCode() == AMapLocation.LOCATION_SUCCESS) {
                getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit("AMapLocationDidChange", locationToMap(aMapLocation, mWatchedOption.needAddress));
            } else {
                emitError(buildPositionError(aMapLocation.getErrorCode(), aMapLocation.getErrorInfo()));
            }
        }
    };
    private LocationOptions mWatchedOption;

    public AMapLocationModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "AMapLocationObserver";
    }

    @ReactMethod
    public void getCurrentPosition(ReadableMap options,
                                   final Callback success,
                                   Callback error) {
        LocationOptions locationOptions = LocationOptions.fromReactMap(options);
        AMapLocationManager locationManager = AMapLocationManager.getInstance(getReactApplicationContext());
        AMapLocation location = locationManager.getLastKnownLocation();
        if (location != null && location.getErrorCode() == AMapLocation.LOCATION_SUCCESS && SystemClock.currentTimeMillis() - location.getTime() < locationOptions.maximumAge && (!locationOptions.needAddress || !TextUtils.isEmpty(location.getAddress()))) {
            success.invoke(locationToMap(location, locationOptions.needAddress));
            return;
        }
        new SingleUpdateRequest(locationManager, locationOptions, success, error).invoke();
    }

    @ReactMethod
    public void startObserving(ReadableMap options) {
        LocationOptions locationOptions = LocationOptions.fromReactMap(options);
        if (locationOptions.equivalent(mWatchedOption)) {
            return;
        }
        AMapLocationManager.getInstance(getReactApplicationContext()).addLocationUpdate(mLocationListener, locationOptions.toAMapLocationOption());
        mWatchedOption = locationOptions;
    }

    @ReactMethod
    public void stopObserving() {
        AMapLocationManager.getInstance(getReactApplicationContext()).removeLocationUpdate(mLocationListener);
        mWatchedOption = null;
    }

    private void emitError(WritableMap error) {
        getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("AMapLocationError", error);
    }

    private static WritableMap locationToMap(AMapLocation location, boolean widthAddress) {
        WritableMap map = Arguments.createMap();
        WritableMap coords = Arguments.createMap();
        coords.putDouble("latitude", location.getLatitude());
        coords.putDouble("longitude", location.getLongitude());
        coords.putDouble("altitude", location.getAltitude());
        coords.putDouble("accuracy", location.getAccuracy());
        coords.putDouble("heading", location.getBearing());
        coords.putDouble("speed", location.getSpeed());
        coords.putDouble("bearing", location.getBearing());
        map.putMap("coords", coords);
        map.putDouble("timestamp", location.getTime());

        if (android.os.Build.VERSION.SDK_INT >= 18) {
            map.putBoolean("mocked", location.isFromMockProvider());
        }
        if (widthAddress) {
            WritableMap regeocode = Arguments.createMap();
            regeocode.putString("adCode", location.getAdCode());
            regeocode.putString("address", location.getAddress());
            regeocode.putString("aoiName", location.getAoiName());
            regeocode.putString("buildingId", location.getBuildingId());
            regeocode.putString("city", location.getCity());
            regeocode.putString("cityCode", location.getCityCode());
            regeocode.putString("country", location.getCountry());
            regeocode.putString("district", location.getDistrict());
            regeocode.putString("floor", location.getFloor());
            regeocode.putInt("gpsAccuracyStatus", location.getGpsAccuracyStatus());
            regeocode.putString("locationDetail", location.getLocationDetail());
            regeocode.putInt("locationType", location.getLocationType());
            regeocode.putString("poiName", location.getPoiName());
            regeocode.putString("provider", location.getProvider());
            regeocode.putString("province", location.getProvince());
            regeocode.putString("street", location.getStreet());
            regeocode.putString("streetNum", location.getStreetNum());
            regeocode.putString("poiName", location.getPoiName());

            map.putMap("regeocode", regeocode);
        }

        return map;
    }

    private static WritableMap buildPositionError(int errorCode, String errorMessage) {
        WritableMap error = Arguments.createMap();
        error.putInt("code", errorCode);
        if (errorMessage != null) {
            error.putString("message", errorMessage);
        }
        return error;
    }

    private static class LocationOptions {
        private final long timeout;
        private final double maximumAge;
        private final boolean highAccuracy;
        private final float distanceFilter;
        private final int interval;
        private final boolean needAddress;

        LocationOptions(long timeout, double maximumAge, boolean highAccuracy, float distanceFilter, int interval, boolean needAddress) {
            this.timeout = timeout;
            this.maximumAge = maximumAge;
            this.highAccuracy = highAccuracy;
            this.distanceFilter = distanceFilter;
            this.interval = interval;
            this.needAddress = needAddress;
        }

        AMapLocationClientOption toAMapLocationOption() {
            AMapLocationClientOption option = new AMapLocationClientOption();
            option.setLocationMode(this.highAccuracy ? AMapLocationClientOption.AMapLocationMode.Battery_Saving : AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            option.setInterval(interval);
            option.setNeedAddress(needAddress);
            return option;
        }

        boolean equivalent(LocationOptions options) {
            return options != null && options.interval == this.interval && options.highAccuracy == this.highAccuracy && options.needAddress == this.needAddress;
        }

        private static LocationOptions fromReactMap(ReadableMap map) {
            // precision might be dropped on timeout (double -> int conversion), but that's OK
            long timeout =
                    map.hasKey("timeout") ? (long) map.getDouble("timeout") : Long.MAX_VALUE;
            double maximumAge =
                    map.hasKey("maximumAge") ? map.getDouble("maximumAge") : Double.POSITIVE_INFINITY;
            boolean highAccuracy =
                    map.hasKey("enableHighAccuracy") && map.getBoolean("enableHighAccuracy");
            float distanceFilter = map.hasKey("distanceFilter") ?
                    (float) map.getDouble("distanceFilter") :
                    100;
            int interval = map.hasKey("interval") ? map.getInt("interval") : 2000;
            boolean needAddress = map.hasKey("needAddress") && map.getBoolean("needAddress");

            return new LocationOptions(timeout, maximumAge, highAccuracy, distanceFilter, interval, needAddress);
        }
    }

    private static class SingleUpdateRequest {

        private final Callback mSuccess;
        private final Callback mError;
        private final AMapLocationManager mLocationManager;
        private final LocationOptions mOptions;
        private final Handler mHandler = new Handler();
        private final Runnable mTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                synchronized (SingleUpdateRequest.this) {
                    if (!mTriggered) {
                        mError.invoke(buildPositionError(3, "Location request timed out"));
                        mLocationManager.removeLocationUpdate(mLocationListener);
                        mTriggered = true;
                    }
                }
            }
        };
        private final AMapLocationListener mLocationListener = new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation aMapLocation) {
                synchronized (SingleUpdateRequest.this) {
                    if (!mTriggered) {
                        if (aMapLocation.getErrorCode() == AMapLocation.LOCATION_SUCCESS) {
                            mSuccess.invoke(locationToMap(aMapLocation, mOptions.needAddress));
                        } else {
                            mError.invoke(buildPositionError(aMapLocation.getErrorCode(), aMapLocation.getErrorInfo()));
                        }
                        mHandler.removeCallbacks(mTimeoutRunnable);
                        mTriggered = true;
                    }
                }
            }
        };
        private boolean mTriggered;

        private SingleUpdateRequest(
                AMapLocationManager locationManager,
                LocationOptions options,
                Callback success,
                Callback error) {
            mLocationManager = locationManager;
            mOptions = options;
            mSuccess = success;
            mError = error;
        }

        void invoke() {
            mLocationManager.requestOnceLocation(mLocationListener, mOptions.toAMapLocationOption());
            mHandler.postDelayed(mTimeoutRunnable, mOptions.timeout);
        }
    }
}
