package com.lgzhuo.rct.amap;

import com.facebook.react.uimanager.LayoutShadowNode;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;

import java.util.Map;

/**
 * Created by lgzhuo on 2017/3/13.
 */

public class AMapCalloutManager extends ViewGroupManager<AMapCallout> {
    private static final String REACT_CLASS = "AMapCallout";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected AMapCallout createViewInstance(ThemedReactContext reactContext) {
        return new AMapCallout(reactContext);
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
    public void updateExtraData(AMapCallout view, Object extraData) {
        Map data = (Map) extraData;
        Float width = (Float) data.get("width");
        Float height = (Float) data.get("height");
        view.width = width.intValue();
        view.height = height.intValue();
    }
}
