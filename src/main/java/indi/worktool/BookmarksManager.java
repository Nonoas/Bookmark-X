package indi.worktool;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import indi.worktool.dialog.BookmarkCreatorDialog;
import indi.worktool.model.BookmarkNodeModel;
import indi.worktool.model.po.BookmarkPO;

import java.util.UUID;

/**
 * 用于管理所有「书签UI」的变化
 */
public class BookmarksManager {

    public static Project project;

    /**
     * 创建一个书签
     *
     * @param project 项目
     * @param editor  编辑器
     * @param file    文件
     */
    public static void createBookRemarkPopup(Project project, Editor editor, VirtualFile file) {
        CaretModel caretModel = editor.getCaretModel();

        // 获取行号
        int line = caretModel.getLogicalPosition().line;
        int column = caretModel.getLogicalPosition().column;
        // 获取选中文本
        String selectedText = caretModel.getCurrentCaret().getSelectedText();
        selectedText = selectedText == null ? "" : (" " + selectedText + " ");

        String uuid = UUID.randomUUID().toString();

        new BookmarkCreatorDialog()
                .defaultName(file.getName())
                .defaultDesc(selectedText)
                .showAndCallback((name, desc) -> {
                    BookmarkNodeModel bookmarkModel = new BookmarkNodeModel();
                    bookmarkModel.setUuid(uuid);
                    bookmarkModel.setLine(line);
                    bookmarkModel.setColumn(column);
                    bookmarkModel.setIcon(file.getFileType().getIcon());
                    bookmarkModel.setName(name);
                    bookmarkModel.setIcon(file.getFileType().getIcon());
                    bookmarkModel.setOpenFileDescriptor(new OpenFileDescriptor(project, file, line, column));
                    bookmarkModel.setDesc(desc);

                    addToTree(bookmarkModel);
                });
    }

    public static void persistentSave(BookmarkPO po) {
        MyPersistent persistent = MyPersistent.getInstance(project);
        persistent.setState(po);
        Application application = ApplicationManager.getApplication();
        application.saveSettings();
    }

    private static void addToTree(BookmarkNodeModel bookmarkModel) {
        BookmarksManagePanel managePanel = BookmarksManagePanel.getInstance();
        managePanel.addAndGet(bookmarkModel);
    }

    public static void add(BookmarkPopup p) {

    }

    public static void prev() {
        BookmarksManagePanel.getInstance().prev();
    }

    public static void next() {
        BookmarksManagePanel.getInstance().next();

    }

    public static void select(BookmarkPopup popup) {
        popup.select(true);
        popup.navigate();
    }
}
