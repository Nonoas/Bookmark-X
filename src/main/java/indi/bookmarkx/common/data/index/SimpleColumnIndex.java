package indi.bookmarkx.common.data.index;

import java.util.HashMap;

/**
 * @author: codeleep
 * @createTime: 2024/03/20 15:01
 * @description: 简单列索引.以hashMap为底层存储
 */
public class SimpleColumnIndex<V> implements ColumnIndex<String,V>{

    private final HashMap<String,V> indexMap = new HashMap<>();

    @Override
    public V hunt(String key) {
        return indexMap.get(key);
    }

    @Override
    public void drawUp(String key, V value) {
        indexMap.put(key, value);
    }

    @Override
    public void drawDown(String key) {
        indexMap.remove(key);
    }
}
