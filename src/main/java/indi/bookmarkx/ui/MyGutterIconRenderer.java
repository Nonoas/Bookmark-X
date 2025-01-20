package indi.bookmarkx.ui;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import indi.bookmarkx.action.BookmarkEditAction;
import indi.bookmarkx.action.BookmarkRemoveAction;
import indi.bookmarkx.common.MyIcons;
import indi.bookmarkx.model.BookmarkNodeModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class MyGutterIconRenderer extends GutterIconRenderer {

    private final BookmarkNodeModel model;

    public MyGutterIconRenderer(BookmarkNodeModel model) {
        this.model = model;
    }

    @Override
    public @NotNull ActionGroup getPopupMenuActions() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new BookmarkEditAction(model));
        actionGroup.add(new BookmarkRemoveAction(model));
        return actionGroup;
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
}
