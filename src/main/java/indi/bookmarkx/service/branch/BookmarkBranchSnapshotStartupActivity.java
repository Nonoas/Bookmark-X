package indi.bookmarkx.service.branch;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public final class BookmarkBranchSnapshotStartupActivity implements StartupActivity, DumbAware {

    @Override
    public void runActivity(@NotNull Project project) {
        BookmarkBranchSnapshotService.getInstance(project).initialize();
    }
}
