package indi.bookmarkx;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import indi.bookmarkx.common.I18N;
import indi.bookmarkx.common.data.BookmarkArrayListTable;
import indi.bookmarkx.ui.dialog.BookmarkCreatorDialog;
import indi.bookmarkx.model.BookmarkConverter;
import indi.bookmarkx.model.BookmarkNodeModel;
import indi.bookmarkx.ui.painter.LineEndPainter;
import indi.bookmarkx.ui.tree.BookmarkTree;
import indi.bookmarkx.ui.tree.BookmarkTreeNode;
import indi.bookmarkx.ui.pannel.BookmarksManagePanel;
import indi.bookmarkx.utils.PersistenceUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * 项目级别的管理器：用于管理所有「书签UI」的变化
 */
@Service(Service.Level.PROJECT)
public final class BookmarksManager {

    public Project project;

    private BookmarksManagePanel toolWindowRootPanel;

    private BookmarkArrayListTable bookmarkArrayListTable;

    public BookmarksManager(Project project) {
        this.project = project;
        bookmarkArrayListTable = BookmarkArrayListTable.getInstance(project);
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

        BookmarkNodeModel bookmarkNodeModel = LineEndPainter.findLine(BookmarkArrayListTable.getInstance(project).getOnlyIndex(file.getPath()), line);
        String defaultName = file.getName();
        String defaultDesc;
        boolean add = true;
        if (bookmarkNodeModel == null) {
            // 获取选中文本
            String selectedText = caretModel.getCurrentCaret().getSelectedText();
            defaultDesc = selectedText == null ? "" : (" " + selectedText + " ");
            String uuid = UUID.randomUUID().toString();
            bookmarkNodeModel = new BookmarkNodeModel();
            bookmarkNodeModel.setUuid(uuid);
            bookmarkNodeModel.setLine(line);
            bookmarkNodeModel.setColumn(column);
            bookmarkNodeModel.setIcon(file.getFileType().getIcon());
            bookmarkNodeModel.setIcon(file.getFileType().getIcon());
            bookmarkNodeModel.setOpenFileDescriptor(new OpenFileDescriptor(project, file, line, column));
        }else {
            add = false;
            defaultName = bookmarkNodeModel.getName();
            defaultDesc = bookmarkNodeModel.getDesc();
        }
        final BookmarkNodeModel finalBookmarkNodeModel = bookmarkNodeModel;
        final boolean addFlag = add;
        new BookmarkCreatorDialog(project, I18N.get("bookmark.create.title"))
                .defaultName(defaultName)
                .defaultDesc(defaultDesc)
                .showAndCallback((name, desc) -> {
                    finalBookmarkNodeModel.setName(name);
                    finalBookmarkNodeModel.setDesc(desc);
                    bookmarkArrayListTable.insert(finalBookmarkNodeModel);
                    if (addFlag) {
                        submitCreateBookRemark(finalBookmarkNodeModel, editor);
                    } else {
                        if (!Objects.isNull(toolWindowRootPanel)) {
                            BookmarkTree tree = toolWindowRootPanel.tree();
                            BookmarkTreeNode nodeByModel = tree.getNodeByModel(finalBookmarkNodeModel);
                            tree.getModel().nodeChanged(nodeByModel);
                        }
                    }

                });
    }

    private void submitCreateBookRemark(BookmarkNodeModel bookmarkModel, Editor editor) {
        //  The toolWindowRootPanel may be null the first time IDEA is opened
        if (Objects.isNull(toolWindowRootPanel)) {
            MyPersistent persistent = MyPersistent.getInstance(project);
            persistent.getState().getChildren().add(BookmarkConverter.convertToPO(bookmarkModel));
        } else {
            afterCreateSubmit(bookmarkModel, editor);
        }

    }

    private void afterCreateSubmit(BookmarkNodeModel bookmarkModel, Editor editor) {
        addToTree(bookmarkModel);
    }

    /**
     * 持久化保存
     */
    public void persistentSave() {
        PersistenceUtil.persistentSave(project, toolWindowRootPanel.tree());
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

    /**
     * 重新加载标签树
     */
    public void reload() {
        toolWindowRootPanel.reInit(project);
    }
}
