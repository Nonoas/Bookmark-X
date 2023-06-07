package indi.bookmarkx.tree;

import indi.bookmarkx.model.BookmarkNodeModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.tree.TreePath;

/**
 * @author Nonoas
 * @date 2023/6/8
 */
class BookmarkTreeTest {

    @Test
    void addNodeTest() {
        BookmarkTree tree = new BookmarkTree();

        BookmarkNodeModel model = new BookmarkNodeModel();
        model.setUuid("uuid");

        BookmarkTreeNode node = new BookmarkTreeNode(model);

        tree.add(node);

        int rowForPath = tree.getRowForPath(new TreePath(node.getPath()));
        // 添加之后的节点必须存在于当前树上
        Assertions.assertNotEquals(rowForPath, -1, "添加之后的节点必须存在于当前树上");
    }

}