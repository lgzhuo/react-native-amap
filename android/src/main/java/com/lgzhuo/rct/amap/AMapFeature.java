package com.lgzhuo.rct.amap;

import android.content.Context;

import com.amap.api.maps.AMap;
import com.facebook.react.views.view.ReactViewGroup;

/**
 * Created by lgzhuo on 2017/3/9.
 */

public abstract class AMapFeature extends ReactViewGroup {

    public AMapFeature(Context context) {
        super(context);
    }

    public abstract void addToMap(AMap map);

    public abstract void removeFromMap(AMap map);

    public abstract Object getFeature();
}
