package com.lgzhuo.rct.amap;

import android.content.Context;
import android.graphics.Canvas;
import android.view.TextureView;
import android.view.View;

import com.facebook.react.views.view.ReactViewGroup;

/**
 * Created by lgzhuo on 2017/4/12.
 */

public class ARTContainer extends ReactViewGroup {
    public ARTContainer(Context context) {
        super(context);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (child instanceof TextureView) {
            TextureView surfaceView = (TextureView) child;
            if (AMapUtils.drawSurfaceView(canvas, surfaceView)) {
                return true;
            }
        }
        return super.drawChild(canvas, child, drawingTime);
    }
}
