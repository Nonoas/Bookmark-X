package indi.bookmarkx.service.dto;

import java.util.ArrayList;
import java.util.List;

public class BookmarkTreeItemView {

    private String type;
    private String uuid;
    private String name;
    private String desc;
    private String filePath;
    private Integer line;
    private Integer indexInGroup;
    private List<String> groupPath = new ArrayList<>();
    private List<BookmarkTreeItemView> children = new ArrayList<>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Integer getLine() {
        return line;
    }

    public void setLine(Integer line) {
        this.line = line;
    }

    public Integer getIndexInGroup() {
        return indexInGroup;
    }

    public void setIndexInGroup(Integer indexInGroup) {
        this.indexInGroup = indexInGroup;
    }

    public List<String> getGroupPath() {
        return groupPath;
    }

    public void setGroupPath(List<String> groupPath) {
        this.groupPath = groupPath == null ? new ArrayList<>() : new ArrayList<>(groupPath);
    }

    public List<BookmarkTreeItemView> getChildren() {
        return children;
    }

    public void setChildren(List<BookmarkTreeItemView> children) {
        this.children = children == null ? new ArrayList<>() : new ArrayList<>(children);
    }
}
