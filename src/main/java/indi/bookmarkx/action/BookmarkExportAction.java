package indi.bookmarkx.action;

import com.google.gson.Gson;
import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindowId;
import indi.bookmarkx.MyPersistent;
import indi.bookmarkx.common.I18N;
import indi.bookmarkx.model.po.BookmarkPO;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

import static indi.bookmarkx.utils.PersistenceUtil.deepCopy;

/**
 * 书签导出
 *
 * @author Nonoas
 * @date 2024-1-31
 */
public final class BookmarkExportAction extends AnAction {

    private static final String ACTION_ID = I18N.get("bookmark.export");

    private Project project;

    public BookmarkExportAction() {
        super(ACTION_ID, null, AllIcons.ToolbarDecorator.Export);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        project = e.getProject();
        if (null == project) {
            return;
        }
        MyPersistent persistent = MyPersistent.getInstance(project);
        BookmarkPO state = persistent.getState();

        BookmarkPO copy = deepCopy(state, BookmarkPO.class);
        stateTranslate(copy);
        saveToJsonFile(copy);
    }

    public void saveToJsonFile(BookmarkPO state) {
        BookmarkPO copy = deepCopy(state, BookmarkPO.class);
        String projectDir = FileUtil.toSystemIndependentName(Objects.requireNonNull(project.getBasePath()));
        String outputPath = projectDir + File.separator + "Bookmark_X.json";
        Gson gson = new Gson();
        try (FileWriter fw = new FileWriter(outputPath)) {
            gson.toJson(copy, fw);
        } catch (IOException e) {
            throw new RuntimeException("export error", e);
        }
        success(outputPath);
    }

    private void success(String outputPath) {
        String groupId = ToolWindowId.PROJECT_VIEW;
        Notification notification = new Notification(groupId,
                I18N.get("bookmark.notification.title"),
                I18N.get("bookmark.export.success", outputPath),
                NotificationType.INFORMATION);
        Notifications.Bus.notify(notification, project);
    }

    private void stateTranslate(BookmarkPO po) {
        String basePath = project.getBasePath();
        if (null == basePath) {
            return;
        }
        String projectDir = FileUtil.toSystemIndependentName(basePath);
        stateTranslate(po, projectDir);
    }

    private void stateTranslate(BookmarkPO po, String dir) {
        String virtualFilePath = po.getVirtualFilePath();
        if (StringUtil.isNotEmpty(virtualFilePath)) {
            po.setVirtualFilePath(virtualFilePath.replace(dir, "$PROJECT_DIR$"));
        }
        if (CollectionUtils.isNotEmpty(po.getChildren())) {
            for (BookmarkPO child : po.getChildren()) {
                stateTranslate(child, dir);
            }
        }
    }

}
