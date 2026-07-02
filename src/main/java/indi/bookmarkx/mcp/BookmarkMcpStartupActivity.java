package indi.bookmarkx.mcp;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public class BookmarkMcpStartupActivity implements StartupActivity, DumbAware {

    @Override
    public void runActivity(@NotNull Project project) {
        ApplicationManager.getApplication().getService(BookmarkMcpServerService.class);
    }
}
