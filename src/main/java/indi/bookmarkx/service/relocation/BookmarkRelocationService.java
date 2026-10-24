package indi.bookmarkx.service.relocation;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowId;
import indi.bookmarkx.BookmarksManager;
import indi.bookmarkx.common.I18N;
import indi.bookmarkx.common.data.BookmarkArrayListTable;
import indi.bookmarkx.listener.BookmarkListener;
import indi.bookmarkx.model.BookmarkNodeModel;
import indi.bookmarkx.utils.CollectionUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service(Service.Level.PROJECT)
public final class BookmarkRelocationService {

    private static final Logger LOG = Logger.getInstance(BookmarkRelocationService.class);
    private static final int CONTEXT_RADIUS = 3;
    private static final double ACCEPT_CONFIDENCE = 0.6;

    private final Project project;
    private final List<BookmarkRelocationStrategy> strategies;

    public BookmarkRelocationService(Project project) {
        this.project = project;
        this.strategies = Arrays.asList(new GitDiffRelocationStrategy(), new SnapshotRelocationStrategy());
    }

    public static BookmarkRelocationService getInstance(Project project) {
        return project.getService(BookmarkRelocationService.class);
    }

    public void refreshFileAnchorsAsync(VirtualFile file, List<BookmarkNodeModel> bookmarks) {
        if (file == null || CollectionUtil.isEmpty(bookmarks) || !isTextFile(file)) {
            return;
        }
        List<BookmarkNodeModel> bookmarkCopy = new ArrayList<>(bookmarks);
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Refreshing Bookmark Anchors", false) {
            private List<AnchorUpdate> updates = List.of();

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                updates = computeAnchorUpdates(file, bookmarkCopy);
            }

