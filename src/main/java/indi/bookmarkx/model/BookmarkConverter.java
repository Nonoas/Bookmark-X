package indi.bookmarkx.model;

import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import indi.bookmarkx.model.po.BookmarkPO;

/**
 * @author Nonoas
 * @date 2023/6/5
 */
public class BookmarkConverter {

    public static BookmarkPO convertToPO(AbstractTreeNodeModel model) {

        if (model instanceof BookmarkNodeModel) {
            BookmarkNodeModel nodeModel = (BookmarkNodeModel) model;

            BookmarkPO po = new BookmarkPO();
            po.setUuid(nodeModel.getUuid());
            po.setIndex(nodeModel.getIndex());
            po.setLine(nodeModel.getLine());
            po.setColumn(nodeModel.getColumn());
            po.setName(model.getName());
            po.setDesc(nodeModel.getDesc());
            po.setBookmark(true);

            VirtualFile file = nodeModel.getOpenFileDescriptor().getFile();
            po.setVirtualFilePath(file.getPath());
            return po;
        } else {
            GroupNodeModel nodeModel = (GroupNodeModel) model;

            BookmarkPO po = new BookmarkPO();
            po.setName(nodeModel.getName());
            po.setDesc(nodeModel.getDesc());
            po.setBookmark(false);

            return po;
        }

    }

    public static AbstractTreeNodeModel convertToModel(Project project, BookmarkPO po) {

        if (po.isBookmark()) {
            BookmarkNodeModel model = new BookmarkNodeModel();
            model.setUuid(po.getUuid());
            model.setIndex(po.getIndex());
            model.setLine(po.getLine());
            model.setColumn(po.getColumn());
            model.setName(po.getName());
            model.setDesc(po.getDesc());

            VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(po.getVirtualFilePath());
            if (null == virtualFile) {
                return model;
            }
            FileType fileType = virtualFile.getFileType();
            model.setIcon(fileType.getIcon());
            model.setOpenFileDescriptor(new OpenFileDescriptor(project, virtualFile, po.getLine(), po.getColumn()));
            return model;
        } else {
            GroupNodeModel model = new GroupNodeModel();
            model.setName(po.getName());
            model.setDesc(po.getDesc());
            return model;
        }
    }
}
