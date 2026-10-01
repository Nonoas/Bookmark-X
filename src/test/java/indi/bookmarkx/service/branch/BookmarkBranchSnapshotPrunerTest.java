package indi.bookmarkx.service.branch;

import indi.bookmarkx.model.po.BookmarkPO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookmarkBranchSnapshotPrunerTest {

    @TempDir
    Path tempDir;

    @Test
    void deletesSnapshotWhenBranchNoLongerExists() throws Exception {
        Path root = createRepo("main");
        GitBranchContext main = GitBranchDetector.detect(root.toFile()).orElseThrow();
        Files.writeString(root.resolve(".git/HEAD"), "ref: refs/heads/feature\n", StandardCharsets.UTF_8);
        GitBranchContext feature = GitBranchDetector.detect(root.toFile()).orElseThrow();
        BookmarkBranchSnapshotStore store = new BookmarkBranchSnapshotStore(tempDir.resolve("snapshots").toFile(), "project");
        store.write(main, createBookmarkRoot());
        store.write(feature, createBookmarkRoot());
        BookmarkBranchSnapshotPruner pruner = new BookmarkBranchSnapshotPruner(store,
                ignored -> Optional.of(List.of("main")));

        int deleted = pruner.pruneDeletedBranches();

        assertEquals(1, deleted);
        assertTrue(store.exists(main.getBranchKey()));
        assertFalse(store.exists(feature.getBranchKey()));
    }

    @Test
    void keepsDetachedSnapshot() throws Exception {
        Path root = createRepo("main");
        Files.writeString(root.resolve(".git/HEAD"),
                "0123456789abcdef0123456789abcdef01234567\n",
                StandardCharsets.UTF_8);
        GitBranchContext detached = GitBranchDetector.detect(root.toFile()).orElseThrow();
        BookmarkBranchSnapshotStore store = new BookmarkBranchSnapshotStore(tempDir.resolve("snapshots").toFile(), "project");
        store.write(detached, createBookmarkRoot());
        BookmarkBranchSnapshotPruner pruner = new BookmarkBranchSnapshotPruner(store,
                ignored -> Optional.of(List.of()));

        int deleted = pruner.pruneDeletedBranches();

        assertEquals(0, deleted);
        assertTrue(store.exists(detached.getBranchKey()));
    }

    @Test
    void keepsLegacySnapshotWithoutRootMetadata() {
        BookmarkBranchSnapshotStore store = new BookmarkBranchSnapshotStore(tempDir.resolve("snapshots").toFile(), "project");
        store.write("legacy", "legacy", createBookmarkRoot());
        BookmarkBranchSnapshotPruner pruner = new BookmarkBranchSnapshotPruner(store,
                ignored -> Optional.of(List.of()));

        int deleted = pruner.pruneDeletedBranches();

        assertEquals(0, deleted);
        assertTrue(store.exists("legacy"));
    }

    private Path createRepo(String branch) throws Exception {
        Path root = tempDir.resolve("repo");
        Files.createDirectories(root.resolve(".git"));
        Files.writeString(root.resolve(".git/HEAD"), "ref: refs/heads/" + branch + "\n", StandardCharsets.UTF_8);
        return root;
    }

    private BookmarkPO createBookmarkRoot() {
        BookmarkPO root = new BookmarkPO("root", "");
        root.setBookmark(false);
        return root;
    }
}
