package indi.bookmarkx.ui.tree;


import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import indi.bookmarkx.model.BookmarkNodeModel;
import indi.bookmarkx.persistence.MySettings;

import javax.swing.Icon;
import javax.swing.JTree;

/**
 * 标签树节点渲染
 *
 * @author Nonoas
 * @version 1.0
 * @date 2024/5/2
 * @since 1.2.1
 */
public class BmkTreeCellRenderer extends ColoredTreeCellRenderer {

    @Override
    public void customizeCellRenderer(JTree tree,
                                      Object value,
                                      boolean selected,
                                      boolean expanded,
                                      boolean leaf,
                                      int row,
                                      boolean hasFocus) {
        BookmarkTreeNode node = (BookmarkTreeNode) value;
        MySettings settings = MySettings.getInstance();

        if (settings.isShowNodeNumbers()) {
            String number = BookmarkTreeOrder.number(node);
            if (!number.isEmpty()) {
                append(number + ". ");
            }
        }

        String name = String.valueOf(node.getUserObject());
        String moduleSuffix = node.isBookmark() && settings.isShowModuleSuffix()
                ? moduleSuffix((BookmarkNodeModel) node.getUserObject())
                : "";
        if (!moduleSuffix.isEmpty() && name.endsWith(moduleSuffix)) {
            append(name.substring(0, name.length() - moduleSuffix.length()));
            append(moduleSuffix, SimpleTextAttributes.GRAYED_ATTRIBUTES);
        } else {
            append(name);
            if (!moduleSuffix.isEmpty()) {
                append(moduleSuffix, SimpleTextAttributes.GRAYED_ATTRIBUTES);
            }
        }

        Icon icon = null;
        if (0 == row) {
            // if is root node
            icon = AllIcons.Nodes.Module;
        } else if (row > 0) {
            icon = node.isBookmark()
                    ? IconLoader.findIcon("icons/bookmark.svg", BmkTreeCellRenderer.class)
                    : AllIcons.Nodes.Folder;
            if (node.isBookmark()) {
                BookmarkNodeModel model = (BookmarkNodeModel) node.getUserObject();
                if (null == model.getOpenFileDescriptor()) {
                    icon = IconLoader.findIcon("icons/dissmiss.svg", BmkTreeCellRenderer.class);
                }
            }
        }
        setIcon(icon);
    }

    private static String moduleSuffix(BookmarkNodeModel model) {
        String moduleName = model.getModuleName();
        return moduleName == null || moduleName.isBlank() ? "" : " [" + moduleName + "]";
    }
}
