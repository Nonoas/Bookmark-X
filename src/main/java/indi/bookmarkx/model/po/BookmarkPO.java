package indi.bookmarkx.model.po;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 书签持久化对象<br/>
 * bookmark：true 为书签<br/>
 * bookmark：false 为书签分组 <br/><br/>
 * <p>需要注意，一些「书签」使用的属性，和「书签分组」</p>
 *
 * @author Nonoas
 * @date 2023/6/5
 */
@XmlRootElement
public class BookmarkPO {

    private String uuid;

    private int index;
    private int line;

    private String desc;

    private String name;

    private String iconPath;

    private boolean bookmark;

    private String anchorLineText;

    private String anchorLineHash;

    private String beforeContextHash;

    private String afterContextHash;

    private String snapshotId;

    private String baseContentHash;

    private String gitRootPath;

    private String gitRelativePath;

    private String gitCommit;

    /**
     * 虚拟文件路径
     */
    private String virtualFilePath;

    private List<BookmarkPO> children = new ArrayList<>();

    public BookmarkPO() {

    }

    public BookmarkPO(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }


    public List<BookmarkPO> getChildren() {
        List<BookmarkPO> list = children.stream().distinct().toList();
        children.clear();
        children.addAll(list);
        return children;
    }

    public void setChildren(List<BookmarkPO> children) {
        if (bookmark) {
            throw new UnsupportedOperationException("Bookmarks do not support adding child nodes");
        }
        this.children = children;
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

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getIconPath() {
        return iconPath;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }

    public String getVirtualFilePath() {
        return virtualFilePath;
    }

    public void setVirtualFilePath(String virtualFilePath) {
        this.virtualFilePath = virtualFilePath;
    }

    public boolean isBookmark() {
        return bookmark;
    }

    public void setBookmark(boolean bookmark) {
        this.bookmark = bookmark;
    }

    public String getAnchorLineText() {
        return anchorLineText;
    }

    public void setAnchorLineText(String anchorLineText) {
        this.anchorLineText = anchorLineText;
    }

    public String getAnchorLineHash() {
        return anchorLineHash;
    }

    public void setAnchorLineHash(String anchorLineHash) {
        this.anchorLineHash = anchorLineHash;
    }

    public String getBeforeContextHash() {
        return beforeContextHash;
    }

    public void setBeforeContextHash(String beforeContextHash) {
        this.beforeContextHash = beforeContextHash;
    }

    public String getAfterContextHash() {
        return afterContextHash;
    }

    public void setAfterContextHash(String afterContextHash) {
        this.afterContextHash = afterContextHash;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getBaseContentHash() {
        return baseContentHash;
    }

    public void setBaseContentHash(String baseContentHash) {
        this.baseContentHash = baseContentHash;
    }

    public String getGitRootPath() {
        return gitRootPath;
    }

    public void setGitRootPath(String gitRootPath) {
        this.gitRootPath = gitRootPath;
    }

    public String getGitRelativePath() {
        return gitRelativePath;
    }

    public void setGitRelativePath(String gitRelativePath) {
        this.gitRelativePath = gitRelativePath;
    }

    public String getGitCommit() {
        return gitCommit;
    }

    public void setGitCommit(String gitCommit) {
        this.gitCommit = gitCommit;
    }

    @Override
    public String toString() {
        return name;
    }
}
