package indi.bookmarkx.global;

import com.google.gson.Gson;
import com.intellij.ide.AppLifecycleListener;
import com.intellij.ide.BrowserUtil;
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

/**
 * 版本校验
 *
 * @author huangshengsheng
 * @date 2025/1/23 15:32
 * @since 2.2.1
 */
public class VersionChecker implements AppLifecycleListener {
    private static final Logger LOG = Logger.getInstance(VersionChecker.class);
    private static final String REPOSITORY_URL = "https://plugins.jetbrains.com/api/plugins";

    @Override
    public void appStarted() {
        ApplicationManager.getApplication().executeOnPooledThread(this::checkForUpdates);
    }

    private void checkForUpdates() {
        // 当前插件 ID
        PluginId pluginId = PluginId.getId(Constants.PLUGIN_ID);
        IdeaPluginDescriptor pluginDescriptor = PluginManagerCore.getPlugin(pluginId);
        if (null == pluginDescriptor) {
            return;
        }
        // 获取当前插件版本
        String currentVersion = pluginDescriptor.getVersion();
        try {
            // 从远程获取最新版本
            String latestVersion = fetchLatestVersionFromBeta();

            // 比较版本号
            if (isVersionNewer(latestVersion, currentVersion)) {
                String groupId = ToolWindowId.PROJECT_VIEW;
                Notification notification = new Notification(groupId,
                        Constants.PLUGIN_NAME + " " + I18N.get("version.update.title"),
                        I18N.get("version.update.message", latestVersion),
                        NotificationType.INFORMATION);
                // 添加一个跳转到网站的按钮
                notification.addAction(new NotificationAction(I18N.get("version.update.button")) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                        String url = "https://plugins.jetbrains.com/plugin/22013-bookmark-x/edit/versions/beta";
                        BrowserUtil.browse(url);
                        notification.expire();
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

    private String parseLatestVersionFromResponse(String response) {
        Gson gson = new Gson();
        List<Map<String, Object>> list = gson.fromJson(response, List.class);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        Map<String, Object> map = list.get(0);
        return MapUtils.getString(map, "version");
    }

    private boolean isVersionNewer(String latestVersion, String currentVersion) {
        return compareVersions(latestVersion, currentVersion) > 0;
    }

    /**
     * 比较两个版本号，支持后缀（如 -alpha, -beta）。
     *
     * @param version1 第一个版本号
     * @param version2 第二个版本号
     * @return 返回值：
     * 0：版本号相等
     * -1：version1 < version2
     * 1：version1 > version2
     * @throws IllegalArgumentException 如果版本号格式无效
     */
    public static int compareVersions(String version1, String version2) {
        if (version1 == null || version2 == null) {
            throw new IllegalArgumentException("版本号不能为空");
        }

        String[] parts1 = splitVersion(version1);
        String[] parts2 = splitVersion(version2);

        // 比较主版本号和数字部分
        int numericComparison = compareNumericParts(parts1[0], parts2[0]);
        if (numericComparison != 0) {
            return numericComparison;
        }

        // 比较后缀部分（预发布版本）
        return compareSuffix(parts1[1], parts2[1]);
    }

    /**
     * 拆分版本号为数字部分和后缀部分。
     *
     * @param version 版本号
     * @return [数字部分, 后缀部分]
     */
    private static String[] splitVersion(String version) {
        String[] result = new String[2];
        int index = version.indexOf('-');
        if (index == -1) {
            result[0] = version; // 没有后缀
            result[1] = "";      // 后缀为空
        } else {
            result[0] = version.substring(0, index);
            result[1] = version.substring(index + 1);
        }
        return result;
    }

    /**
     * 比较版本号的数字部分（如 1.0.0 和 1.0.1）。
     */
    private static int compareNumericParts(String numeric1, String numeric2) {
        String[] parts1 = numeric1.split("\\.");
        String[] parts2 = numeric2.split("\\.");
        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int v1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int v2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (v1 != v2) {
                return Integer.compare(v1, v2);
            }
        }
        return 0;
    }

    /**
     * 比较版本号的后缀部分（如 alpha, beta, rc）。
     */
    private static int compareSuffix(String suffix1, String suffix2) {
        // 如果没有后缀，视为正式版本，优先级最高
        if (suffix1.isEmpty() && suffix2.isEmpty()) {
            return 0;
        }
        if (suffix1.isEmpty()) {
            return 1; // 没有后缀的版本 > 带有后缀的版本
        }
        if (suffix2.isEmpty()) {
            return -1; // 带有后缀的版本 < 没有后缀的版本
        }

        // 自定义后缀优先级列表
        String[] priorities = {"alpha", "beta", "rc"};
        int index1 = Arrays.asList(priorities).indexOf(suffix1);
        int index2 = Arrays.asList(priorities).indexOf(suffix2);

        // 如果不在优先级列表中，按字典顺序比较
        if (index1 == -1 && index2 == -1) {
            return suffix1.compareTo(suffix2);
        }
        if (index1 == -1) {
            return 1; // 未知后缀优先级最低
        }
        if (index2 == -1) {
            return -1; // 未知后缀优先级最低
        }

        return Integer.compare(index1, index2);
    }
}
