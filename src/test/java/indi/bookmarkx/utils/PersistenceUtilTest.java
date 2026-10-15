package indi.bookmarkx.utils;

import indi.bookmarkx.model.GroupNodeModel;
import indi.bookmarkx.model.po.BookmarkPO;
import indi.bookmarkx.ui.tree.BookmarkTreeNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersistenceUtilTest {

    @Test
    void loadsEmptyGroupWithoutCommonsCollections() {
        BookmarkPO group = new BookmarkPO("empty", "");
        BookmarkTreeNode node = PersistenceUtil.generateTreeNode(group, null);

        assertEquals(0, node.getChildCount());
        assertEquals("empty", ((GroupNodeModel) node.getUserObject()).getName());
    }

    @Test
    void restoresNestedGroupsAndTheirOrder() {
        BookmarkPO root = new BookmarkPO("root", "");
        BookmarkPO first = new BookmarkPO("first", "");
        BookmarkPO second = new BookmarkPO("second", "");
        first.setChildren(new ArrayList<>(List.of(new BookmarkPO("nested", ""))));
        root.setChildren(new ArrayList<>(List.of(first, second)));

        BookmarkTreeNode node = PersistenceUtil.generateTreeNode(root, null);
        BookmarkTreeNode firstNode = (BookmarkTreeNode) node.getChildAt(0);
        assertEquals(1, firstNode.getChildCount());
        assertEquals(2, node.getChildCount());
        assertEquals("first", ((GroupNodeModel) firstNode.getUserObject()).getName());
        BookmarkTreeNode secondNode = (BookmarkTreeNode) node.getChildAt(1);
        assertEquals("second", ((GroupNodeModel) secondNode.getUserObject()).getName());
        BookmarkTreeNode nestedNode = (BookmarkTreeNode) firstNode.getChildAt(0);
        assertEquals("nested", ((GroupNodeModel) nestedNode.getUserObject()).getName());
    }
}
