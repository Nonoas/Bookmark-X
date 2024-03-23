package indi.bookmarkx.action;

import com.google.gson.Gson;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import indi.bookmarkx.BookmarksManager;
import indi.bookmarkx.MyPersistent;
import indi.bookmarkx.common.I18N;
import indi.bookmarkx.model.po.BookmarkPO;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public final class BookmarkImportAction extends AnAction {

    private static final String ACTION_ID = I18N.get("bookmark.import");

    private Project project;

    public BookmarkImportAction() {
        super(ACTION_ID, null, AllIcons.ToolbarDecorator.Import);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(
                true,
                false,
                false,
                false,
                false,
                false);
        descriptor.withFileFilter(file -> file != null && file.getName().toLowerCase().endsWith(".json"));

        project = e.getProject();
        VirtualFile virtualFile = FileChooser.chooseFile(descriptor, project, null);
        if (null == virtualFile || null == project) {
            return;
        }
        try {
            String content = new String(virtualFile.contentsToByteArray());
            Gson gson = new Gson();
            BookmarkPO bookmarkPO = gson.fromJson(content, BookmarkPO.class);
            stateTranslate(bookmarkPO);
            MyPersistent persistent = MyPersistent.getInstance(project);
            persistent.setState(bookmarkPO);
            BookmarksManager.getInstance(project).reload();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
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
            po.setVirtualFilePath(virtualFilePath.replace("$PROJECT_DIR$", dir));
        }
        if (CollectionUtils.isNotEmpty(po.getChildren())) {
            for (BookmarkPO child : po.getChildren()) {
                stateTranslate(child, dir);
            }
        }
    }
}
