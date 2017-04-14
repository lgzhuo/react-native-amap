package com.lgzhuo.rct.amap.helper;

import com.facebook.react.bridge.Dynamic;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;

/**
 * Created by lgzhuo on 2017/4/13.
 */

public class ReadableMapWrapper implements ReadableMap {

    private ReadableMap map;

    private ReadableMapWrapper(ReadableMap map) {
        this.map = map;
    }

    @Override
    public boolean hasKey(String name) {
        return map != null && map.hasKey(name);
    }

    @Override
    public boolean isNull(String name) {
        return !hasKey(name) || map.isNull(name);
    }

    @Override
    public boolean getBoolean(String name) {
        return isType(name, ReadableType.Boolean) && map.getBoolean(name);
    }

    @Override
    public double getDouble(String name) {
        return isType(name, ReadableType.Number) ? map.getDouble(name) : 0;
    }

    @Override
    public int getInt(String name) {
        return isType(name, ReadableType.Number) ? map.getInt(name) : 0;
    }

    @Override
    public String getString(String name) {
        return isType(name, ReadableType.String) ? map.getString(name) : null;
    }

    @Override
    public ReadableArray getArray(String name) {
        return isType(name, ReadableType.Array) ? map.getArray(name) : null;
    }

    @Override
    public ReadableMap getMap(String name) {
        return isType(name, ReadableType.Map) ? map.getMap(name) : null;
    }

    @Override
    public ReadableType getType(String name) {
        return isNull(name) ? ReadableType.Null : map.getType(name);
    }

    @Override
    public ReadableMapKeySetIterator keySetIterator() {
        return map == null ? NONE_KEY_SET_ITERATOR : map.keySetIterator();
    }

    @Override
    public Dynamic getDynamic(String name) {
        return map == null ? NONE_DYNAMIC : map.getDynamic(name);
    }

    private boolean isType(String name, ReadableType type) {
        return getType(name) == type;
    }

    public static ReadableMapWrapper wrap(ReadableMap map) {
        return new ReadableMapWrapper(map);
    }

    private static final ReadableMapKeySetIterator NONE_KEY_SET_ITERATOR = new ReadableMapKeySetIterator() {
        @Override
        public boolean hasNextKey() {
            return false;
        }

        @Override
        public String nextKey() {
            return null;
        }
    };

    static final Dynamic NONE_DYNAMIC = new Dynamic() {
        @Override
        public boolean isNull() {
            return true;
        }

        @Override
        public boolean asBoolean() {
            return false;
        }

        @Override
        public double asDouble() {
            return 0;
        }

        @Override
        public int asInt() {
            return 0;
        }

        @Override
        public String asString() {
            return null;
        }

        @Override
        public ReadableArray asArray() {
            return null;
        }

        @Override
        public ReadableMap asMap() {
            return null;
        }

        @Override
        public ReadableType getType() {
            return null;
        }

        @Override
        public void recycle() {

        }
    };
}
