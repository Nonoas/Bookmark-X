package indi.bookmarkx;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.markup.LineMarkerRenderer;
import com.intellij.openapi.editor.markup.MarkupEditorFilterFactory;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import indi.bookmarkx.dialog.BookmarkCreatorDialog;
import indi.bookmarkx.model.BookmarkConverter;
import indi.bookmarkx.model.BookmarkNodeModel;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Objects;
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
                    submitCreateBookRemark(bookmarkModel, editor);
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
        createLineMark(editor);
    }

    private void createLineMark(Editor editor) {
        // 获取当前光标所在的行号
        int lineNumber = editor.getCaretModel().getLogicalPosition().line;

        // 获取标记模型
        MarkupModel markupModel = editor.getMarkupModel();

        // 创建行标记
        RangeHighlighter lineMarker = markupModel.addLineHighlighter(lineNumber, 0, null);

        // 设置标记的颜色和图标
        lineMarker.setGutterIconRenderer(new LineMarkIRenderer());

        // 设置标记的提示文本
        lineMarker.setLineMarkerRenderer(new LineMarkerRenderer() {
            @Override
            public void paint(@NotNull Editor editor, @NotNull Graphics g, @NotNull Rectangle r) {

            }

        });

        // 添加标记到编辑器中
        lineMarker.setEditorFilter(MarkupEditorFilterFactory.createIsNotDiffFilter());

        // 刷新编辑器以显示标记
        editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);

        // 如果需要，你还可以保存标记对象以供后续操作
        // project.getComponent(MyLineMarkerStorage.class).addLineMarker(lineMarker);

        // 可以执行其他操作或处理标记对象
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
}
