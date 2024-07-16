package indi.bookmarkx;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import indi.bookmarkx.action.BookmarkExportAction;
import indi.bookmarkx.action.BookmarkImportAction;
import indi.bookmarkx.ui.pannel.BookmarksManagePanel;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;


public class RootWindowFactory implements ToolWindowFactory, DumbAware {

    /**
     * plugin.xml 文件中的 key 名
     */
    public static final String TOOLWINDOW_KEY = "Bookmark-X";

    @Override
    public void createToolWindowContent(@NotNull Project project, ToolWindow toolWindow) {

        initTitleAction(toolWindow);

        BookmarksManagePanel panel = BookmarksManagePanel.create(project);

        initManager(project, panel);

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content regularRetention = contentFactory.createContent(panel, null, false);

        toolWindow.getContentManager().addContent(regularRetention);
    }

    private void initTitleAction(ToolWindow toolWindow) {
        BookmarkExportAction exportAction = new BookmarkExportAction();
        BookmarkImportAction importAction = new BookmarkImportAction();

        // 在 ToolWindow 的标题栏中添加自定义动作按钮
        toolWindow.setTitleActions(Arrays.asList(importAction, exportAction));
    }

    /**
     * 初始化项目级别的书签管理器
     *
     * @param project 当前项目
     * @param panel   当前ToolWindow面板
     */
    private void initManager(Project project, BookmarksManagePanel panel) {
        BookmarksManager manager = project.getService(BookmarksManager.class);
        manager.setToolWindowRootPanel(panel);
    }

}
