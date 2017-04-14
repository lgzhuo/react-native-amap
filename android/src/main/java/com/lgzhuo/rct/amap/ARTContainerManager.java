package com.lgzhuo.rct.amap;

import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.views.view.ReactViewGroup;
import com.facebook.react.views.view.ReactViewManager;

/**
 * Created by lgzhuo on 2017/4/12.
 */

public class ARTContainerManager extends ReactViewManager {

    private static final String REACT_NAME = "ARTContainer";

    @Override
    public String getName() {
        return REACT_NAME;
    }

    @Override
    public ReactViewGroup createViewInstance(ThemedReactContext reactContext) {
        return new ARTContainer(reactContext);
    }
}
