package indi.worktool;

import com.intellij.openapi.project.Project;
import indi.worktool.model.GroupNodeModel;
import indi.worktool.model.AbstractTreeNodeModel;
import indi.worktool.model.BookmarkConverter;
import indi.worktool.model.BookmarkNodeModel;
import indi.worktool.model.po.BookmarkPO;
import indi.worktool.tree.BookmarkTree;
import indi.worktool.tree.BookmarkTreeNode;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Nonoas
 * @date 2023/6/6
 */
public class PersistenceUtil {

    public static BookmarkPO getPersistenceObject(BookmarkTree tree) {
        BookmarkTreeNode rootNode = (BookmarkTreeNode) tree.getModel().getRoot();
        return covertToPO(rootNode);
    }

    private static BookmarkPO covertToPO(BookmarkTreeNode node) {

        int childCount = node.getChildCount();
        AbstractTreeNodeModel model = (AbstractTreeNodeModel) node.getUserObject();
        BookmarkPO po = BookmarkConverter.convertToPO(model);

        if (0 == childCount) {
            return po;
        }

        List<BookmarkPO> children = new ArrayList<>();
        BookmarkTreeNode child;
        for (int i = 0; i < childCount; i++) {
            child = (BookmarkTreeNode) node.getChildAt(i);
            children.add(covertToPO(child));
        }
        po.setChildren(children);
        return po;
    }

    public static BookmarkTreeNode generateTreeNode(BookmarkPO po, Project project) {
        if (po.isBookmark()) {
            BookmarkNodeModel model = (BookmarkNodeModel) BookmarkConverter.convertToModel(project, po);
            return new BookmarkTreeNode(model);
        }

        GroupNodeModel model = (GroupNodeModel) BookmarkConverter.convertToModel(project, po);
        BookmarkTreeNode node = new BookmarkTreeNode(model);

        List<BookmarkPO> children = po.getChildren();
        if (CollectionUtils.isEmpty(children)) {
            return node;
        }
        for (BookmarkPO child : children) {
            node.add(generateTreeNode(child, project));
        }
        return node;
    }

}
