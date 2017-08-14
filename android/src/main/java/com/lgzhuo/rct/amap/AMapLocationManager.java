package com.lgzhuo.rct.amap;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lgzhuo on 2017/8/8.
 */

public class AMapLocationManager implements AMapLocationListener {

    private static volatile AMapLocationManager sInstance;
    private static AMapLocationManagerListener sListener;

    public static AMapLocationManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (AMapLocationManager.class) {
                if (sInstance == null) {
                    sInstance = new AMapLocationManager(context);
                }
            }
        }
        return sInstance;
    }

    public static void destroy() {
        if (sInstance != null) {
            synchronized (AMapLocationManager.class) {
                if (sInstance != null) {
                    sInstance.onDestroy();
                    sInstance = null;
                }
            }
        }
    }

    public static void setManagerListener(AMapLocationManagerListener listener) {
        sListener = listener;
    }

    private AMapLocationClient mClient;
    private Map<AMapLocationListener, AMapLocationClientOption> mListenerMap;
    private Map<AMapLocationListener, AMapLocationClientOption> mOnceRequestListenerMap;
    private HandlerThread mWorkThread;
    private Handler mHandler;
    private Handler mListenerHandler;

    private AMapLocationManager(Context context) {
        mClient = new AMapLocationClient(context);
        mClient.setLocationListener(this);
        mListenerMap = new HashMap<>();
        mOnceRequestListenerMap = new HashMap<>();

        mWorkThread = new HandlerThread("AMapLocationManager work thread");
        mWorkThread.start();
        mHandler = new WorkHandler(mWorkThread.getLooper());
        mListenerHandler = new ListenerHandler(Looper.getMainLooper());
    }

    /* public api */

    public void requestOnceLocation(AMapLocationListener listener, AMapLocationClientOption option) {
        mHandler.obtainMessage(MSG_ADD_ONCE_UPDATE, ListenerPair.create(listener, option)).sendToTarget();
    }

    public void addLocationUpdate(@NonNull AMapLocationListener listener, AMapLocationClientOption option) {
        mHandler.obtainMessage(MSG_ADD_UPDATE, ListenerPair.create(listener, option)).sendToTarget();
    }

    public void removeLocationUpdate(@NonNull AMapLocationListener listener) {
        mHandler.obtainMessage(MSG_REMOVE_UPDATE, listener).sendToTarget();
    }

    public AMapLocation getLastKnownLocation() {
        return mClient.getLastKnownLocation();
    }

    /* private method */

    private void onDestroy() {
        if (mClient.isStarted()) {
            mClient.stopLocation();
            mHandler.removeCallbacksAndMessages(null);
            mWorkThread.quit();
        }
        mClient.onDestroy();
    }

    private void onListenerChange() {
        if (mListenerMap.isEmpty() && mOnceRequestListenerMap.isEmpty()) {
            mClient.stopLocation();
            mListenerHandler.obtainMessage(MSG_WATCH_STOP).sendToTarget();
        } else {
            boolean onStart = true;
            if (mClient.isStarted()) {
                mClient.stopLocation();
                onStart = false;
            }
            List<AMapLocationClientOption> options = new ArrayList<>(mListenerMap.size() + mOnceRequestListenerMap.size());
            options.addAll(mListenerMap.values());
            options.addAll(mOnceRequestListenerMap.values());
            AMapLocationClientOption option = mergeOptions(options);
            mClient.setLocationOption(option);
            mClient.startLocation();
            mListenerHandler.obtainMessage(onStart ? MSG_WATCH_START : MSG_WATCH_CHANGE, option).sendToTarget();
        }
    }

    private static AMapLocationClientOption mergeOptions(Collection<AMapLocationClientOption> options) {
        AMapLocationClientOption option = null;
        for (AMapLocationClientOption _option : options) {
            if (_option == null) {
                continue;
            }
            if (option == null) {
                option = _option.clone();
            } else {
                option.setHttpTimeOut(Math.min(option.getHttpTimeOut(), _option.getHttpTimeOut()));
                option.setInterval(Math.min(option.getInterval(), _option.getInterval()));
                AMapLocationClientOption.AMapLocationMode mode0 = option.getLocationMode();
                AMapLocationClientOption.AMapLocationMode mode1 = _option.getLocationMode();
                option.setLocationMode(mode0 == null || mode0 == mode1 ? mode1 : AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
                option.setMockEnable(false);
                option.setNeedAddress(_option.isNeedAddress() || option.isNeedAddress());
                option.setOnceLocation(false);
                option.setWifiScan(_option.isWifiScan() || option.isWifiScan());
                option.setLocationCacheEnable(option.isLocationCacheEnable() || _option.isLocationCacheEnable());
                option.setSensorEnable(option.isSensorEnable() || _option.isSensorEnable());
            }
        }
        return option;
    }

    /* handle */

    private void handleAddUpdate(AMapLocationListener listener, AMapLocationClientOption option) {
        mListenerMap.put(listener, option);
        onListenerChange();
    }

    private void handleAddOnceUpdate(AMapLocationListener listener, AMapLocationClientOption option) {
        mOnceRequestListenerMap.put(listener, option);
        onListenerChange();
    }

    private void handleRemoveUpdate(AMapLocationListener listener) {
        boolean changed = false;
        if (mListenerMap.containsKey(listener)) {
            mListenerMap.remove(listener);
            changed = true;
        }
        if (mOnceRequestListenerMap.containsKey(listener)) {
            mOnceRequestListenerMap.remove(listener);
            changed = true;
        }
        if (changed) {
            onListenerChange();
        }
    }

    private void handleLocationChange(AMapLocation location) {
        for (AMapLocationListener listener : mListenerMap.keySet()) {
            listener.onLocationChanged(location);
        }
        if (!mOnceRequestListenerMap.isEmpty()) {
            for (AMapLocationListener listener : mOnceRequestListenerMap.keySet()) {
                listener.onLocationChanged(location);
            }
            mOnceRequestListenerMap.clear();
            onListenerChange();
        }
    }

    /* AMapLocationListener */

    @Override
    public void onLocationChanged(AMapLocation location) {
        mHandler.obtainMessage(MSG_LOCATION_CHANGE, location).sendToTarget();
    }

    private static final int MSG_ADD_UPDATE = 0;
    private static final int MSG_REMOVE_UPDATE = 1;
    private static final int MSG_ADD_ONCE_UPDATE = 2;
    private static final int MSG_LOCATION_CHANGE = 3;

    private class WorkHandler extends Handler {

        WorkHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ADD_UPDATE: {
                    ListenerPair pair = (ListenerPair) msg.obj;
                    handleAddUpdate(pair.first, pair.second);
                }
                break;
                case MSG_ADD_ONCE_UPDATE: {
                    ListenerPair pair = (ListenerPair) msg.obj;
                    handleAddOnceUpdate(pair.first, pair.second);
                }
                break;
                case MSG_REMOVE_UPDATE:
                    handleRemoveUpdate((AMapLocationListener) msg.obj);
                    break;
                case MSG_LOCATION_CHANGE:
                    handleLocationChange((AMapLocation) msg.obj);
                    break;
            }
        }
    }

    private static class ListenerPair extends Pair<AMapLocationListener, AMapLocationClientOption> {
        ListenerPair(AMapLocationListener first, AMapLocationClientOption second) {
            super(first, second);
        }

        static ListenerPair create(AMapLocationListener listener, AMapLocationClientOption option) {
            return new ListenerPair(listener, option);
        }
    }

    private static final int MSG_WATCH_START = 0;
    private static final int MSG_WATCH_CHANGE = 1;
    private static final int MSG_WATCH_STOP = 2;

    private static class ListenerHandler extends Handler {

        ListenerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_WATCH_START:
                    if (sListener != null) {
                        sListener.onWatchStart((AMapLocationClientOption) msg.obj);
                    }
                    break;
                case MSG_WATCH_CHANGE:
                    if (sListener != null) {
                        sListener.onWatchChange((AMapLocationClientOption) msg.obj);
                    }
                    break;
                case MSG_WATCH_STOP:
                    if (sListener != null) {
                        sListener.onWatchStop();
                    }
                    break;
            }
        }
    }

    public interface AMapLocationManagerListener {
        void onWatchStart(AMapLocationClientOption option);

        void onWatchChange(AMapLocationClientOption option);

        void onWatchStop();
    }
}
