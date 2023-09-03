package indi.bookmarkx;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;


public class RootWindowFactory implements ToolWindowFactory {

    /**
     * plugin.xml 文件中的 key 名
     */
    public static final String TOOLWINDOW_KEY = "Bookmark-X";

    @Override
    public void createToolWindowContent(@NotNull Project project, ToolWindow toolWindow) {
        BookmarksManagePanel panel = BookmarksManagePanel.create(project);

        initManager(project, panel);

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content regularRetention = contentFactory.createContent(panel, null, false);

        toolWindow.getContentManager().addContent(regularRetention);
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
