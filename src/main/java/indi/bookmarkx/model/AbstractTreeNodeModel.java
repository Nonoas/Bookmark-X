package indi.bookmarkx.model;

/**
 * @author Nonoas
 * @date 2023/6/6
 */
public abstract class AbstractTreeNodeModel {

    protected String desc;

    protected String name;

    public abstract boolean isBookmark();

    public final boolean isGroup() {
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
