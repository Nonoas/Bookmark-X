package indi.bookmarkx.ui.painter;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.EditorLinePainter;
import com.intellij.openapi.editor.LineExtensionInfo;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import indi.bookmarkx.MyPersistent;
import indi.bookmarkx.common.data.BookmarkArrayListTable;
import indi.bookmarkx.model.BookmarkNodeModel;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author: codeleep
 * @createTime: 2023/12/14 23:37
 * @description:
 */
public class LineEndPainter extends EditorLinePainter {

    private static final Logger LOG = Logger.getInstance(MyPersistent.class);

    private BookmarkArrayListTable bookmarkArrayListTable;

    private Project project;

    @Override
    public Collection<LineExtensionInfo> getLineExtensions(Project project,  VirtualFile virtualFile, int i) {
        if (this.project != project || bookmarkArrayListTable == null) {
            this.project = project;
            bookmarkArrayListTable = BookmarkArrayListTable.getInstance(project);
        }
        List<LineExtensionInfo> result = new ArrayList<>();
        try {
            List<BookmarkNodeModel> onlyIndex = bookmarkArrayListTable.getOnlyIndex(virtualFile.getPath());
            BookmarkNodeModel bookmarkNodeModel = LineEndPainter.findLine(onlyIndex, i);
            if (bookmarkNodeModel == null) {
                return null;
            }
            result.add(new LineExtensionInfo(String.format(" // %s", bookmarkNodeModel.getName()),
                    new TextAttributes(null, null, JBColor.GRAY, null, Font.PLAIN)));
            return result;
        } catch (Exception e) {
            LOG.error("渲染行尾注释失败 path:" + virtualFile.getPath(), e);
        }
        return null;
    }

    public static BookmarkNodeModel findLine(List<BookmarkNodeModel> list, int i) {
        if (list == null || list.isEmpty()){
            return null;
        }
        Optional<BookmarkNodeModel> bookmarkNodeModel1 = list.stream()
                .filter(bookmarkNodeModel -> bookmarkNodeModel.getLine() == i)
                .findFirst();
        return bookmarkNodeModel1.orElse(null);
    }

}

