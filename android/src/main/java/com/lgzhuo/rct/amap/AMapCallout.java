package com.lgzhuo.rct.amap;

import android.content.Context;

import com.facebook.react.views.view.ReactViewGroup;

/**
 * Created by lgzhuo on 2017/3/13.
 */

public class AMapCallout extends ReactViewGroup {

    int width;
    int height;
    private boolean tooltip = false;

    public AMapCallout(Context context) {
        super(context);
    }

    public boolean isTooltip() {
        return tooltip;
    }

    public void setTooltip(boolean tooltip) {
        this.tooltip = tooltip;
    }
}
