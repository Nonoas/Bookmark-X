package indi.bookmarkx.service.relocation;

import com.intellij.testFramework.LightVirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookmarkSnapshotStoreTest {

    @TempDir
    Path directory;

    @Test
    void preservesPreviousContentWhenAnotherBookmarkRefreshesTheFile() {
        BookmarkSnapshotStore store = store("IntelliJIdea2026.2");
        LightVirtualFile file = new LightVirtualFile("Example.java");
        BookmarkSnapshotStore.Snapshot first = store.writeSnapshot(file, "before\ntarget();").orElseThrow();
        BookmarkSnapshotStore.Snapshot second = store.writeSnapshot(file, "inserted\nbefore\ntarget();").orElseThrow();

        assertNotEquals(first.getId(), second.getId());
        assertEquals("before\ntarget();", store.readSnapshot(first.getId(), first.getContentHash()).orElseThrow());
        assertEquals("inserted\nbefore\ntarget();", store.readSnapshot(second.getId(), second.getContentHash()).orElseThrow());
    }

    @Test
    void sharesSnapshotsAcrossIdeVersions() {
        BookmarkSnapshotStore.Snapshot snapshot = store("IntelliJIdea2026.2")
                .writeSnapshot(new LightVirtualFile("Example.java"), "target();").orElseThrow();
        assertEquals("target();", store("IntelliJIdea2026.3")
                .readSnapshot(snapshot.getId(), snapshot.getContentHash()).orElseThrow());
    }

    @Test
    void readsLegacySnapshotFromPreviousIdeAndRejectsOverwrittenContent() throws Exception {
        String id = TextLineUtil.sha256("legacy-snapshot");
        Path path = directory.resolve("IntelliJIdea2026.2/bookmark-x/snapshots/project/" + id + ".txt.gz");
        Files.createDirectories(path.getParent());
        try (GZIPOutputStream out = new GZIPOutputStream(Files.newOutputStream(path))) {
            out.write("old source".getBytes(StandardCharsets.UTF_8));
        }
        BookmarkSnapshotStore store = store("IntelliJIdea2026.3");
        assertEquals("old source", store.readSnapshot(id, TextLineUtil.hashText("old source")).orElseThrow());
        assertTrue(store.readSnapshot(id, TextLineUtil.hashText("other source")).isEmpty());
    }

    @Test
    void missingOrInvalidSnapshotCanBeReportedWithoutThrowing() {
        BookmarkSnapshotStore store = store("IntelliJIdea2026.3");
        assertTrue(store.readSnapshot(TextLineUtil.sha256("missing")).isEmpty());
        assertTrue(store.readSnapshot(null).isEmpty());
        assertTrue(store.readSnapshot("../outside").isEmpty());
    }

    private BookmarkSnapshotStore store(String ideVersion) {
        return new BookmarkSnapshotStore(directory.resolve(ideVersion).toFile(), "project");
    }
}
