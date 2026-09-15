package indi.bookmarkx.service.branch;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.BranchChangeListener;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.util.Alarm;
import indi.bookmarkx.BookmarksManager;
import indi.bookmarkx.model.po.BookmarkPO;
import indi.bookmarkx.utils.PersistenceUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service(Service.Level.PROJECT)
public final class BookmarkBranchSnapshotService {

    private static final Logger LOG = Logger.getInstance(BookmarkBranchSnapshotService.class);
    private static final int SAVE_DEBOUNCE_MS = 1000;
    private static final int BRANCH_CHANGE_DELAY_MS = 300;
    private static final int TREE_LOAD_RETRY_MS = 200;

    private final Project project;
    private final BookmarkBranchSnapshotStore snapshotStore;
    private final BookmarkBranchState branchState;
    private final Alarm saveAlarm;
    private final Alarm workAlarm;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private volatile boolean restoringSnapshot;

    public BookmarkBranchSnapshotService(Project project) {
        this.project = project;
        this.snapshotStore = BookmarkBranchSnapshotStore.getInstance(project);
        this.branchState = BookmarkBranchState.getInstance(project);
        this.saveAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, project);
        this.workAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, project);
    }

    public static BookmarkBranchSnapshotService getInstance(Project project) {
        return project.getService(BookmarkBranchSnapshotService.class);
    }

    public void initialize() {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }
        project.getMessageBus().connect(project).subscribe(BranchChangeListener.VCS_BRANCH_CHANGED, new BranchChangeListener() {
            @Override
            public void branchWillChange(String branchName) {
                saveCurrentBranchSnapshotImmediately();
            }

            @Override
            public void branchHasChanged(String branchName) {
                workAlarm.cancelAllRequests();
                workAlarm.addRequest(BookmarkBranchSnapshotService.this::restoreOrCreateCurrentBranchSnapshot,
                        BRANCH_CHANGE_DELAY_MS);
            }
        });
        workAlarm.addRequest(this::initializeCurrentBranch, BRANCH_CHANGE_DELAY_MS);
    }

    public void scheduleCurrentSnapshotSave() {
        if (!initialized.get() || restoringSnapshot || project.isDisposed()) {
            return;
        }
        saveAlarm.cancelAllRequests();
        saveAlarm.addRequest(this::saveDetectedBranchSnapshot, SAVE_DEBOUNCE_MS);
    }

    private void initializeCurrentBranch() {
        Optional<GitBranchContext> context = GitBranchDetector.detect(project);
        if (context.isEmpty()) {
            return;
        }
        GitBranchContext current = context.get();
        String activeBranchKey = branchState.getActiveBranchKey();
        if (StringUtils.isBlank(activeBranchKey) || activeBranchKey.equals(current.getBranchKey())) {
            branchState.setActiveBranch(current);
            scheduleCurrentSnapshotSave();
            return;
        }
        runWhenTreeLoaded(() -> restoreOrCreateSnapshot(current));
    }

    private void saveCurrentBranchSnapshotImmediately() {
        Optional<GitBranchContext> context = GitBranchDetector.detect(project);
        if (context.isEmpty()) {
            return;
        }
        saveAlarm.cancelAllRequests();
        BookmarksManager manager = BookmarksManager.getInstance(project);
        if (!manager.getToolWindowRootPanel().isTreeLoaded()) {
            return;
        }
        BookmarkPO state = captureCurrentBookmarkState(manager);
        ApplicationManager.getApplication().executeOnPooledThread(() -> writeSnapshot(context.get(), state, false));
    }

    private void saveDetectedBranchSnapshot() {
        GitBranchDetector.detect(project).ifPresent(this::saveSnapshot);
    }

    private Optional<BranchBookmarkSnapshot> saveSnapshot(GitBranchContext context) {
        if (context == null || project.isDisposed()) {
            return Optional.empty();
        }
        BookmarksManager manager = BookmarksManager.getInstance(project);
        if (!manager.getToolWindowRootPanel().isTreeLoaded()) {
            saveAlarm.addRequest(() -> saveSnapshot(context), TREE_LOAD_RETRY_MS);
            return Optional.empty();
        }
        try {
            BookmarkPO state = captureCurrentBookmarkState(manager);
            return writeSnapshot(context, state, true);
        } catch (Exception e) {
            LOG.debug("Failed to save bookmark branch snapshot", e);
            return Optional.empty();
        }
    }

    private Optional<BranchBookmarkSnapshot> writeSnapshot(GitBranchContext context, BookmarkPO state, boolean updateActiveBranch) {
        Optional<BranchBookmarkSnapshot> snapshot = snapshotStore.write(context, state);
        if (updateActiveBranch) {
            snapshot.ifPresent(ignored -> branchState.setActiveBranch(context));
        }
        return snapshot;
    }

    private void restoreOrCreateCurrentBranchSnapshot() {
        Optional<GitBranchContext> context = GitBranchDetector.detect(project);
        if (context.isEmpty()) {
            return;
        }
        runWhenTreeLoaded(() -> restoreOrCreateSnapshot(context.get()));
    }

    private void restoreOrCreateSnapshot(GitBranchContext context) {
        if (context == null || project.isDisposed()) {
            return;
        }
        Optional<BranchBookmarkSnapshot> snapshot = snapshotStore.read(context.getBranchKey());
        if (snapshot.isPresent()) {
            restoreSnapshot(context, snapshot.get());
            return;
        }
        createInitialSnapshot(context);
    }

    private void restoreSnapshot(GitBranchContext context, BranchBookmarkSnapshot snapshot) {
        BookmarkPO root = PersistenceUtil.deepCopy(snapshot.getRoot(), BookmarkPO.class);
        if (root == null) {
            createInitialSnapshot(context);
            return;
        }
        try {
            restoringSnapshot = true;
            branchState.setActiveBranch(context);
            BookmarksManager.getInstance(project).replaceBookmarkState(root);
        } finally {
            restoringSnapshot = false;
        }
    }

    private void createInitialSnapshot(GitBranchContext context) {
        Optional<BranchBookmarkSnapshot> snapshot = saveSnapshot(context);
        if (snapshot.isPresent()) {
            notifyNewBranchSnapshot(context);
        }
    }

    private void runWhenTreeLoaded(Runnable action) {
        if (project.isDisposed()) {
            return;
        }
        BookmarksManager manager = BookmarksManager.getInstance(project);
        if (manager.getToolWindowRootPanel().isTreeLoaded()) {
            action.run();
            return;
        }
        workAlarm.addRequest(() -> runWhenTreeLoaded(action), TREE_LOAD_RETRY_MS);
    }

    private BookmarkPO captureCurrentBookmarkState(BookmarksManager manager) {
        return callOnEdt(() -> PersistenceUtil.deepCopy(manager.getCurrentBookmarkState(), BookmarkPO.class));
    }

    private <T> T callOnEdt(ThrowableSupplier<T> supplier) {
        Application application = ApplicationManager.getApplication();
        if (application.isDispatchThread()) {
            return getOrThrow(supplier);
        }
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<RuntimeException> error = new AtomicReference<>();
        application.invokeAndWait(() -> {
            try {
                result.set(supplier.get());
            } catch (RuntimeException ex) {
                error.set(ex);
            } catch (Exception ex) {
                error.set(new IllegalStateException(ex));
            }
        }, application.getDefaultModalityState());
        if (error.get() != null) {
            throw error.get();
        }
        return result.get();
    }

    private <T> T getOrThrow(ThrowableSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void notifyNewBranchSnapshot(GitBranchContext context) {
        Notifications.Bus.notify(new Notification(
                ToolWindowId.PROJECT_VIEW,
                "Bookmark-X",
                "Created an initial bookmark snapshot for Git branch: " + context.getDisplayName(),
                NotificationType.INFORMATION
        ), project);
    }

    @FunctionalInterface
    private interface ThrowableSupplier<T> {
        T get() throws Exception;
    }
}
