package indi.worktool.model;

/**
 * @author Nonoas
 * @date 2023/6/6
 */
public abstract class AbstractTreeNodeModel {

    public abstract boolean isBookmark();

    public boolean isGroup() {
        return !isBookmark();
    }

    public abstract String getName();

    @Override
    public String toString() {
        return getName();
    }
}
