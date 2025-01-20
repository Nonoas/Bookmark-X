package indi.bookmarkx.common;

import com.intellij.openapi.util.IconLoader;

import javax.swing.Icon;
import java.util.Objects;

/**
 * @author Nonoas
 * @version 2.2.0
 * @date 2025/1/1
 * @since 2.2.0
 */
public interface MyIcons {
    Icon BOOKMARK = getIcon("icons/bookmark.svg");

    static Icon getIcon(String path) {
        return Objects.requireNonNull(IconLoader.findIcon(path));
    }
}
