package indi.bookmarkx.action;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import indi.bookmarkx.BookmarksManager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * 通过在行标签处右键添加书签时触发的事件
 */
public class MyGutterContextMenuAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // 获取 ContextComponent，即触发动作的 UI 组件
        DataContext dataContext = e.getDataContext();
        Integer lineNum = EditorGutterComponentEx.LOGICAL_LINE_AT_CURSOR.getData(dataContext);

        Project project = e.getProject();
        VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(dataContext);
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        if (project == null || file == null || null == lineNum || null == editor) {
            return;
        }

        Document document = editor.getDocument();
        // 获取指定行的文本内容
        String lineText = document.getText().substring(document.getLineStartOffset(lineNum), document.getLineEndOffset(lineNum));

        BookmarksManager manager = BookmarksManager.getInstance(project);
        manager.createBookRemark(project, file, StringUtils.trim(lineText), lineNum);

        // 使文件标记失效并强制重新计算
        DaemonCodeAnalyzer daemonCodeAnalyzer = DaemonCodeAnalyzer.getInstance(project);
        Optional.ofNullable(e.getData(CommonDataKeys.PSI_FILE))
                .ifPresent(daemonCodeAnalyzer::restart);
    }

}
