package indi.bookmarkx.tree;

import indi.bookmarkx.model.AbstractTreeNodeModel;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * @author Nonoas
 * @date 2023/6/1
 */
public class BookmarkTreeNode extends DefaultMutableTreeNode {

    private boolean isGroup;

    public BookmarkTreeNode() {

    }

    public BookmarkTreeNode(AbstractTreeNodeModel nodeModel) {
        super(nodeModel);
        setGroup(nodeModel.isGroup());
    }

    public BookmarkTreeNode(boolean isGroup) {
        super();
        setGroup(isGroup);
    }

    public void setGroup(boolean group) {
        isGroup = group;
        setAllowsChildren(isGroup);
    }

    public boolean isGroup() {
        return isGroup;
    }

    public boolean isBookmark() {
        return !isGroup;
    }

    public int getBookmarkChildCount() {
        if (!isGroup) {
            return 0;
        }
        int count = getChildCount();
        int bookmarkCount = 0;

        BookmarkTreeNode node;
        for (int i = 0; i < count; i++) {
            node = (BookmarkTreeNode) getChildAt(i);
            if (node.isBookmark()) {
                bookmarkCount++;
            }
        }
        return bookmarkCount;
    }

}
