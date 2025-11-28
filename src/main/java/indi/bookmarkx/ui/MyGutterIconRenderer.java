package indi.bookmarkx.ui;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.GutterDraggableObject;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import indi.bookmarkx.action.BookmarkEditAction;
import indi.bookmarkx.action.BookmarkRemoveAction;
import indi.bookmarkx.common.MyIcons;
import indi.bookmarkx.model.BookmarkNodeModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.awt.Cursor;

public class MyGutterIconRenderer extends GutterIconRenderer {

    private final BookmarkNodeModel model;

    private RangeHighlighter lastHighlighter;

    public MyGutterIconRenderer(BookmarkNodeModel model) {
        this.model = model;
    }

    /**
     * GutterIcon上的菜单
     *
     * <pre>
     * 特别说明：
     *   因为增加了拖拽功能，所以这里从 getPopupMenuActions 换成了 getRightButtonClickAction。
     *   但在 idea version = 2021.2.2 时，鼠标左右键点击，都没菜单展示 (这应该是idea的bug！！)
     *     在 idea version = 2023.3.6 时，鼠标右键点击，有菜单展示
     * </pre>
     *
     * @return
     */
    private ActionGroup createPopupMenuActions() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new BookmarkEditAction(model));
        actionGroup.add(new BookmarkRemoveAction(model));
        return actionGroup;
    }

    @Override
    public @Nullable ActionGroup getPopupMenuActions() {
        // 兼容 2021.x / 2022.x
        return createPopupMenuActions();
    }

    @Override
    public @Nullable ActionGroup getRightButtonClickAction() {
        // 兼容 2023.x+（官方推荐）
        return createPopupMenuActions();
    }


    @Override
    public @Nullable String getTooltipText() {
        return model.getDesc();
    }

    @Override
    @NotNull
    public Icon getIcon() {
        return MyIcons.BOOKMARK;
    }

    @Override
    public @NotNull Alignment getAlignment() {
        return Alignment.RIGHT; // 图标对齐方式
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MyGutterIconRenderer;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public BookmarkNodeModel getModel() {
        return model;
    }

    @Override
    public GutterDraggableObject getDraggableObject() {
        // 拖拽
        return new GutterDraggableObject() {
            @Override
            public boolean copy(int line, VirtualFile file, int actionId) {
                Editor editor = getEditorForFile(file);
                if (editor != null) {
                    clearDragHighlights(editor);
                }
                model.updateBookmarkLine(line, true);
                return true;
            }

            @Override
            public Cursor getCursor(int line, VirtualFile file, int actionId) {
                Editor editor = getEditorForFile(file);
                if (editor != null) {
                    addDragHighlight(editor, line);
                }
                return GutterDraggableObject.super.getCursor(line, file, actionId);
            }

            @Override
            public void remove() {
                GutterDraggableObject.super.remove();
            }
        };
    }

    private Editor getEditorForFile(VirtualFile file) {
        if (file == null) {
            return null;
        }
        Project project = model.getOpenFileDescriptor().getProject();
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        FileEditor[] fileEditors = fileEditorManager.getEditors(file);
        for (FileEditor fileEditor : fileEditors) {
            if (fileEditor instanceof TextEditor) {
                return ((TextEditor) fileEditor).getEditor();
            }
        }
        return null;
    }

    private void addDragHighlight(Editor editor, int line) {
        // 清除之前添加的拖拽高亮
        clearDragHighlights(editor);

        MarkupModel markupModel = editor.getMarkupModel();
        TextAttributes attributes = new TextAttributes();
        attributes.setBackgroundColor(JBColor.YELLOW);
        lastHighlighter = markupModel.addLineHighlighter(
                line,
                HighlighterLayer.SELECTION - 1, // 层级略低于选中层
                attributes
        );
    }

    private void clearDragHighlights(Editor editor) {
        MarkupModel markupModel = editor.getMarkupModel();
        if (lastHighlighter != null) {
            markupModel.removeHighlighter(lastHighlighter);
        }
    }
}
