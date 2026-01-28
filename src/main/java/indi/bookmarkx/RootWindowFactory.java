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

        initPosition(toolWindow);
    }

    private void initPosition(@NotNull ToolWindow toolWindow) {
        MySettings settings = MySettings.getInstance();
        MySettingsConfigurable.DescShowType descShowType = settings.getDescShowType();

        // 2. 核心：修改位置属性
        if (descShowType == MySettingsConfigurable.DescShowType.POPUP) {
            // 设置为左侧，并确保 splitMode 开启
            toolWindow.setAnchor(ToolWindowAnchor.LEFT, null);
            toolWindow.setSplitMode(true, null);
        } else if (descShowType == MySettingsConfigurable.DescShowType.SPLIT_PANE) {
            // 设置为底部，关闭分栏
            toolWindow.setAnchor(ToolWindowAnchor.BOTTOM, null);
            toolWindow.setSplitMode(false, null);
        }
    }

    private void initTitleAction(ToolWindow toolWindow) {
        BookmarkExportAction exportAction = new BookmarkExportAction();
        BookmarkImportAction importAction = new BookmarkImportAction();

        // 在 ToolWindow 的标题栏中添加自定义动作按钮
        toolWindow.setTitleActions(Arrays.asList(importAction, exportAction));
    }
}
