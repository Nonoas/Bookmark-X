package indi.bookmarkx.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import indi.bookmarkx.WebPageOpener;
import org.jetbrains.annotations.NotNull;

public class ToHelpWebPageAction extends AnAction {

    public void actionPerformed(@NotNull AnActionEvent e) {
        WebPageOpener.openWebPage("https://www.nonoas.top/archives/bookmark-x");
    }

}
