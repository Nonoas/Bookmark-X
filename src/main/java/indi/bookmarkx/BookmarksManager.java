package indi.bookmarkx;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import indi.bookmarkx.dialog.BookmarkCreatorDialog;
import indi.bookmarkx.model.BookmarkConverter;
import indi.bookmarkx.model.BookmarkNodeModel;
import indi.bookmarkx.model.po.BookmarkPO;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * 项目级别的管理器：用于管理所有「书签UI」的变化
 */
@Service(Service.Level.PROJECT)
public final class BookmarksManager {

    public Project project;

    private BookmarksManagePanel toolWindowRootPanel;

    public BookmarksManager(Project project) {
        this.project = project;
    }

    public static BookmarksManager getInstance(Project project) {
        return project.getService(BookmarksManager.class);
    }

    /**
     * 创建一个书签
     *
     * @param project 项目
     * @param editor  编辑器
     * @param file    文件
     */
    public void createBookRemark(Project project, Editor editor, VirtualFile file) {
        CaretModel caretModel = editor.getCaretModel();

        // 获取行号
        int line = caretModel.getLogicalPosition().line;
        int column = caretModel.getLogicalPosition().column;
        // 获取选中文本
        String selectedText = caretModel.getCurrentCaret().getSelectedText();
        selectedText = selectedText == null ? "" : (" " + selectedText + " ");

        String uuid = UUID.randomUUID().toString();

        new BookmarkCreatorDialog(project)
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

    public static void editBookRemark(BookmarkNodeModel model) {
        Project project = model.getOpenFileDescriptor().getProject();
        BookmarksManager instance = BookmarksManager.getInstance(project);

        new BookmarkCreatorDialog(project)
                .defaultName(model.getName())
                .defaultDesc(model.getDesc())
                .showAndCallback((name, desc) -> {
                    model.setName(name);
                    model.setDesc(desc);
                    instance.persistentSave();
                });
    }

    /**
     * 持久化保存
     */
    public void persistentSave() {
        BookmarkPO po = PersistenceUtil.getPersistenceObject(toolWindowRootPanel.tree());
        MyPersistent persistent = MyPersistent.getInstance(project);
        persistent.setState(po);
        Application application = ApplicationManager.getApplication();
        application.saveSettings();
    }

    private void addToTree(BookmarkNodeModel bookmarkModel) {
        toolWindowRootPanel.addAndGet(bookmarkModel);
    }

    public static void add(BookmarkPopup p) {

    }

    public void prev() {
        toolWindowRootPanel.prev();
    }

    public void next() {
        toolWindowRootPanel.next();
    }

    public void select(BookmarkPopup popup) {
        popup.select(true);
        popup.navigate();
    }

    public void setToolWindowRootPanel(@NotNull BookmarksManagePanel panel) {
        this.toolWindowRootPanel = panel;
    }
}
