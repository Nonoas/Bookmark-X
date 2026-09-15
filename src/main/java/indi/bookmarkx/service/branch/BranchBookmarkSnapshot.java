package indi.bookmarkx.service.branch;

import indi.bookmarkx.model.po.BookmarkPO;

public final class BranchBookmarkSnapshot {

    private int version;
    private String branchKey;
    private String branchDisplayName;
    private long createdAt;
    private BookmarkPO root;

    public BranchBookmarkSnapshot() {
    }

    BranchBookmarkSnapshot(int version, String branchKey, String branchDisplayName, long createdAt, BookmarkPO root) {
        this.version = version;
        this.branchKey = branchKey;
        this.branchDisplayName = branchDisplayName;
        this.createdAt = createdAt;
        this.root = root;
    }

    public int getVersion() {
        return version;
    }

    public String getBranchKey() {
        return branchKey;
    }

    public String getBranchDisplayName() {
        return branchDisplayName;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public BookmarkPO getRoot() {
        return root;
    }
}
