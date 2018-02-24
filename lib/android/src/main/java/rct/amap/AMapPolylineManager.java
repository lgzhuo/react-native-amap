package rct.amap;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.amap.api.maps.model.LatLng;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Created by lgzhuo on 2017/3/16.
 */

class AMapPolylineManager extends SimpleViewManager<AMapPolyline> {

    private static final String REACT_NAME = "AMapPolyline";

    private final DisplayMetrics metrics;

    AMapPolylineManager(ReactApplicationContext reactContext) {
        super();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            metrics = new DisplayMetrics();
            ((WindowManager) reactContext.getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay()
                    .getRealMetrics(metrics);
        } else {
            metrics = reactContext.getResources().getDisplayMetrics();
        }
    }

    @Override
    public String getName() {
        return REACT_NAME;
    }

    @Override
    protected AMapPolyline createViewInstance(ThemedReactContext reactContext) {
        return new AMapPolyline(reactContext);
    }

    @ReactProp(name = "coordinates")
    public void setCoordinates(AMapPolyline view, ReadableArray dataArr) {
        List<LatLng> coordinates = new ArrayList<>(dataArr.size());
        for (int i = 0; i < dataArr.size(); i++) {
            ReadableMap coordinate = dataArr.getMap(i);
            coordinates.add(i, new LatLng(coordinate.getDouble("latitude"), coordinate.getDouble("longitude")));
        }
        view.setPoints(coordinates);
    }

    @ReactProp(name = "strokeWidth", defaultFloat = 1f)
    public void setStrokeWidth(AMapPolyline view, float widthInPoints) {
        float widthInScreenPx = metrics.density * widthInPoints; // done for parity with iOS
        view.setWidth(widthInScreenPx);
    }

    @ReactProp(name = "strokeColor", defaultInt = Color.RED, customType = "Color")
    public void setStrokeColor(AMapPolyline view, int color) {
        view.setColor(color);
    }

    @ReactProp(name = "geodesic", defaultBoolean = false)
    public void setGeodesic(AMapPolyline view, boolean geodesic) {
        view.setGeodesic(geodesic);
    }

    @ReactProp(name = "zIndex", defaultFloat = 1.0f)
    public void setZIndex(AMapPolyline view, float zIndex) {
        view.setZIndex(zIndex);
    }

    @ReactProp(name = "lineDash", defaultBoolean = false)
    public void setLineDash(AMapPolyline view, boolean lineDash) {
        view.setDottedLine(lineDash);
    }

    @Override
    @Nullable
    public Map getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.of(
                "onPress", MapBuilder.of("registrationName", "onPress")
        );
    }

}
