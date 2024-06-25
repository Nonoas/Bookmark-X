package indi.bookmarkx.common.data;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import indi.bookmarkx.model.AbstractTreeNodeModel;
import indi.bookmarkx.model.BookmarkNodeModel;
import indi.bookmarkx.ui.tree.BookmarkTree;
import indi.bookmarkx.ui.tree.BookmarkTreeNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author: codeleep
 * @createTime: 2024/03/20 14:58
 * @description: 数组表格
 */
@Service(Service.Level.PROJECT)
public final class BookmarkArrayListTable extends ArrayListTable<BookmarkNodeModel> {

    private final Project project;

    public static BookmarkArrayListTable getInstance(Project project) {
        return project.getService(BookmarkArrayListTable.class);
    }

    public BookmarkArrayListTable(Project project) {
        super(new ArrayList<>(), getColumnIndexFunctions());
        this.project = project;
    }

    public void initData(BookmarkTree bookmarkPO) {
        this.dataList = treeToList(bookmarkPO, new ArrayList<>());
        columnIndices.keySet().forEach(super::addColumIndex);
    }

    private static List<Function<BookmarkNodeModel, Object>> getColumnIndexFunctions() {
        ArrayList<Function<BookmarkNodeModel, Object>> functions = new ArrayList<>();
        functions.add(lineColumnIndex());
        functions.add(uuIdColumnIndex());
        return functions;
    }

    private static Function<BookmarkNodeModel, Object> lineColumnIndex() {
        return bookmarkPO -> bookmarkPO.getOpenFileDescriptor().getFile().getPath();
    }

    private static Function<BookmarkNodeModel, Object> uuIdColumnIndex() {
        return BookmarkNodeModel::getUuid;
    }


    private List<BookmarkNodeModel> treeToList(BookmarkTree bookmarkTree,List<BookmarkNodeModel> list) {
        if (bookmarkTree == null) {
            return list;
        }
        BookmarkTreeNode bookmarkTreeNode = (BookmarkTreeNode)bookmarkTree.getModel().getRoot();
        if (bookmarkTreeNode == null) {
            return list;
        }
        toList(bookmarkTreeNode, list);
        return list;

    }

    private static void toList(BookmarkTreeNode node, List<BookmarkNodeModel> list) {
        int childCount = node.getChildCount();
        AbstractTreeNodeModel model = (AbstractTreeNodeModel) node.getUserObject();
        if (0 == childCount) {
            return;
        }
        for (int i = 0; i < childCount; i++) {
            BookmarkTreeNode treeNode = (BookmarkTreeNode) node.getChildAt(i);
            Object userObject = treeNode.getUserObject();
            if (userObject instanceof BookmarkNodeModel) {
                list.add((BookmarkNodeModel)userObject);
            }
            toList(treeNode, list);
        }
    }


}
