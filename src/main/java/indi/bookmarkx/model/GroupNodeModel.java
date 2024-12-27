package indi.bookmarkx.model;

/**
 * @author Nonoas
 * @date 2023/6/6
 */
public class GroupNodeModel extends AbstractTreeNodeModel {

    public GroupNodeModel() {

    }

    public GroupNodeModel(String name) {
        this.name = name;
    }

    @Override
    public boolean isBookmark() {
        return false;
    }
}
