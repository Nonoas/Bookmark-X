package indi.bookmarkx.service.branch;

import com.google.gson.Gson;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import indi.bookmarkx.model.po.BookmarkPO;
import indi.bookmarkx.utils.PersistenceUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Service(Service.Level.PROJECT)
public final class BookmarkBranchSnapshotStore {

    private static final int VERSION = 1;
    private static final Gson GSON = new Gson();

    private final File baseDir;
    private final String projectHash;

    public BookmarkBranchSnapshotStore(Project project) {
        this(new File(PathManager.getSystemPath(), "bookmark-x/branch-snapshots"), project.getLocationHash());
    }

    BookmarkBranchSnapshotStore(File baseDir, String projectHash) {
        this.baseDir = baseDir;
        this.projectHash = projectHash;
    }

    public static BookmarkBranchSnapshotStore getInstance(Project project) {
        return project.getService(BookmarkBranchSnapshotStore.class);
    }

    public boolean exists(String branchKey) {
        return snapshotFile(branchKey).isFile();
    }

    public Optional<BranchBookmarkSnapshot> read(String branchKey) {
        if (StringUtils.isBlank(branchKey)) {
            return Optional.empty();
        }
        File snapshotFile = snapshotFile(branchKey);
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
            String json = out.toString(StandardCharsets.UTF_8);
            BranchBookmarkSnapshot snapshot = GSON.fromJson(json, BranchBookmarkSnapshot.class);
            if (snapshot == null || snapshot.getRoot() == null) {
                return Optional.empty();
            }
            return Optional.of(snapshot);
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    public Optional<BranchBookmarkSnapshot> write(GitBranchContext context, BookmarkPO root) {
        if (context == null) {
            return Optional.empty();
        }
        return write(context.getBranchKey(), context.getDisplayName(), root);
    }

    Optional<BranchBookmarkSnapshot> write(String branchKey, String branchDisplayName, BookmarkPO root) {
        if (StringUtils.isBlank(branchKey) || root == null) {
            return Optional.empty();
        }
        BookmarkPO copy = PersistenceUtil.deepCopy(root, BookmarkPO.class);
        BranchBookmarkSnapshot snapshot = new BranchBookmarkSnapshot(
                VERSION,
                branchKey,
                branchDisplayName,
                System.currentTimeMillis(),
                copy
        );
        File snapshotFile = snapshotFile(branchKey);
        try {
            FileUtil.createParentDirs(snapshotFile);
            try (GZIPOutputStream out = new GZIPOutputStream(Files.newOutputStream(snapshotFile.toPath()))) {
                out.write(GSON.toJson(snapshot).getBytes(StandardCharsets.UTF_8));
            }
            return Optional.of(snapshot);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    File snapshotFile(String branchKey) {
        return new File(new File(baseDir, projectHash), branchKey + ".json.gz");
    }
}
