package indi.bookmarkx.common.data.index;

import java.util.List;

/**
 * @author: codeleep
 * @createTime: 2024/03/20 15:00
 * @description: 列索引
 */
public interface ColumnIndex<K,V> {

    V hunt(K key);

    void drawUp(K key, V value);

    void drawDown(K key);
}
