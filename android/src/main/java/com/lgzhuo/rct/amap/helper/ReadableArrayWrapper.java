package com.lgzhuo.rct.amap.helper;

import com.facebook.react.bridge.Dynamic;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;

/**
 * Created by lgzhuo on 2017/4/13.
 */

public class ReadableArrayWrapper implements ReadableArray {
    private ReadableArray array;

    private ReadableArrayWrapper(ReadableArray array) {
        this.array = array;
    }

    @Override
    public int size() {
        return array == null ? 0 : array.size();
    }

    @Override
    public boolean isNull(int index) {
        return array == null || index >= array.size() || array.isNull(index);
    }

    @Override
    public boolean getBoolean(int index) {
        return isType(index, ReadableType.Boolean) && array.getBoolean(index);
    }

    @Override
    public double getDouble(int index) {
        return isType(index, ReadableType.Number) ? array.getDouble(index) : 0;
    }

    @Override
    public int getInt(int index) {
        return isType(index, ReadableType.Number) ? array.getInt(index) : 0;
    }

    @Override
    public String getString(int index) {
        return isType(index, ReadableType.String) ? array.getString(index) : null;
    }

    @Override
    public ReadableArray getArray(int index) {
        return isType(index, ReadableType.Array) ? array.getArray(index) : null;
    }

    @Override
    public ReadableMap getMap(int index) {
        return isType(index, ReadableType.Map) ? array.getMap(index) : null;
    }

    @Override
    public ReadableType getType(int index) {
        return isNull(index) ? ReadableType.Null : array.getType(index);
    }

    @Override
    public Dynamic getDynamic(int index) {
        return array == null ? ReadableMapWrapper.NONE_DYNAMIC : array.getDynamic(index);
    }

    private boolean isType(int index, ReadableType type) {
        return getType(index) == type;
    }

    public static ReadableArrayWrapper wrap(ReadableArray array) {
        return new ReadableArrayWrapper(array);
    }
}
