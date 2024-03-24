package indi.bookmarkx;

import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.util.IconLoader;
import indi.bookmarkx.ui.tree.BookmarkTree;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Nonoas
 * @date 2023/9/3
 */
public class LineMarkIRenderer extends GutterIconRenderer {
    @Override
    public boolean equals(Object obj) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean isDumbAware() {
        return super.isDumbAware();
    }

    @NotNull
    @Override
    public Icon getIcon() {
        // 自定义标记的图标
        return IconLoader.getIcon("icons/bookmark.svg", BookmarkTree.class);
    }
}
