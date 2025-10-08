package indi.bookmarkx;

import com.google.gson.Gson;
import com.intellij.ide.AppLifecycleListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.wm.ToolWindowId;
import indi.bookmarkx.common.Constants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.Desktop;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * @author Nonoas
 * @version 2.2.1
 * @date 2025/1/22
 * @since 2.2.1
 */
public final class VersionChecker implements AppLifecycleListener {

    private static final Logger LOG = Logger.getInstance(VersionChecker.class);
    private final String REPOSITORY_URL = "https://plugins.jetbrains.com/api/plugins";

    @Override
    public void appStarted() {
        ApplicationManager.getApplication().executeOnPooledThread(this::checkForUpdates);
    }

    private void checkForUpdates() {
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
                        "A new Beta version is available: " + latestVersion + ",You can go to the following link for download.",
                        NotificationType.INFORMATION);
                // 添加超链接按钮
                notification.addAction(new NotificationAction("Download bate version") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                        try {
                            // 打开链接
                            Desktop.getDesktop().browse(new URI("https://plugins.jetbrains.com/plugin/22013-bookmark-x/edit/versions/beta"));
                        } catch (Exception ex) {
                            LOG.error(ex);
                        }
                    }
                });
                Notifications.Bus.notify(notification, null);
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

    @SuppressWarnings("unchecked")
    private String parseLatestVersionFromResponse(String response) {
        Gson gson = new Gson();
        List<Map<String, Object>> list = gson.fromJson(response, List.class);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        Map<String, Object> map = list.get(0);
        return MapUtils.getString(map, "version"); // 替换为实际解析逻辑
    }

    private boolean isVersionNewer(String latestVersion, String currentVersion) {
        return latestVersion.compareTo(currentVersion) > 0;
    }

}
