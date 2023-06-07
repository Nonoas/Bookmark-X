package indi.bookmarkx.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import indi.bookmarkx.BookmarksManager;
import org.jetbrains.annotations.NotNull;

public class PrevBookmarkAction extends AnAction {
    public void actionPerformed(@NotNull AnActionEvent e) {
        BookmarksManager.prev();
    }
}
