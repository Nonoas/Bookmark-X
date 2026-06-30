package indi.bookmarkx.service.dto;

import java.util.ArrayList;
import java.util.List;

public class BookmarkDraft {

    private String uuid;
    private String name;
    private String desc;
    private String filePath;
    /**
     * 1-based line number exposed to external callers.
     */
    private Integer line;
    private List<String> groupPath = new ArrayList<>();

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

    public List<String> getGroupPath() {
        return groupPath;
    }

    public void setGroupPath(List<String> groupPath) {
        this.groupPath = groupPath == null ? new ArrayList<>() : new ArrayList<>(groupPath);
    }
}
