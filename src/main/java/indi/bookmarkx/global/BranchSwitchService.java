package indi.bookmarkx.global;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Service(Service.Level.PROJECT)
public final class BranchSwitchService {

    private volatile boolean branchSwitchInProgress = false;
    private final Project project;

    public BranchSwitchService(Project project) {
        this.project = project;
        project.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {
                for (VFileEvent event : events) {
                    String path = event.getPath();
                    if (path.endsWith(".git/HEAD") || path.contains(".git/refs/heads/")) {
                        onBranchChanged();
                        break;
                    }
                }
            }
        });
    }

    public boolean isBranchSwitchInProgress() {
        return branchSwitchInProgress;
    }

    private void onBranchChanged() {
        branchSwitchInProgress = true;
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                BookmarkReconciler.reconcile(project);
            } finally {
                branchSwitchInProgress = false;
            }
        });
    }

    public static BranchSwitchService getInstance(Project project) {
        return project.getService(BranchSwitchService.class);
    }
}