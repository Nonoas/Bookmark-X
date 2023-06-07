package indi.bookmarkx.model;

/**
 * @author Nonoas
 * @date 2023/6/6
 */
public class GroupNodeModel extends AbstractTreeNodeModel {

    private String name;

    public GroupNodeModel() {

    }

    public GroupNodeModel(String name) {
        this.name = name;
    }

    @Override
    public boolean isBookmark() {
        return false;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
