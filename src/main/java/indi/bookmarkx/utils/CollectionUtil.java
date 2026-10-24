package indi.bookmarkx.utils;

import java.util.Collection;

/**
 * @author huangshengsheng
 * @date 2026/10/24 16:46
 */
public final class CollectionUtil {

    private CollectionUtil() {
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
}
