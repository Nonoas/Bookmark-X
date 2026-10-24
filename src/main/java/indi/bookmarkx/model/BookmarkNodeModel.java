package indi.bookmarkx.model;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.reference.SoftReference;
import indi.bookmarkx.BookmarksManager;
import indi.bookmarkx.common.Constants;
import indi.bookmarkx.ui.MyGutterIconRenderer;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Optional;

/**
 * 书签数据模型
 *
 * @author Nonoas
 * @date 2023/6/4
 */
public class BookmarkNodeModel extends AbstractTreeNodeModel {

    private int index;
    private int line;

    private Icon icon;

    private String anchorLineText;

    private String anchorLineHash;

    private String beforeContextHash;

    private String afterContextHash;

    private String snapshotId;

    private String baseContentHash;

    private String gitRootPath;

    private String gitRelativePath;

    private String gitCommit;

    /**
     * 文件跳转器
     */
    private OpenFileDescriptor openFileDescriptor;

    private Reference<RangeHighlighter> refHighlighter;

    public BookmarkNodeModel() {
    }

    public OpenFileDescriptor getOpenFileDescriptor() {
        return openFileDescriptor;
    }

    public void setOpenFileDescriptor(OpenFileDescriptor openFileDescriptor) {
        this.openFileDescriptor = openFileDescriptor;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getLine() {
        return line;
    }

    /**
     * 设置行号值。
     * 注意：此方法仅更新行号字段，如需同步更新行标记（Gutter Icon）和 openFileDescriptor，
     * 请使用 {@link #updateBookmarkLine(int, boolean)}
     *
     * @param newLine 新行号值（从0开始）
     */
    public void setLine(int newLine) {
        this.line = newLine;
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    public String getAnchorLineText() {
        return anchorLineText;
    }

    public void setAnchorLineText(String anchorLineText) {
        this.anchorLineText = anchorLineText;
    }

    public String getAnchorLineHash() {
        return anchorLineHash;
    }

    public void setAnchorLineHash(String anchorLineHash) {
        this.anchorLineHash = anchorLineHash;
    }

    public String getBeforeContextHash() {
        return beforeContextHash;
    }

    public void setBeforeContextHash(String beforeContextHash) {
        this.beforeContextHash = beforeContextHash;
    }

    public String getAfterContextHash() {
        return afterContextHash;
    }

    public void setAfterContextHash(String afterContextHash) {
        this.afterContextHash = afterContextHash;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getBaseContentHash() {
        return baseContentHash;
    }

    public void setBaseContentHash(String baseContentHash) {
        this.baseContentHash = baseContentHash;
    }

    public String getGitRootPath() {
        return gitRootPath;
    }

    public void setGitRootPath(String gitRootPath) {
        this.gitRootPath = gitRootPath;
    }

    public String getGitRelativePath() {
        return gitRelativePath;
    }

    public void setGitRelativePath(String gitRelativePath) {
        this.gitRelativePath = gitRelativePath;
    }

    public String getGitCommit() {
        return gitCommit;
    }

    public void setGitCommit(String gitCommit) {
        this.gitCommit = gitCommit;
    }

    @Override
    public final boolean isBookmark() {
        return true;
    }

    public Optional<String> getFilePath() {
        return Optional.ofNullable(openFileDescriptor)
                .map(OpenFileDescriptor::getFile)
                .map(VirtualFile::getPath);
    }

    public RangeHighlighter findMyHighlighter() {
        Document document = getCachedDocument();
        if (document == null) return null;
        RangeHighlighter result = SoftReference.dereference(refHighlighter);
        if (BookmarkLineMarkers.belongsTo(result, this)) {
            return result;
        }
        MarkupModelEx markup = (MarkupModelEx) DocumentMarkupModel.forDocument(document, openFileDescriptor.getProject(), false);
        result = markup == null ? null : BookmarkLineMarkers.findForModel(markup.getAllHighlighters(), this);
        refHighlighter = result == null ? null : new WeakReference<>(result);
        return result;
    }

    @Nullable
    public Document getCachedDocument() {
        OpenFileDescriptor descriptor = openFileDescriptor;
        if (descriptor == null) {
            return null;
        }
        return FileDocumentManager.getInstance().getCachedDocument(descriptor.getFile());
    }

    public void release() {
        RangeHighlighter cached = SoftReference.dereference(refHighlighter);
        refHighlighter = null;
        if (BookmarkLineMarkers.belongsTo(cached, this)) {
            cached.dispose();
        }
        final Document document = getCachedDocument();
        if (document == null) return;
        MarkupModelEx markup = (MarkupModelEx) DocumentMarkupModel.forDocument(document, openFileDescriptor.getProject(), false);
        if (markup != null) {
            BookmarkLineMarkers.disposeForModel(markup.getAllHighlighters(), this);
        }
    }

    public void createLineMarker() {
        RangeHighlighter myHighlighter = findMyHighlighter();

        if (myHighlighter != null) {
            return;
        }
        Document document = getCachedDocument();
        if (null == document) {
            return;
        }
        Optional.ofNullable(openFileDescriptor)
                .map(OpenFileDescriptor::getProject)
                .ifPresent(project -> {
                    MarkupModelEx markupModel = (MarkupModelEx) DocumentMarkupModel.forDocument(document, project, true);
                    RangeHighlighterEx bkx = markupModel.addPersistentLineHighlighter(Constants.TK_BOOKMARK_X, getLine(), HighlighterLayer.ERROR + 1);
                    if (bkx == null) {
                        return;
                    }
                    bkx.setGutterIconRenderer(new MyGutterIconRenderer(this));
                });

    }

    public void updateBookmarkLine(int newLine, boolean doPersistentSave) {
        if (this.line == newLine) {
            return;
        }
        OpenFileDescriptor oldDescriptor = getOpenFileDescriptor();
        if (oldDescriptor == null) {
            // openFileDescriptor 为 null 时，只更新行号，不操作行标记
            this.line = newLine;
            return;
        }
        this.line = newLine;
        this.setOpenFileDescriptor(
                new OpenFileDescriptor(
                        oldDescriptor.getProject(),
                        oldDescriptor.getFile(),
                        newLine,
                        0
                )
        );
        this.release();
        this.createLineMarker();
        if (doPersistentSave) {
            BookmarksManager.getInstance(oldDescriptor.getProject()).persistentSave();
        }
    }
}
