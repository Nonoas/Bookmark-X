package indi.worktool.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import indi.worktool.BookmarksManager;
import org.jetbrains.annotations.NotNull;

public class PrevBookmarkAction extends AnAction {
    public void actionPerformed(@NotNull AnActionEvent e) {
        BookmarksManager.prev();
    }
}
