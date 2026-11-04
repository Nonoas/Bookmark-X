package indi.bookmarkx.ui.tree;

import indi.bookmarkx.model.BookmarkNodeModel;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Display numbering follows sibling order; names and bookmark identities stay unchanged. */
final class BookmarkTreeOrder {
    private BookmarkTreeOrder() {
    }

    static String number(BookmarkTreeNode node) {
        List<String> parts = new ArrayList<>();
        TreeNode child = node;
        while (child.getParent() != null) {
            TreeNode parent = child.getParent();
            parts.add(0, Integer.toString(parent.getIndex(child) + 1));
            child = parent;
        }
        return String.join(".", parts);
    }

    static boolean move(DefaultTreeModel model, List<BookmarkTreeNode> selection,
                        BookmarkTreeNode target, int childIndex) {
        if (target == null || !target.isGroup() || target.getRoot() != model.getRoot()
                || childIndex < -1 || childIndex > target.getChildCount() || selection.isEmpty()) {
            return false;
        }
        Set<BookmarkTreeNode> unique = new LinkedHashSet<>(selection);
        for (BookmarkTreeNode node : unique) {
            if (node == null || node.getParent() == null || node.getRoot() != model.getRoot()
                    || target.isNodeAncestor(node)) {
                return false;
            }
        }
        // Moving a selected group already moves its selected descendants.
        List<BookmarkTreeNode> nodes = new ArrayList<>();
        for (BookmarkTreeNode node : unique) {
            boolean covered = false;
            for (TreeNode parent = node.getParent(); parent != null; parent = parent.getParent()) {
                if (unique.contains(parent)) {
                    covered = true;
                    break;
                }
            }
            if (!covered) nodes.add(node);
        }
        int index = childIndex < 0 ? target.getChildCount() : childIndex;
        int insertionIndex = index;
        Set<BookmarkTreeNode> parents = new LinkedHashSet<>();
        parents.add(target);
        for (BookmarkTreeNode node : nodes) {
            BookmarkTreeNode parent = (BookmarkTreeNode) node.getParent();
            parents.add(parent);
            if (parent == target && parent.getIndex(node) < index) insertionIndex--;
        }
        for (BookmarkTreeNode node : nodes) model.removeNodeFromParent(node);
        for (BookmarkTreeNode node : nodes) model.insertNodeInto(node, target, insertionIndex++);
        for (BookmarkTreeNode parent : parents) {
            for (int i = 0; i < parent.getChildCount(); i++) {
                BookmarkTreeNode child = (BookmarkTreeNode) parent.getChildAt(i);
                if (child.isBookmark()) ((BookmarkNodeModel) child.getUserObject()).setIndex(i);
                model.nodeChanged(child);
            }
        }
        return true;
    }
}
