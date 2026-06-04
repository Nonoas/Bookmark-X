package indi.bookmarkx.global;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import indi.bookmarkx.BookmarksManager;
import indi.bookmarkx.common.data.BookmarkArrayListTable;
import indi.bookmarkx.model.BookmarkNodeModel;

import java.util.List;
import java.util.Objects;

public class BookmarkReconciler {

    private static final int NEARBY_RANGE = 50;

    public static void reconcile(Project project) {
        BookmarkArrayListTable table = BookmarkArrayListTable.getInstance(project);
        List<BookmarkNodeModel> allBookmarks = table.getAll();
        if (allBookmarks == null || allBookmarks.isEmpty()) {
            return;
        }

        for (BookmarkNodeModel bookmark : allBookmarks) {
            reconcileOne(project, bookmark);
        }

        BookmarksManager.getInstance(project).persistentSave();
    }

    private static void reconcileOne(Project project, BookmarkNodeModel bookmark) {
        OpenFileDescriptor descriptor = bookmark.getOpenFileDescriptor();
        if (descriptor == null) {
            return;
        }

        VirtualFile file = descriptor.getFile();
        if (!file.isValid()) {
            return;
        }

        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) {
            return;
        }

        String signature = bookmark.getLineSignature();
        int originalLine = bookmark.getLine();
        int lineCount = document.getLineCount();

        if (signature == null || signature.isEmpty()) {
            int clampedLine = Math.min(originalLine, Math.max(0, lineCount - 1));
            refreshBookmark(project, bookmark, clampedLine, file);
            return;
        }

        if (originalLine < lineCount) {
            String currentContent = getLineTrimmed(document, originalLine);
            if (Objects.equals(signature, currentContent)) {
                refreshBookmark(project, bookmark, originalLine, file);
                return;
            }
        }

        int foundLine = searchNearby(document, originalLine, signature, NEARBY_RANGE);
        if (foundLine >= 0) {
            refreshBookmark(project, bookmark, foundLine, file);
            return;
        }

        foundLine = searchFullDocument(document, signature);
        if (foundLine >= 0) {
            refreshBookmark(project, bookmark, foundLine, file);
            return;
        }

        int clampedLine = Math.min(originalLine, Math.max(0, lineCount - 1));
        refreshBookmark(project, bookmark, clampedLine, file);
    }

    private static String getLineTrimmed(Document document, int line) {
        int lineStart = document.getLineStartOffset(line);
        int lineEnd = document.getLineEndOffset(line);
        return document.getText(new TextRange(lineStart, lineEnd)).trim();
    }

    private static int searchNearby(Document document, int originalLine, String signature, int range) {
        int lineCount = document.getLineCount();
        for (int offset = 1; offset <= range; offset++) {
            int above = originalLine - offset;
            if (above >= 0 && above < lineCount) {
                if (Objects.equals(signature, getLineTrimmed(document, above))) {
                    return above;
                }
            }
            int below = originalLine + offset;
            if (below >= 0 && below < lineCount) {
                if (Objects.equals(signature, getLineTrimmed(document, below))) {
                    return below;
                }
            }
        }
        return -1;
    }

    private static int searchFullDocument(Document document, String signature) {
        int lineCount = document.getLineCount();
        for (int i = 0; i < lineCount; i++) {
            if (Objects.equals(signature, getLineTrimmed(document, i))) {
                return i;
            }
        }
        return -1;
    }

    private static void refreshBookmark(Project project, BookmarkNodeModel bookmark, int newLine, VirtualFile file) {
        bookmark.setLine(newLine);
        bookmark.setOpenFileDescriptor(new OpenFileDescriptor(project, file, newLine, 0));
        bookmark.release();
        bookmark.createLineMarker();
    }
}