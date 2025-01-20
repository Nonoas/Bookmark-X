package indi.bookmarkx;

import com.google.gson.Gson;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import indi.bookmarkx.action.BookmarkExportAction;
import indi.bookmarkx.action.BookmarkImportAction;
import indi.bookmarkx.common.Constants;
import indi.bookmarkx.common.I18N;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.jetbrains.annotations.NotNull;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


public class RootWindowFactory implements ToolWindowFactory, DumbAware {

    private static final Logger LOG = Logger.getInstance(RootWindowFactory.class);
    private static final String REPOSITORY_URL = "https://plugins.jetbrains.com/api/plugins";

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        initTitleAction(toolWindow);
        BookmarksManager manager = BookmarksManager.getInstance(project);
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content regularRetention = contentFactory.createContent(manager.getToolWindowRootPanel(), null, false);
        toolWindow.getContentManager().addContent(regularRetention);

        // TODO 提示 beta 版本, 需要改为后台执行
        // https://plugins.jetbrains.com/api/plugins/22013/updates?channel=beta&size=1
        checkForUpdates(project);
    }

    private void initTitleAction(ToolWindow toolWindow) {
        BookmarkExportAction exportAction = new BookmarkExportAction();
        BookmarkImportAction importAction = new BookmarkImportAction();

        // 在 ToolWindow 的标题栏中添加自定义动作按钮
        toolWindow.setTitleActions(Arrays.asList(importAction, exportAction));
    }

    private void checkForUpdates(Project project) {
        try {
            // 当前插件 ID
            PluginId pluginId = PluginId.getId(Constants.PLUGIN_ID);
            IdeaPluginDescriptor pluginDescriptor = PluginManagerCore.getPlugin(pluginId);
            if (null == pluginDescriptor) {
                return;
            }
            // 获取当前插件版本
            String currentVersion = pluginDescriptor.getVersion();
            // 从远程获取最新版本
            String latestVersion = fetchLatestVersionFromBeta();

            // 比较版本号
            if (isVersionNewer(latestVersion, currentVersion)) {
                String groupId = ToolWindowId.PROJECT_VIEW;
                Notification notification = new Notification(groupId,
                        "版本更新",
                        "A new Beta version is available: " + latestVersion + ",You can go to " +
                                "https://plugins.jetbrains.com/plugin/22013-bookmark-x/edit/versions/beta for download.",
                        NotificationType.INFORMATION);
                Notifications.Bus.notify(notification, project);
            }
        } catch (Exception e) {
            LOG.error("Failed to check for updates: ", e);
        }
    }

    private String fetchLatestVersionFromBeta() throws Exception {
        // 拼接 Marketplace API URL
        String urlString = REPOSITORY_URL + "/" + Constants.PLUGIN_MARKET_ID + "/updates?channel=beta&size=1";

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        try (Scanner scanner = new Scanner(connection.getInputStream())) {
            StringBuilder response = new StringBuilder();
            while (scanner.hasNext()) {
                response.append(scanner.nextLine());
            }
            // 解析最新版本 (简单示例，实际应该解析 JSON)
            return parseLatestVersionFromResponse(response.toString());
        }
    }

    private String parseLatestVersionFromResponse(String response) {
        Gson gson = new Gson();
        List<Map<String, Object>> list = gson.fromJson(response, List.class);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        Map<String, Object> map = list.get(0);
        //return MapUtils.getString(map, "version"); // 替换为实际解析逻辑
        return "2.2.1"; // 替换为实际解析逻辑
    }

    private boolean isVersionNewer(String latestVersion, String currentVersion) {
        return latestVersion.compareTo(currentVersion) > 0;
    }

}
