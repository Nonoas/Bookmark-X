package indi.bookmarkx.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import indi.bookmarkx.BookmarksManager;
import indi.bookmarkx.common.I18N;
import indi.bookmarkx.model.BookmarkNodeModel;
import org.jetbrains.annotations.NotNull;

/**
 * 在 Bookmark-X 标签树中定位书签。
 */
public class BookmarkLocateInTreeAction extends AnAction {

    private final BookmarkNodeModel model;

    public BookmarkLocateInTreeAction(BookmarkNodeModel model) {
        super(I18N.get("bookmark.locateInTree"), null, null);
        this.model = model;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        BookmarksManager.getInstance(project).locateBookRemark(model);
    }
}
