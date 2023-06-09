package indi.bookmarkx;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import indi.bookmarkx.model.GroupNodeModel;
import indi.bookmarkx.model.AbstractTreeNodeModel;
import indi.bookmarkx.model.BookmarkConverter;
import indi.bookmarkx.model.BookmarkNodeModel;
import indi.bookmarkx.model.po.BookmarkPO;
import indi.bookmarkx.tree.BookmarkTree;
import indi.bookmarkx.tree.BookmarkTreeNode;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Nonoas
 * @date 2023/6/6
 */
public class PersistenceUtil {

    /**
     * 持久化保存
     */
    public static void persistentSave(Project project, BookmarkTree tree) {
        BookmarkPO po = getPersistenceObject(tree);
        MyPersistent persistent = MyPersistent.getInstance(project);
        persistent.setState(po);
        Application application = ApplicationManager.getApplication();
        application.saveSettings();
    }

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
            AbstractTreeNodeModel model =  BookmarkConverter.convertToModel(project, po);
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
