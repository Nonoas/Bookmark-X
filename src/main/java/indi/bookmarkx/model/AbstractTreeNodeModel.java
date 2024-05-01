package indi.bookmarkx.model;

/**
 * @author Nonoas
 * @date 2023/6/6
 */
public abstract class AbstractTreeNodeModel {

    private String desc;

    private String name;

    public abstract boolean isBookmark();

    public boolean isGroup() {
        return !isBookmark();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public String toString() {
        return getName();
    }

}
