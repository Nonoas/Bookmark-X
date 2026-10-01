package indi.bookmarkx.service.branch;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class BookmarkBranchSnapshotPruner {

    private final BookmarkBranchSnapshotStore snapshotStore;
    private final BranchListProvider branchListProvider;

    BookmarkBranchSnapshotPruner(BookmarkBranchSnapshotStore snapshotStore) {
        this(snapshotStore, GitBranchDetector::listLocalBranches);
    }

    BookmarkBranchSnapshotPruner(BookmarkBranchSnapshotStore snapshotStore, BranchListProvider branchListProvider) {
        this.snapshotStore = snapshotStore;
        this.branchListProvider = branchListProvider;
    }

    int pruneDeletedBranches() {
        int deleted = 0;
        Map<String, Optional<List<String>>> branchCache = new HashMap<>();
        for (BranchBookmarkSnapshot snapshot : snapshotStore.listSnapshots()) {
            if (shouldDelete(snapshot, branchCache) && snapshotStore.delete(snapshot.getBranchKey())) {
                deleted++;
            }
        }
        return deleted;
    }

    private boolean shouldDelete(BranchBookmarkSnapshot snapshot, Map<String, Optional<List<String>>> branchCache) {
        if (snapshot == null || snapshot.getRoots().isEmpty()) {
            return false;
        }
        for (BranchBookmarkSnapshot.RootSnapshot root : snapshot.getRoots()) {
            if (root == null || root.isDetached()) {
                continue;
            }
            if (StringUtils.isBlank(root.getRootPath()) || StringUtils.isBlank(root.getBranchName())) {
                continue;
            }
            String rootPath = root.getRootPath();
            Optional<List<String>> branches = branchCache.computeIfAbsent(rootPath,
                    ignored -> branchListProvider.listLocalBranches(new File(rootPath)));
            if (branches.isPresent() && !branches.get().contains(root.getBranchName())) {
                return true;
            }
        }
        return false;
    }

    interface BranchListProvider {
        Optional<List<String>> listLocalBranches(File root);
    }
}
