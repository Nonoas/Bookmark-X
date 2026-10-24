package indi.bookmarkx.service.relocation;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Service(Service.Level.PROJECT)
public final class BookmarkSnapshotStore {

    private static final int MAX_SNAPSHOT_CHARS = 1_000_000;

    private final Project project;

    public BookmarkSnapshotStore(Project project) {
        this.project = project;
    }

    public static BookmarkSnapshotStore getInstance(Project project) {
        return project.getService(BookmarkSnapshotStore.class);
    }

    public Optional<Snapshot> writeSnapshot(VirtualFile file, String text) {
        if (file == null || text == null || text.length() > MAX_SNAPSHOT_CHARS) {
            return Optional.empty();
        }
        try {
            String snapshotId = TextLineUtil.sha256(project.getLocationHash() + "\n" + file.getPath());
            File snapshotFile = snapshotFile(snapshotId);
            FileUtil.createParentDirs(snapshotFile);
            try (GZIPOutputStream out = new GZIPOutputStream(Files.newOutputStream(snapshotFile.toPath()))) {
                out.write(text.getBytes(StandardCharsets.UTF_8));
            }
            return Optional.of(new Snapshot(snapshotId, TextLineUtil.hashText(text)));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public Optional<String> readSnapshot(String snapshotId) {
        if (snapshotId == null || snapshotId.trim().isEmpty()) {
            return Optional.empty();
        }
        File snapshotFile = snapshotFile(snapshotId);
        if (!snapshotFile.isFile()) {
            return Optional.empty();
        }
        try (GZIPInputStream in = new GZIPInputStream(Files.newInputStream(snapshotFile.toPath()));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
            return Optional.of(out.toString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private File snapshotFile(String snapshotId) {
        return new File(PathManager.getSystemPath(), "bookmark-x/snapshots/" + project.getLocationHash() + "/" + snapshotId + ".txt.gz");
    }

    public static final class Snapshot {
        private final String id;
        private final String contentHash;

        private Snapshot(String id, String contentHash) {
            this.id = id;
            this.contentHash = contentHash;
        }

        public String getId() {
            return id;
        }

        public String getContentHash() {
            return contentHash;
        }
    }
}
