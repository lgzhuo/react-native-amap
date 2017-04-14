package com.lgzhuo.rct.amap;

import com.facebook.react.uimanager.LayoutShadowNode;
import com.facebook.react.uimanager.UIViewOperationQueue;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lgzhuo on 2017/3/13.
 */

public class SizeReportingShadowNode extends LayoutShadowNode {

    @Override
    public void onCollectExtraUpdates(UIViewOperationQueue uiViewOperationQueue) {
        super.onCollectExtraUpdates(uiViewOperationQueue);

        Map<String, Float> data = new HashMap<>();
        data.put("width", getLayoutWidth());
        data.put("height", getLayoutHeight());

        uiViewOperationQueue.enqueueUpdateExtraData(getReactTag(), data);
    }
}
