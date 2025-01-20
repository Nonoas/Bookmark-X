package indi.bookmarkx.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import indi.bookmarkx.BookmarksManager;
import indi.bookmarkx.common.I18N;
import indi.bookmarkx.model.AbstractTreeNodeModel;
import org.jetbrains.annotations.NotNull;

/**
 * 书签删除动作
 */
public class BookmarkRemoveAction extends AnAction {

    public static String ACTION_TEXT = I18N.get("bookmark.delete");

    private final AbstractTreeNodeModel model;

    public BookmarkRemoveAction(AbstractTreeNodeModel model) {
        super(ACTION_TEXT, null, null);
        this.model = model;
    }

    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (null == project) {
            return;
        }
        BookmarksManager manager = BookmarksManager.getInstance(project);
        manager.removeBookRemark(model);
    }
}
