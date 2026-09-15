package indi.bookmarkx.service.branch;

import indi.bookmarkx.model.po.BookmarkPO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookmarkBranchSnapshotStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void createsDifferentSnapshotFilesForDifferentBranches() throws Exception {
        Path root = createRepo("main");
        GitBranchContext main = GitBranchDetector.detect(root.toFile()).orElseThrow();
        Files.writeString(root.resolve(".git/HEAD"), "ref: refs/heads/feature\n", StandardCharsets.UTF_8);
        GitBranchContext feature = GitBranchDetector.detect(root.toFile()).orElseThrow();
        BookmarkBranchSnapshotStore store = new BookmarkBranchSnapshotStore(tempDir.resolve("snapshots").toFile(), "project");

        assertNotEquals(main.getBranchKey(), feature.getBranchKey());
        assertNotEquals(store.snapshotFile(main.getBranchKey()), store.snapshotFile(feature.getBranchKey()));
    }

    @Test
    void writesAndReadsSnapshot() throws Exception {
        Path root = createRepo("main");
        GitBranchContext context = GitBranchDetector.detect(root.toFile()).orElseThrow();
        BookmarkBranchSnapshotStore store = new BookmarkBranchSnapshotStore(tempDir.resolve("snapshots").toFile(), "project");
        BookmarkPO bookmarkRoot = createBookmarkRoot();

        store.write(context, bookmarkRoot);
        BranchBookmarkSnapshot snapshot = store.read(context.getBranchKey()).orElseThrow();

        assertTrue(store.exists(context.getBranchKey()));
        assertEquals(1, snapshot.getVersion());
        assertEquals(context.getBranchKey(), snapshot.getBranchKey());
        assertEquals("main", snapshot.getBranchDisplayName());
        assertEquals("root", snapshot.getRoot().getName());
        assertEquals("Solution.java", snapshot.getRoot().getChildren().get(0).getName());
        assertEquals(14, snapshot.getRoot().getChildren().get(0).getLine());
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
        BookmarkPO child = new BookmarkPO("Solution.java", "test");
        child.setBookmark(true);
        child.setLine(14);
        child.setVirtualFilePath("D:/MyCode/Solution.java");
        root.setChildren(new ArrayList<>(List.of(child)));
        return root;
    }
}
