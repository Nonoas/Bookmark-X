package indi.bookmarkx.global;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.impl.EditorFactoryImpl;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import indi.bookmarkx.BookmarksManager;
import indi.bookmarkx.MyPersistent;
import indi.bookmarkx.common.data.BookmarkArrayListTable;
import indi.bookmarkx.model.BookmarkNodeModel;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: codeleep
 * @createTime: 2024/03/20 19:44
 * @description: 文档变化监听
 */
public class BookmarkDocumentListener implements DocumentListener {

    private static final Logger LOG = Logger.getInstance(MyPersistent.class);

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        try {
            Document document = event.getDocument();

            VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
            Editor editor = getEditor(document);

            if (virtualFile == null || editor == null) {
                return;
            }

            Project project = editor.getProject();
            if (project == null) {
                return;
            }
            BookmarkArrayListTable bookmarkArrayListTable = BookmarkArrayListTable.getInstance(project);
            List<BookmarkNodeModel> indexList = bookmarkArrayListTable.getOnlyIndex(virtualFile.getPath());
            // 空的直接返回
            if (CollectionUtils.isEmpty(indexList)) {
                return;
            }

            perceivedLineChange(project, indexList);
        } catch (Exception e) {
            LOG.info("perceivedLineChange error", e);
        }
    }


    private void perceivedLineChange(Project project, List<BookmarkNodeModel> indexList) {
        if (CollectionUtils.isEmpty(indexList)) {
            return;
        }
        BookmarkArrayListTable bookmarkArrayListTable = BookmarkArrayListTable.getInstance(project);
        BookmarksManager bookmarksManager = BookmarksManager.getInstance(project);
        List<BookmarkNodeModel> removeList = new ArrayList<>();
        Document document;
        for (BookmarkNodeModel node : indexList) {
            if (node == null) {
                continue;
            }
            OpenFileDescriptor descriptor = node.getOpenFileDescriptor();
            RangeMarker rangeMarker = descriptor.getRangeMarker();

            if (null == rangeMarker || !rangeMarker.isValid()) {
                // 移除行尾描述信息
                removeList.add(node);
            } else {
                document = rangeMarker.getDocument();
                int line = document.getLineNumber(rangeMarker.getStartOffset());
                node.setLine(line);
            }
        }
        removeList.forEach(bookmarkArrayListTable::delete);
        bookmarksManager.persistentSave();
    }

    private Editor getEditor(Document document) {
        Editor[] editors = EditorFactoryImpl.getInstance().getEditors(document);
        if (editors.length >= 1) {
            return editors[0];
        }
        return null;
    }

}
