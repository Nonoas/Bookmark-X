package indi.bookmarkx.utils;

import com.intellij.openapi.util.IconLoader;
import indi.bookmarkx.model.AbstractTreeNodeModel;
import indi.bookmarkx.ui.tree.BookmarkTreeNode;

import javax.swing.Icon;
import javax.swing.event.TreeModelEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Nonoas
 * @date 2024/12/15
 * @since
 */
public class UIUtil {
    // 打印事件详情的辅助方法
    public static List<AbstractTreeNodeModel> getEffectedModels(TreeModelEvent e) {
        // 获取受影响的子节点
        List<AbstractTreeNodeModel> list = new ArrayList<>();
        Object[] affectedNodes = e.getChildren();
        if (null == affectedNodes) {
            return list;
        }
        return Arrays.stream(affectedNodes)
                .filter(node -> node instanceof BookmarkTreeNode)
                .map(node -> (BookmarkTreeNode) node)
                .map(node -> (AbstractTreeNodeModel) node.getUserObject())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
