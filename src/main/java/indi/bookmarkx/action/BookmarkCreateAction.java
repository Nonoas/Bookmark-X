package indi.bookmarkx.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import indi.bookmarkx.BookmarksManager;
import indi.bookmarkx.common.I18N;
import org.jetbrains.annotations.NotNull;

public class BookmarkCreateAction extends AnAction {

    public static String ACTION_TEXT = I18N.get("bookmark.create");

    public BookmarkCreateAction() {
        super(ACTION_TEXT, null, null);
    }

    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (project == null || editor == null || file == null)
            return;

        BookmarksManager.getInstance(project)
                .createBookRemark(project, editor, file);
    }
}
