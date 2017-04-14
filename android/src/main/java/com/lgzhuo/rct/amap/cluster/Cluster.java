package com.lgzhuo.rct.amap.cluster;

import com.amap.api.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yunrui on 2017/4/11.
 */

public class Cluster {
    private LatLng coordinate;
    private List<ClusterPoint> points = new ArrayList<>();

    public Cluster(LatLng coordinate) {
        this.coordinate = coordinate;
    }

    void addPoint(ClusterPoint point){
        this.points.add(point);
    }

    public LatLng getCoordinate() {
        return coordinate;
    }

    public List<ClusterPoint> getPoints() {
        return points;
    }
}