            @Override
            public void onSuccess() {
                if (updates.isEmpty()) {
                    return;
                }
                applyAnchorUpdates(updates);
                BookmarksManager.getInstance(project).persistentSave();
            }
        });
    }

    public void refreshBookmarkAnchorAsync(BookmarkNodeModel bookmark) {
        if (bookmark == null || bookmark.getOpenFileDescriptor() == null) {
            return;
        }
        VirtualFile file = bookmark.getOpenFileDescriptor().getFile();
        refreshFileAnchorsAsync(file, BookmarkArrayListTable.getInstance(project).findByFilePath(file.getPath()));
    }

    public void checkBookmarksAsync() {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Checking Bookmark Locations", false) {
            private List<RelocationPlan> plans = List.of();

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                plans = computeRelocationPlans(BookmarkArrayListTable.getInstance(project).listAll());
            }

            @Override
            public void onSuccess() {
                if (!plans.isEmpty()) {
                    notifyRelocationAvailable(plans.size());
                }
            }
        });
    }

    public void relocateBookmarksAsync() {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Relocating Bookmarks", true) {
            private List<RelocationPlan> plans = List.of();

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                plans = computeRelocationPlans(BookmarkArrayListTable.getInstance(project).listAll());
            }

            @Override
            public void onSuccess() {
                if (plans.isEmpty()) {
                    notifyNoRelocationNeeded();
                    return;
                }
                applyRelocationPlans(plans);
                refreshAnchorsAfterRelocation(plans);
            }
        });
    }

    private List<RelocationPlan> computeRelocationPlans(List<BookmarkNodeModel> bookmarks) {
        if (CollectionUtil.isEmpty(bookmarks)) {
            return List.of();
        }
        List<RelocationPlan> plans = new ArrayList<>();
        for (Map.Entry<VirtualFile, List<BookmarkNodeModel>> entry : groupByFile(bookmarks).entrySet()) {
            VirtualFile file = entry.getKey();
            if (!isTextFile(file)) {
                continue;
            }
            try {
                String currentText = readTextInReadAction(file);
                List<String> currentLines = TextLineUtil.splitLines(currentText);
                for (BookmarkNodeModel bookmark : entry.getValue()) {
                    findRelocationPlan(file, bookmark, currentLines).ifPresent(plans::add);
                }
            } catch (Exception e) {
                LOG.debug("Failed to check bookmark relocation", e);
            }
        }
        return plans;
    }

    private Optional<RelocationPlan> findRelocationPlan(VirtualFile file,
                                                        BookmarkNodeModel bookmark,
                                                        List<String> currentLines) {
        Optional<String> baseText = BookmarkSnapshotStore.getInstance(project).readSnapshot(bookmark.getSnapshotId());
        if (baseText.isEmpty()) {
            return Optional.empty();
        }
        RelocationContext context = new RelocationContext(
                project,
                file,
                bookmark,
                TextLineUtil.splitLines(baseText.get()),
                currentLines
        );
        return strategies.stream()
                .map(strategy -> strategy.relocate(context))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(it -> it.getConfidence() >= ACCEPT_CONFIDENCE)
                .filter(it -> it.getLine() != bookmark.getLine())
                .findFirst()
                .map(result -> new RelocationPlan(bookmark, file, result.getLine()));
    }

    private List<AnchorUpdate> computeAnchorUpdates(VirtualFile file, List<BookmarkNodeModel> bookmarks) {
        if (file == null || CollectionUtil.isEmpty(bookmarks) || !isTextFile(file)) {
            return List.of();
        }
        try {
            String text = readTextInReadAction(file);
            List<String> lines = TextLineUtil.splitLines(text);
            Optional<BookmarkSnapshotStore.Snapshot> snapshot =
                    BookmarkSnapshotStore.getInstance(project).writeSnapshot(file, text);
            Optional<GitDiffRelocationStrategy.GitInfo> gitInfo = inspectGit(file);
            List<AnchorUpdate> updates = new ArrayList<>();
            for (BookmarkNodeModel bookmark : bookmarks) {
                createAnchorUpdate(bookmark, lines, snapshot, gitInfo).ifPresent(updates::add);
            }
            return updates;
        } catch (Exception e) {
            LOG.debug("Failed to refresh bookmark anchors", e);
            return List.of();
        }
    }

    private Optional<AnchorUpdate> createAnchorUpdate(BookmarkNodeModel bookmark,
                                                     List<String> lines,
                                                     Optional<BookmarkSnapshotStore.Snapshot> snapshot,
                                                     Optional<GitDiffRelocationStrategy.GitInfo> gitInfo) {
        if (bookmark == null || CollectionUtil.isEmpty(lines)) {
            return Optional.empty();
        }
        int line = Math.max(0, Math.min(bookmark.getLine(), lines.size() - 1));
        return Optional.of(new AnchorUpdate(
                bookmark,
                lines.get(line),
                TextLineUtil.hashLine(lines.get(line)),
                TextLineUtil.hashContext(lines, line - CONTEXT_RADIUS, line),
                TextLineUtil.hashContext(lines, line + 1, line + 1 + CONTEXT_RADIUS),
                snapshot.map(BookmarkSnapshotStore.Snapshot::getId).orElse(bookmark.getSnapshotId()),
                snapshot.map(BookmarkSnapshotStore.Snapshot::getContentHash).orElse(bookmark.getBaseContentHash()),
                gitInfo.map(GitDiffRelocationStrategy.GitInfo::getRootPath).orElse(null),
                gitInfo.map(GitDiffRelocationStrategy.GitInfo::getRelativePath).orElse(null),
                gitInfo.map(GitDiffRelocationStrategy.GitInfo::getCommit).orElse(null)
        ));
    }

    private void applyRelocationPlans(List<RelocationPlan> plans) {
        for (RelocationPlan plan : plans) {
            BookmarkNodeModel bookmark = plan.bookmark;
            bookmark.release();
            bookmark.setLine(plan.newLine);
            bookmark.setOpenFileDescriptor(new OpenFileDescriptor(project, plan.file, plan.newLine, 0));
            bookmark.createLineMarker();
            project.getMessageBus().syncPublisher(BookmarkListener.TOPIC).bookmarkChanged(bookmark);
        }
        BookmarksManager.getInstance(project).persistentSave();
        notifyRelocationApplied(plans.size());
    }

    private void applyAnchorUpdates(List<AnchorUpdate> updates) {
        for (AnchorUpdate update : updates) {
            BookmarkNodeModel bookmark = update.bookmark;
            bookmark.setAnchorLineText(update.anchorLineText);
            bookmark.setAnchorLineHash(update.anchorLineHash);
            bookmark.setBeforeContextHash(update.beforeContextHash);
            bookmark.setAfterContextHash(update.afterContextHash);
            bookmark.setSnapshotId(update.snapshotId);
            bookmark.setBaseContentHash(update.baseContentHash);
            bookmark.setGitRootPath(update.gitRootPath);
            bookmark.setGitRelativePath(update.gitRelativePath);
            bookmark.setGitCommit(update.gitCommit);
        }
    }

    private void refreshAnchorsAfterRelocation(List<RelocationPlan> plans) {
        Map<VirtualFile, List<BookmarkNodeModel>> bookmarksByFile = new LinkedHashMap<>();
        for (RelocationPlan plan : plans) {
            bookmarksByFile.computeIfAbsent(plan.file, ignored -> new ArrayList<>()).add(plan.bookmark);
        }
        for (Map.Entry<VirtualFile, List<BookmarkNodeModel>> entry : bookmarksByFile.entrySet()) {
            refreshFileAnchorsAsync(entry.getKey(), entry.getValue());
        }
    }

    private void notifyRelocationAvailable(int count) {
        Notifications.Bus.notify(new Notification(
                ToolWindowId.PROJECT_VIEW,
                "Bookmark-X",
                count + " bookmark location(s) may be out of date. Use the Bookmark-X relocate button to update them.",
                NotificationType.WARNING
        ), project);
    }

    private void notifyRelocationApplied(int count) {
        Notifications.Bus.notify(new Notification(
                ToolWindowId.PROJECT_VIEW,
                "Bookmark-X",
                count + " bookmark location(s) relocated.",
                NotificationType.INFORMATION
        ), project);
    }

    private void notifyNoRelocationNeeded() {
        Notifications.Bus.notify(new Notification(
                ToolWindowId.PROJECT_VIEW,
                "Bookmark-X",
                I18N.get("bookmark.relocate.notNeeded"),
                NotificationType.INFORMATION
        ), project);
    }

    private Map<VirtualFile, List<BookmarkNodeModel>> groupByFile(List<BookmarkNodeModel> bookmarks) {
        Map<VirtualFile, List<BookmarkNodeModel>> result = new LinkedHashMap<>();
        for (BookmarkNodeModel bookmark : bookmarks) {
            if (bookmark == null || bookmark.getOpenFileDescriptor() == null) {
                continue;
            }
            VirtualFile file = bookmark.getOpenFileDescriptor().getFile();
            if (file != null) {
                result.computeIfAbsent(file, ignored -> new ArrayList<>()).add(bookmark);
            }
        }
        return result;
    }

    private Optional<GitDiffRelocationStrategy.GitInfo> inspectGit(VirtualFile file) {
        String basePath = project.getBasePath();
        if (basePath == null || file == null) {
            return Optional.empty();
        }
        return GitDiffRelocationStrategy.inspectCleanTrackedFile(new File(basePath), new File(file.getPath()));
    }

    private boolean isTextFile(VirtualFile file) {
        return file != null && !file.getFileType().isBinary();
    }

    private String readTextInReadAction(VirtualFile file) {
        return ReadAction.compute(() -> {
            try {
                return readText(file);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private String readText(VirtualFile file) throws IOException {
        Document document = FileDocumentManager.getInstance().getCachedDocument(file);
        if (document != null) {
            return document.getText();
        }
        return VfsUtilCore.loadText(file);
    }

    private static final class RelocationPlan {
        private final BookmarkNodeModel bookmark;
        private final VirtualFile file;
        private final int newLine;

        private RelocationPlan(BookmarkNodeModel bookmark, VirtualFile file, int newLine) {
            this.bookmark = bookmark;
            this.file = file;
            this.newLine = newLine;
        }
    }

    private static final class AnchorUpdate {
        private final BookmarkNodeModel bookmark;
        private final String anchorLineText;
        private final String anchorLineHash;
        private final String beforeContextHash;
        private final String afterContextHash;
        private final String snapshotId;
        private final String baseContentHash;
        private final String gitRootPath;
        private final String gitRelativePath;
        private final String gitCommit;

        private AnchorUpdate(BookmarkNodeModel bookmark,
                             String anchorLineText,
                             String anchorLineHash,
                             String beforeContextHash,
                             String afterContextHash,
                             String snapshotId,
                             String baseContentHash,
                             String gitRootPath,
                             String gitRelativePath,
                             String gitCommit) {
            this.bookmark = bookmark;
            this.anchorLineText = anchorLineText;
            this.anchorLineHash = anchorLineHash;
            this.beforeContextHash = beforeContextHash;
            this.afterContextHash = afterContextHash;
            this.snapshotId = snapshotId;
            this.baseContentHash = baseContentHash;
            this.gitRootPath = gitRootPath;
            this.gitRelativePath = gitRelativePath;
            this.gitCommit = gitCommit;
        }
    }
}
