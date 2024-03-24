package indi.bookmarkx.ui.tree;

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

    /**
     * 统计当前节点下，非分组节点的「书签节点」数，如果当前节点为书签节点，直接返回 0
     *
     * @return 当前节点下，非分组节点的「书签节点」数
     */
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

    public int firstChildIndex() {
        if (isBookmark()) {
            return -1;
        }
        int childCount = getChildCount();
        BookmarkTreeNode node;
        for (int i = 0; i < childCount; i++) {
            node = (BookmarkTreeNode) getChildAt(i);
            if (node.isBookmark()) {
                return i;
            }
        }
        return -1;
    }

}
