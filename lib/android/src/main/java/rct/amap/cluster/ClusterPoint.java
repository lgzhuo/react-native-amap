package rct.amap.cluster;

import com.amap.api.maps.model.LatLng;

/**
 * Created by lgzhuo on 2017/4/11.
 */

public class ClusterPoint {
    public final double latitude;
    public final double longitude;
    public final String id;
    public final int idx;

    public ClusterPoint(double latitude, double longitude, String id, int idx) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.id = id;
        this.idx = idx;
    }

    LatLng getCoordinate() {
        return new LatLng(latitude, longitude);
    }
}
