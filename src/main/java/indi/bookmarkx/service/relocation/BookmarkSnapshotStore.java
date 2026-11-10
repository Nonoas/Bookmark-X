package indi.bookmarkx.service.relocation;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Service(Service.Level.PROJECT)
public final class BookmarkSnapshotStore {

    private static final int MAX_SNAPSHOT_CHARS = 1_000_000;

    private final File systemDirectory;
    private final String projectLocationHash;

    public BookmarkSnapshotStore(Project project) {
        this(new File(PathManager.getSystemPath()), project.getLocationHash());
    }

    BookmarkSnapshotStore(File systemDirectory, String projectLocationHash) {
        this.systemDirectory = systemDirectory;
        this.projectLocationHash = projectLocationHash;
    }

    public static BookmarkSnapshotStore getInstance(Project project) {
        return project.getService(BookmarkSnapshotStore.class);
    }

    public Optional<Snapshot> writeSnapshot(VirtualFile file, String text) {
        if (file == null || text == null || text.length() > MAX_SNAPSHOT_CHARS) {
            return Optional.empty();
        }
        String contentHash = TextLineUtil.hashText(text);
        String snapshotId = TextLineUtil.sha256(projectLocationHash + "\n" + file.getPath() + "\n" + contentHash);
        File snapshotFile = snapshotFile(sharedDirectory(), snapshotId);
        Path temporaryFile = null;
        try {
            FileUtil.createParentDirs(snapshotFile);
            temporaryFile = Files.createTempFile(snapshotFile.getParentFile().toPath(), "snapshot-", ".tmp");
            try (GZIPOutputStream out = new GZIPOutputStream(Files.newOutputStream(temporaryFile))) {
                out.write(text.getBytes(StandardCharsets.UTF_8));
            }
            try {
                Files.move(temporaryFile, snapshotFile.toPath(),
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporaryFile, snapshotFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return Optional.of(new Snapshot(snapshotId, contentHash));
        } catch (IOException e) {
            return Optional.empty();
        } finally {
            if (temporaryFile != null) {
                FileUtil.delete(temporaryFile.toFile());
            }
        }
    }

    public Optional<String> readSnapshot(String snapshotId) {
        return readSnapshot(snapshotId, null);
    }

    public Optional<String> readSnapshot(String snapshotId, String expectedContentHash) {
        if (snapshotId == null || !snapshotId.matches("[a-f0-9]{64}")) {
            return Optional.empty();
        }
        Optional<String> shared = readSnapshotFile(snapshotFile(sharedDirectory(), snapshotId), expectedContentHash);
        if (shared.isPresent()) {
            return shared;
        }
        Optional<String> current = readSnapshotFile(snapshotFile(systemDirectory, snapshotId), expectedContentHash);
        if (current.isPresent()) {
            return current;
        }
        // Older releases stored snapshots inside the IDE's version-specific cache.
        File parent = systemDirectory.getParentFile();
        File[] previousIdeDirectories = parent == null ? null : parent.listFiles(File::isDirectory);
        if (previousIdeDirectories != null) {
            for (File directory : previousIdeDirectories) {
                Optional<String> previous = readSnapshotFile(snapshotFile(directory, snapshotId), expectedContentHash);
                if (previous.isPresent()) {
                    return previous;
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> readSnapshotFile(File file, String expectedContentHash) {
        if (!file.isFile()) {
            return Optional.empty();
        }
        try (GZIPInputStream in = new GZIPInputStream(Files.newInputStream(file.toPath()))) {
            byte[] bytes = in.readNBytes(MAX_SNAPSHOT_CHARS * 4 + 1);
            if (bytes.length > MAX_SNAPSHOT_CHARS * 4) {
                return Optional.empty();
            }
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (text.length() > MAX_SNAPSHOT_CHARS
                    || (expectedContentHash != null && !expectedContentHash.isBlank()
                    && !expectedContentHash.equals(TextLineUtil.hashText(text)))) {
                return Optional.empty();
            }
            return Optional.of(text);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private File sharedDirectory() {
        return systemDirectory.getParentFile() == null ? systemDirectory : systemDirectory.getParentFile();
    }

    private File snapshotFile(File directory, String snapshotId) {
        return new File(directory, "bookmark-x/snapshots/" + projectLocationHash + "/" + snapshotId + ".txt.gz");
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
