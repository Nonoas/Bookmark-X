package indi.bookmarkx.ui.tree;

import indi.bookmarkx.model.BookmarkNodeModel;
import indi.bookmarkx.model.GroupNodeModel;
import org.junit.jupiter.api.Test;

import javax.swing.tree.DefaultTreeModel;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BookmarkTreeOrderTest {
    @Test
    void numbersMixedChildrenAndNestedGroupsWithoutRenaming() {
        BookmarkTreeNode root = group("project");
        BookmarkTreeNode first = group("group");
        BookmarkTreeNode second = bookmark("bookmark");
        BookmarkTreeNode child = bookmark("child");
        root.add(first);
        root.add(second);
        first.add(child);
        assertEquals("", BookmarkTreeOrder.number(root));
        assertEquals("1", BookmarkTreeOrder.number(first));
        assertEquals("2", BookmarkTreeOrder.number(second));
        assertEquals("1.1", BookmarkTreeOrder.number(child));
        assertEquals("child", child.getUserObject().toString());
    }

    @Test
    void movingMultipleBookmarksDownPreservesTheirOrderAndUpdatesIndices() {
        BookmarkTreeNode root = group("project");
        BookmarkTreeNode a = bookmark("a"), b = bookmark("b"), c = bookmark("c"), d = bookmark("d");
        root.add(a); root.add(b); root.add(c); root.add(d);
        assertTrue(BookmarkTreeOrder.move(new DefaultTreeModel(root), List.of(a, b), root, 4));
        assertSame(c, root.getChildAt(0));
        assertSame(d, root.getChildAt(1));
        assertSame(a, root.getChildAt(2));
        assertSame(b, root.getChildAt(3));
        assertEquals("3", BookmarkTreeOrder.number(a));
        assertEquals(2, ((BookmarkNodeModel) a.getUserObject()).getIndex());
    }

    @Test
    void movingUpAndAcrossGroupsRenumbersBothGroups() {
        BookmarkTreeNode root = group("project"), left = group("left"), right = group("right");
        BookmarkTreeNode a = bookmark("a"), b = bookmark("b"), c = bookmark("c");
        root.add(left); root.add(right); left.add(a); left.add(b); right.add(c);
        DefaultTreeModel model = new DefaultTreeModel(root);
        assertTrue(BookmarkTreeOrder.move(model, List.of(b), left, 0));
        assertEquals("1.1", BookmarkTreeOrder.number(b));
        assertTrue(BookmarkTreeOrder.move(model, List.of(b), right, -1));
        assertEquals("1.1", BookmarkTreeOrder.number(a));
        assertEquals("2.2", BookmarkTreeOrder.number(b));
        assertEquals(1, ((BookmarkNodeModel) b.getUserObject()).getIndex());
    }

    @Test
    void rejectsCyclesAndRootMovesWithoutChangingTheTree() {
        BookmarkTreeNode root = group("project"), parent = group("parent"), child = group("child");
        root.add(parent); parent.add(child);
        DefaultTreeModel model = new DefaultTreeModel(root);
        assertFalse(BookmarkTreeOrder.move(model, List.of(parent), child, -1));
        assertFalse(BookmarkTreeOrder.move(model, List.of(root), child, -1));
        assertSame(root, parent.getParent());
        assertSame(parent, child.getParent());
    }

    @Test
    void movingParentAndChildSelectionMovesSubtreeOnlyOnce() {
        BookmarkTreeNode root = group("project"), parent = group("parent"), target = group("target");
        BookmarkTreeNode child = bookmark("child");
        root.add(parent); root.add(target); parent.add(child);
        assertTrue(BookmarkTreeOrder.move(new DefaultTreeModel(root), List.of(parent, child), target, -1));
        assertSame(parent, target.getChildAt(0));
        assertSame(child, parent.getChildAt(0));
        assertEquals("1.1.1", BookmarkTreeOrder.number(child));
    }

    private BookmarkTreeNode group(String name) {
        return new BookmarkTreeNode(new GroupNodeModel(name));
    }

    private BookmarkTreeNode bookmark(String name) {
        BookmarkNodeModel model = new BookmarkNodeModel();
        model.setName(name);
        return new BookmarkTreeNode(model);
    }
}
