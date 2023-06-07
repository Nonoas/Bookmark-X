package indi.worktool.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import indi.worktool.WebPageOpener;
import org.jetbrains.annotations.NotNull;

public class ToHelpWebPageAction extends AnAction {

    public void actionPerformed(@NotNull AnActionEvent e) {
        WebPageOpener.openWebPage("https://www.nonoas.top");
    }

}
