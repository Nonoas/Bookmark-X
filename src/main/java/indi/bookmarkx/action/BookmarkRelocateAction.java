package indi.bookmarkx.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import indi.bookmarkx.common.I18N;
import indi.bookmarkx.service.relocation.BookmarkRelocationService;
import org.jetbrains.annotations.NotNull;

public final class BookmarkRelocateAction extends AnAction {

    public BookmarkRelocateAction() {
        super(() -> I18N.get("bookmark.relocate"),
                () -> I18N.get("bookmark.relocate.description"),
                AllIcons.Actions.Refresh);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        BookmarkRelocationService.getInstance(project).relocateBookmarksAsync();
    }
}
