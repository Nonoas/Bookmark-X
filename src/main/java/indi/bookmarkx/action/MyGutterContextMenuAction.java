package indi.bookmarkx.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

public class MyGutterContextMenuAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) return;

        // 获取当前光标所在的行号
        CaretModel caretModel = editor.getCaretModel();
        int lineNumber = caretModel.getLogicalPosition().line; // 获取当前行号

        // 获取当前编辑器和项目
        Project project = e.getProject();
        if (project != null) {
            Messages.showMessageDialog(project, "You clicked the gutter! 行号：" + lineNumber, "Info", Messages.getInformationIcon());
        }
    }

}
