package rct.amap.helper;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;

import java.util.HashMap;

/**
 * Created by lgzhuo on 2017/4/13.
 */

public class ReadableMapWrapper {

    private ReadableMap map;

    private ReadableMapWrapper(ReadableMap map) {
        this.map = map;
    }

    public boolean hasKey(String name) {
        return map != null && map.hasKey(name);
    }

    public boolean isNull(String name) {
        return !hasKey(name) || map.isNull(name);
    }

    public boolean getBoolean(String name) {
        return isType(name, ReadableType.Boolean) && map.getBoolean(name);
    }

    public double getDouble(String name) {
        return isType(name, ReadableType.Number) ? map.getDouble(name) : 0;
    }

    public int getInt(String name) {
        return isType(name, ReadableType.Number) ? map.getInt(name) : 0;
    }

    public String getString(String name) {
        return isType(name, ReadableType.String) ? map.getString(name) : null;
    }

    public ReadableArray getArray(String name) {
        return isType(name, ReadableType.Array) ? map.getArray(name) : null;
    }

    public ReadableMap getMap(String name) {
        return isType(name, ReadableType.Map) ? map.getMap(name) : null;
    }

    public ReadableType getType(String name) {
        return isNull(name) ? ReadableType.Null : map.getType(name);
    }

    public HashMap<String, Object> toHashMap() {
        return map == null ? null : map.toHashMap();
    }

    private boolean isType(String name, ReadableType type) {
        return getType(name) == type;
    }

    public static ReadableMapWrapper wrap(ReadableMap map) {
        return new ReadableMapWrapper(map);
    }
}
