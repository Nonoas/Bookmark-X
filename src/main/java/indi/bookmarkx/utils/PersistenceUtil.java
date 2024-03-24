package indi.bookmarkx.utils;

import com.google.gson.Gson;
import com.intellij.openapi.project.Project;
import indi.bookmarkx.MyPersistent;
import indi.bookmarkx.model.AbstractTreeNodeModel;
import indi.bookmarkx.model.BookmarkConverter;
import indi.bookmarkx.model.GroupNodeModel;
import indi.bookmarkx.model.po.BookmarkPO;
import indi.bookmarkx.ui.tree.BookmarkTree;
import indi.bookmarkx.ui.tree.BookmarkTreeNode;
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
            AbstractTreeNodeModel model = BookmarkConverter.convertToModel(project, po);
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

    /**
     * 可以为 null
     */
    public static <T> T deepCopy(T object, Class<T> clazz) {
        if (null == object) {
            return null;
        }
        Gson gson = new Gson();
        String json = gson.toJson(object, clazz);
        return gson.fromJson(json, clazz);
    }

}
