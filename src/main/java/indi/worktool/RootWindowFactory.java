package indi.worktool;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;


public class RootWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, ToolWindow toolWindow) {
        BookmarksManager.project = project;

        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content regularRetention = contentFactory.createContent(BookmarksManagePanel.getInstance(project), "书签", false);
        toolWindow.getContentManager().addContent(regularRetention);

    }
}
