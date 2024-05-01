package indi.bookmarkx.ui.tree;


import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import indi.bookmarkx.common.BaseColors;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

/**
 * 标签树节点渲染
 *
 * @author Nonoas
 * @version 1.0
 * @date 2024/5/2
 * @since 1.2.1
 */
public class BmkTreeCellRenderer extends DefaultTreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(JTree tree,
                                                  Object value,
                                                  boolean selected,
                                                  boolean expanded,
                                                  boolean isLeaf,
                                                  int row,
                                                  boolean hasFocus) {
        JComponent component = (JComponent) super.getTreeCellRendererComponent(tree, value, selected, expanded, isLeaf, row, hasFocus);
        component.setOpaque(true);
        component.setBackground(Color.RED);

        BookmarkTreeNode node = (BookmarkTreeNode) value;
        Icon icon = null;
        if (0 == row) {
            // if is root node
            icon = AllIcons.Nodes.Module;
        } else if (row > 0) {
            icon = node.isBookmark()
                    ? IconLoader.findIcon("icons/bookmark.svg")
                    : AllIcons.Nodes.Folder;
        }
        setIcon(icon);
        return component;
    }

    @Override
    public Color getBorderSelectionColor() {
        return BaseColors.TRANSPARENT;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
    }
}
