package indi.bookmarkx.service.branch;

import java.io.File;
import java.util.Collections;
import java.util.List;

public final class GitBranchContext {

    private final List<RootBranch> roots;
    private final String branchKey;
    private final String displayName;

    GitBranchContext(List<RootBranch> roots, String branchKey, String displayName) {
        this.roots = Collections.unmodifiableList(roots);
        this.branchKey = branchKey;
        this.displayName = displayName;
    }

    public List<RootBranch> getRoots() {
        return roots;
    }

    public String getBranchKey() {
        return branchKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static final class RootBranch {
        private final File root;
        private final File gitDir;
        private final String branchName;
        private final String headIdentity;
        private final boolean detached;

        RootBranch(File root, File gitDir, String branchName, String headIdentity, boolean detached) {
            this.root = root;
            this.gitDir = gitDir;
            this.branchName = branchName;
            this.headIdentity = headIdentity;
            this.detached = detached;
        }

        public File getRoot() {
            return root;
        }

        public File getGitDir() {
            return gitDir;
        }

        public String getBranchName() {
            return branchName;
        }

        public String getHeadIdentity() {
            return headIdentity;
        }

        public boolean isDetached() {
            return detached;
        }

        String getKeyIdentity() {
            return detached ? "detached:" + headIdentity : "branch:" + branchName;
        }

        String getDisplayName() {
            if (!detached) {
                return branchName;
            }
            int end = Math.min(headIdentity.length(), 12);
            return "detached:" + headIdentity.substring(0, end);
        }
    }
}
