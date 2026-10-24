package indi.bookmarkx.service.branch;

import indi.bookmarkx.model.po.BookmarkPO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BranchBookmarkSnapshot {

    private int version;
    private String branchKey;
    private String branchDisplayName;
    private long createdAt;
    private List<RootSnapshot> roots;
    private BookmarkPO root;

    public BranchBookmarkSnapshot() {
    }

    BranchBookmarkSnapshot(int version,
                           String branchKey,
                           String branchDisplayName,
                           long createdAt,
                           List<RootSnapshot> roots,
                           BookmarkPO root) {
        this.version = version;
        this.branchKey = branchKey;
        this.branchDisplayName = branchDisplayName;
        this.createdAt = createdAt;
        this.roots = roots == null ? null : new ArrayList<>(roots);
        this.root = root;
    }

    static List<RootSnapshot> rootsFrom(GitBranchContext context) {
        if (context == null || context.getRoots().isEmpty()) {
            return Collections.emptyList();
        }
        List<RootSnapshot> snapshots = new ArrayList<>();
        for (GitBranchContext.RootBranch branch : context.getRoots()) {
            snapshots.add(new RootSnapshot(
                    branch.getRoot().getPath().replace('\\', '/'),
                    branch.getBranchName(),
                    branch.getKeyIdentity(),
                    branch.isDetached()
            ));
        }
        return snapshots;
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

    public List<RootSnapshot> getRoots() {
        return roots == null ? Collections.emptyList() : Collections.unmodifiableList(roots);
    }

    public BookmarkPO getRoot() {
        return root;
    }

    public static final class RootSnapshot {
        private String rootPath;
        private String branchName;
        private String keyIdentity;
        private boolean detached;

        public RootSnapshot() {
        }

        RootSnapshot(String rootPath, String branchName, String keyIdentity, boolean detached) {
            this.rootPath = rootPath;
            this.branchName = branchName;
            this.keyIdentity = keyIdentity;
            this.detached = detached;
        }

        public String getRootPath() {
            return rootPath;
        }

        public String getBranchName() {
            return branchName;
        }

        public String getKeyIdentity() {
            return keyIdentity;
        }

        public boolean isDetached() {
            return detached;
        }
    }
}
