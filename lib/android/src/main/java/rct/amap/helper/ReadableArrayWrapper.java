package rct.amap.helper;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;

import java.util.ArrayList;

/**
 * Created by lgzhuo on 2017/4/13.
 */

public class ReadableArrayWrapper {
    private ReadableArray array;

    private ReadableArrayWrapper(ReadableArray array) {
        this.array = array;
    }

    public int size() {
        return array == null ? 0 : array.size();
    }

    public boolean isNull(int index) {
        return array == null || index >= array.size() || array.isNull(index);
    }

    public boolean getBoolean(int index) {
        return isType(index, ReadableType.Boolean) && array.getBoolean(index);
    }

    public double getDouble(int index) {
        return isType(index, ReadableType.Number) ? array.getDouble(index) : 0;
    }

    public int getInt(int index) {
        return isType(index, ReadableType.Number) ? array.getInt(index) : 0;
    }

    public String getString(int index) {
        return isType(index, ReadableType.String) ? array.getString(index) : null;
    }

    public ReadableArray getArray(int index) {
        return isType(index, ReadableType.Array) ? array.getArray(index) : null;
    }

    public ReadableMap getMap(int index) {
        return isType(index, ReadableType.Map) ? array.getMap(index) : null;
    }

    public ReadableType getType(int index) {
        return isNull(index) ? ReadableType.Null : array.getType(index);
    }

    public ArrayList<Object> toArrayList() {
        return array == null ? null : array.toArrayList();
    }

    private boolean isType(int index, ReadableType type) {
        return getType(index) == type;
    }

    public static ReadableArrayWrapper wrap(ReadableArray array) {
        return new ReadableArrayWrapper(array);
    }
}
