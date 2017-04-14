package com.lgzhuo.rct.amap.cluster;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by yunrui on 2017/4/11.
 */

public class ClusterComputer {
    private Handler mClusterHandler;
    private HandlerThread mClusterThread;
    private Handler mListenerHandler;
    private HandlerThread mListenerThread;

    private List<ClusterPoint> mClusterPointList = new ArrayList<>();
    private float mClusterSize;
    private float mScalePerPixel;
    private LatLngBounds mVisibleBounds;
    private OnClusterListener mOnClusterListener;

    public ClusterComputer() {
        mClusterThread = new HandlerThread("AMap Cluster");
        mClusterThread.start();
        mClusterHandler = new ClusterHandler(mClusterThread.getLooper());

        mListenerThread = new HandlerThread("AMap Cluster Listener");
        mListenerThread.start();
        mListenerHandler = new ListenerHandler(mListenerThread.getLooper());
    }

    public void close() {
        mClusterHandler.removeCallbacksAndMessages(null);
        mClusterThread.quit();
        mListenerHandler.removeCallbacksAndMessages(null);
        mListenerThread.quit();
    }

    public void setOnClusterListener(OnClusterListener mOnClusterListener) {
        this.mOnClusterListener = mOnClusterListener;
    }

    /*dispatch*/

    public void setPoints(Collection<ClusterPoint> points) {
        mClusterHandler.removeMessages(MSG_SET_POINTS);
        mClusterHandler.obtainMessage(MSG_SET_POINTS, points).sendToTarget();
    }

    public void setSize(float clusterSize) {
        mClusterHandler.removeMessages(MSG_SET_SIZE);
        mClusterHandler.obtainMessage(MSG_SET_SIZE, clusterSize).sendToTarget();
    }

    public void setScaleAndBounds(float scalePerPixel, LatLngBounds visibleBounds) {
        mClusterHandler.removeMessages(MSG_SET_SCALE_AND_BOUNDS);
        mClusterHandler.obtainMessage(MSG_SET_SCALE_AND_BOUNDS, new Object[]{scalePerPixel, visibleBounds}).sendToTarget();
    }

    public void calculate() {
        mClusterHandler.removeMessages(MSG_CALCULATE_CLUSTER);
        mClusterHandler.obtainMessage(MSG_CALCULATE_CLUSTER).sendToTarget();
    }

    /*handle*/

    private void handleCalculate() {
        if (mClusterPointList.isEmpty() || mClusterSize <= 0 || mScalePerPixel <= 0 || mVisibleBounds == null) {
            return;
        }
        double clusterDistance = mClusterSize * mScalePerPixel;
        List<Cluster> clusterList = new ArrayList<>();
        for (ClusterPoint point : mClusterPointList) {
            LatLng coordinate = point.getCoordinate();
            toCluster:
            if (mVisibleBounds.contains(coordinate)) {
                for (Cluster cluster : clusterList) {
                    double distance = AMapUtils.calculateLineDistance(coordinate, cluster.getCoordinate());
                    if (distance < clusterDistance) {
                        cluster.addPoint(point);
                        break toCluster;
                    }
                }
                Cluster cluster = new Cluster(coordinate);
                clusterList.add(cluster);
                cluster.addPoint(point);
            }
        }
        mListenerHandler.removeMessages(MSG_ON_COMPLETE);
        mListenerHandler.obtainMessage(MSG_ON_COMPLETE, clusterList).sendToTarget();
    }

    private void handleSetPoints(Collection<ClusterPoint> points) {
        mClusterPointList.clear();
        if (points != null && !points.isEmpty()) {
            mClusterPointList.addAll(points);
        }
        calculate();
    }

    private void handleSetSize(float clusterSize) {
        mClusterSize = clusterSize;
        calculate();
    }

    private void handleSetScaleAndBounds(float scalePerPixel, LatLngBounds bounds) {
        mScalePerPixel = scalePerPixel;
        mVisibleBounds = bounds;
        calculate();
    }

    private void onCalculateComplete(List<Cluster> clusterList) {
        if (this.mOnClusterListener != null) {
            this.mOnClusterListener.onClusterComplete(clusterList);
        }
    }

    private static final int MSG_CALCULATE_CLUSTER = 1;
    private static final int MSG_SET_POINTS = 2;
    private static final int MSG_SET_SIZE = 3;
    private static final int MSG_SET_SCALE_AND_BOUNDS = 4;

    private class ClusterHandler extends Handler {
        ClusterHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CALCULATE_CLUSTER:
                    handleCalculate();
                    break;
                case MSG_SET_POINTS:
                    List<ClusterPoint> points = (List<ClusterPoint>) msg.obj;
                    handleSetPoints(points);
                    break;
                case MSG_SET_SIZE:
                    float size = (float) msg.obj;
                    handleSetSize(size);
                    break;
                case MSG_SET_SCALE_AND_BOUNDS:
                    Object[] objs = (Object[]) msg.obj;
                    handleSetScaleAndBounds((float) objs[0], (LatLngBounds) objs[1]);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    private static final int MSG_ON_COMPLETE = 1;

    private class ListenerHandler extends Handler {
        public ListenerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ON_COMPLETE:
                    List<Cluster> clusterList = (List<Cluster>) msg.obj;
                    onCalculateComplete(clusterList);
                    break;
            }
        }
    }
}
