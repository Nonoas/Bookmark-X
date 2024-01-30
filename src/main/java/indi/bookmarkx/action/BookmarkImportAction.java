package indi.bookmarkx.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import indi.bookmarkx.common.I18N;
import org.jetbrains.annotations.NotNull;

public final class BookmarkImportAction extends AnAction {

    private static final String ACTION_ID = I18N.get("bookmark.import");

    public BookmarkImportAction() {
        super(ACTION_ID, null, AllIcons.ToolbarDecorator.Import);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        System.out.println("Button clicked!");
    }
}
