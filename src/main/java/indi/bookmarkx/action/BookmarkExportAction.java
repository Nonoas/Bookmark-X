package indi.bookmarkx.action;

import com.google.gson.Gson;
import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.openapi.wm.ToolWindowId;
import indi.bookmarkx.common.I18N;
import indi.bookmarkx.model.po.BookmarkPO;
import indi.bookmarkx.persistence.MyPersistent;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Objects;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static indi.bookmarkx.utils.PersistenceUtil.deepCopy;


/**
 * 书签导出
 *
 * @author Nonoas
 * @date 2024-1-31
 */
public final class BookmarkExportAction extends AnAction {

    // 移除了这行代码: private static String ACTION_ID = I18N.get("bookmark.export");
    // 因为它会在类加载时就调用服务，从而导致报错。

    private Project project;

    public BookmarkExportAction() {
        /*
         * ✅ 正确的修改方式：
         * 使用 Supplier (一个 Lambda 表达式) 来包装 I18N.get() 调用。
         * 这样，I18N.get("bookmark.export") 不会在构造函数执行时立即调用，
         * 而是等到 IntelliJ 平台真正需要显示这个 Action 的文本时，才会执行这个 Lambda。
         * 此时，平台已经完全初始化，可以安全地获取服务了。
         *
         * AnAction 的父构造函数接受一个 Supplier<String> 作为动态文本。
         */
        super(() -> I18N.get("bookmark.export"), () -> null, AllIcons.ToolbarDecorator.Export);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        project = e.getProject();
        if (null == project) {
            return;
        }
        // 在 actionPerformed 方法中获取服务是安全的，这里无需改动。
        MyPersistent persistent = MyPersistent.getInstance(project);
        BookmarkPO state = persistent.getState();

        BookmarkPO copy = deepCopy(state, BookmarkPO.class);
        stateTranslate(copy);

        // 创建文件保存对话框
        FileSaverDescriptor descriptor = new FileSaverDescriptor(
                I18N.get("bookmark.export.dialog.title"),
                I18N.get("bookmark.export.dialog.description"),
                "json"
        );

        String defaultFileName = "Bookmark_X_" + DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now());
        FileSaverDialog saveFileDialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project);

        String basePath = project.getBasePath();
        if (basePath == null) {
            basePath = System.getProperty("user.home");
        }
        VirtualFileWrapper fileWrapper = saveFileDialog.save(Path.of(basePath), defaultFileName);
        if (fileWrapper != null) {
            File file = fileWrapper.getFile();
            saveToJsonFile(copy, file.getAbsolutePath());
        }
    }

    public void saveToJsonFile(BookmarkPO state, String outputPath) {
        BookmarkPO copy = deepCopy(state, BookmarkPO.class);
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
        // 在这里调用 I18N.get() 也是安全的，因为它是在 Action 被触发后执行的。
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
