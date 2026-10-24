package indi.bookmarkx.service.relocation;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import indi.bookmarkx.model.BookmarkNodeModel;

import java.util.Collections;
import java.util.List;

public final class RelocationContext {

    private final Project project;
    private final VirtualFile file;
    private final BookmarkNodeModel bookmark;
    private final List<String> baseLines;
    private final List<String> currentLines;

    public RelocationContext(Project project,
                             VirtualFile file,
                             BookmarkNodeModel bookmark,
                             List<String> baseLines,
                             List<String> currentLines) {
        this.project = project;
        this.file = file;
        this.bookmark = bookmark;
        this.baseLines = baseLines == null ? Collections.emptyList() : baseLines;
        this.currentLines = currentLines == null ? Collections.emptyList() : currentLines;
    }

    public Project getProject() {
        return project;
    }

    public VirtualFile getFile() {
        return file;
    }

    public BookmarkNodeModel getBookmark() {
        return bookmark;
    }

    public List<String> getBaseLines() {
        return baseLines;
    }

    public List<String> getCurrentLines() {
        return currentLines;
    }
}
