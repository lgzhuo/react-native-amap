package com.lgzhuo.rct.amap;

import android.graphics.Color;
import android.view.View;

import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.LayoutShadowNode;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.lgzhuo.rct.amap.helper.ReadableMapWrapper;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Created by lgzhuo on 2017/3/9.
 */

class AMapMarkerManager extends ViewGroupManager<AMapMarker> {

    private static final String REACT_NAME = "AMapMarker";
    private static final int SHOW_INFO_WINDOW = 1;
    private static final int HIDE_INFO_WINDOW = 2;

    @Override
    public String getName() {
        return REACT_NAME;
    }

    @Override
    protected AMapMarker createViewInstance(ThemedReactContext reactContext) {
        return new AMapMarker(reactContext);
    }

    @Nullable
    @Override
    public Map getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.of(
                "onPress", MapBuilder.of("registrationName", "onPress"),
                "onCalloutPress", MapBuilder.of("registrationName", "onCalloutPress")
        );
    }

    @ReactProp(name = "anchor")
    public void setAnchor(AMapMarker view, ReadableMap map) {
        float u = map != null && map.hasKey("u") ? (float) map.getDouble("u") : 0.5f;
        float v = map != null && map.hasKey("v") ? (float) map.getDouble("v") : 1.0f;
        view.setAnchor(u, v);
    }

    @ReactProp(name = "draggable")
    public void setDraggable(AMapMarker view, boolean draggable) {
        view.setDraggable(draggable);
    }

    @ReactProp(name = "calloutEnabled")
    public void setInfoWindowEnable(AMapMarker view, boolean calloutEnabled) {
        view.setInfoWindowEnable(calloutEnabled);
    }

    @ReactProp(name = "coordinate")
    public void setCoordinate(AMapMarker view, ReadableMap map) {
        if (map != null) {
            LatLng latLng = new LatLng(map.getDouble("latitude"), map.getDouble("longitude"));
            view.setPosition(latLng);
        } else {
            view.setPosition(null);
        }
    }

    @ReactProp(name = "rotateAngle")
    public void setRotateAngle(AMapMarker view, float rotate) {
        view.setRotateAngle(rotate);
    }

    @ReactProp(name = "flat")
    public void setFlat(AMapMarker view, boolean flat) {
        view.setFlat(flat);
    }

    @ReactProp(name = "gps")
    public void setGps(AMapMarker view, boolean gps) {
        view.setGps(gps);
    }

    @ReactProp(name = "calloutOffset")
    public void setCalloutOffset(AMapMarker view, ReadableMap map) {
        map = ReadableMapWrapper.wrap(map);
        int offsetX = map.getInt("x");
        int offsetY = map.getInt("y");
        view.setInfoWindowOffset(offsetX, offsetY);
    }

    @ReactProp(name = "description")
    public void setDescription(AMapMarker view, String description) {
        view.setSnippet(description);
    }

    @ReactProp(name = "title")
    public void setTitle(AMapMarker view, String title) {
        view.setTitle(title);
    }

    @ReactProp(name = "visible", defaultBoolean = true)
    public void setVisible(AMapMarker view, boolean visible) {
        view.setVisible(visible);
    }

    //    @Override
    @ReactProp(name = "zIndex")
    public void setZIndex(AMapMarker view, float zIndex) {
//        super.setZIndex(view, zIndex);
        view.setZIndex(zIndex);
    }

    @ReactProp(name = "image")
    public void setImage(AMapMarker view, @Nullable String source) {
        view.setImage(source);
    }

    @ReactProp(name = "pinColor", defaultInt = Color.RED, customType = "Color")
    public void setPinColor(AMapMarker view, int pinColor) {
        float[] hsv = new float[3];
        Color.colorToHSV(pinColor, hsv);
        // NOTE: android only supports a hue
        view.setMarkerHue(hsv[0]);
    }

    @Override
    public Class<? extends LayoutShadowNode> getShadowNodeClass() {
        return SizeReportingShadowNode.class;
    }

    @Override
    public LayoutShadowNode createShadowNodeInstance() {
        return new SizeReportingShadowNode();
    }

    @Override
    public void updateExtraData(AMapMarker root, Object extraData) {
        super.updateExtraData(root, extraData);
        HashMap data = (HashMap) extraData;
        Float width = (Float) data.get("width");
        Float height = (Float) data.get("height");
        root.update(width.intValue(), height.intValue());
    }

    @Override
    public void addView(AMapMarker parent, View child, int index) {
        if (child instanceof AMapCallout) {
            parent.setcallout((AMapCallout) child);
        } else {
            super.addView(parent, child, index);
            parent.update();
        }
    }

    @Override
    public void removeViewAt(AMapMarker parent, int index) {
        super.removeViewAt(parent, index);
        parent.update();
    }

    @Override
    public int getChildCount(AMapMarker parent) {
        return super.getChildCount(parent);
    }

    @Override
    public View getChildAt(AMapMarker parent, int index) {
        return super.getChildAt(parent, index);
    }

    @Override
    @Nullable
    public Map<String, Integer> getCommandsMap() {
        return MapBuilder.of(
                "showCallout", SHOW_INFO_WINDOW,
                "hideCallout", HIDE_INFO_WINDOW
        );
    }

    @Override
    public void receiveCommand(AMapMarker view, int commandId, @Nullable ReadableArray args) {
        switch (commandId) {
            case SHOW_INFO_WINDOW:
                view.getFeature().showInfoWindow();
                break;

            case HIDE_INFO_WINDOW:
                view.getFeature().hideInfoWindow();
                break;
        }
    }
}
