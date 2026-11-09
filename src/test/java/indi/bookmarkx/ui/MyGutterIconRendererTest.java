package indi.bookmarkx.ui;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import indi.bookmarkx.action.BookmarkLocateInTreeAction;
import indi.bookmarkx.common.I18N;
import indi.bookmarkx.common.I18NEnum;
import indi.bookmarkx.model.BookmarkNodeModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MyGutterIconRendererTest {

    @Test
    void offersLocateInTreeBeforeEditAndDelete() {
        I18N.switchLanguage(I18NEnum.ENGLISH);
        MyGutterIconRenderer renderer = new MyGutterIconRenderer(new BookmarkNodeModel());

        DefaultActionGroup actionGroup = (DefaultActionGroup) renderer.getPopupMenuActions();
        AnAction[] actions = actionGroup.getChildActionsOrStubs();

        assertEquals(3, actions.length);
        assertTrue(actions[0] instanceof BookmarkLocateInTreeAction);
        assertEquals("Locate in Bookmark Tree", actions[0].getTemplatePresentation().getText());
    }
}
