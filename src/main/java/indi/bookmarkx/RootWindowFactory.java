package indi.bookmarkx;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import indi.bookmarkx.action.BookmarkExportAction;
import indi.bookmarkx.action.BookmarkImportAction;
import indi.bookmarkx.persistence.MySettings;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import java.util.Arrays;


public class RootWindowFactory implements ToolWindowFactory, DumbAware {

    private static final Logger LOG = Logger.getInstance(RootWindowFactory.class);

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        initTitleAction(toolWindow);
        BookmarksManager manager = BookmarksManager.getInstance(project);
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content regularRetention = contentFactory.createContent(manager.getToolWindowRootPanel(), null, false);
        toolWindow.getContentManager().addContent(regularRetention);

        MySettings settings = MySettings.getInstance();
        MySettingsConfigurable.DescShowType descShowType = settings.getDescShowType();
        switch (descShowType) {
            case POPUP:
                toolWindow.setAnchor(ToolWindowAnchor.LEFT, () -> toolWindow.setSplitMode(true, null));
                break;
            case SPLIT_PANE:
                toolWindow.setAnchor(ToolWindowAnchor.BOTTOM, () -> toolWindow.setSplitMode(false, null));
                break;
            default:
                break;
        }

    }

    private void initTitleAction(ToolWindow toolWindow) {
        BookmarkExportAction exportAction = new BookmarkExportAction();
        BookmarkImportAction importAction = new BookmarkImportAction();

        // 在 ToolWindow 的标题栏中添加自定义动作按钮
        toolWindow.setTitleActions(Arrays.asList(importAction, exportAction));
    }
}
