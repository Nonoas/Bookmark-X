package indi.bookmarkx.model;

import java.util.Objects;

/**
 * @author Nonoas
 * @date 2023/6/6
 */
public abstract class AbstractTreeNodeModel {

    private String uuid;

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

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractTreeNodeModel yourClass = (AbstractTreeNodeModel) o;
        return uuid.equals(yourClass.uuid);
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }
}
