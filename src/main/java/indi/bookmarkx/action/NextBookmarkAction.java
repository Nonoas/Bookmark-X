package indi.bookmarkx.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import indi.bookmarkx.BookmarksManager;
import org.jetbrains.annotations.NotNull;

public class NextBookmarkAction extends AnAction {
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (null == project) {
            return;
        }
        BookmarksManager manager = BookmarksManager.getInstance(project);
        manager.next();
    }
}
