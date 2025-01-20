package indi.bookmarkx.common.data;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import indi.bookmarkx.listener.BookmarkListener;
import indi.bookmarkx.model.AbstractTreeNodeModel;
import indi.bookmarkx.model.BookmarkNodeModel;
import indi.bookmarkx.ui.tree.BookmarkTree;
import indi.bookmarkx.ui.tree.BookmarkTreeNode;
import indi.bookmarkx.utils.PersistenceUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 将标签存进List
 *
 * @author: codeleep
 * @createTime: 2024/03/20 14:58
 * @description: 数组表格
 */
@Service(Service.Level.PROJECT)
public final class BookmarkArrayListTable extends ArrayListTable<BookmarkNodeModel> implements BookmarkListener {

    public static BookmarkArrayListTable getInstance(Project project) {
        return project.getService(BookmarkArrayListTable.class);
    }

    public BookmarkArrayListTable(Project project) {
        super(new ArrayList<>(), getColumnIndexFunctions());
        project.getMessageBus().connect().subscribe(TOPIC, this);
    }

    public void initData(BookmarkTree bookmarkPO) {
        this.dataList = treeToList(bookmarkPO, new ArrayList<>());
        columnIndices.keySet().forEach(super::addColumIndex);
    }

    private static List<Function<BookmarkNodeModel, String>> getColumnIndexFunctions() {
        ArrayList<Function<BookmarkNodeModel, String>> functions = new ArrayList<>();
        functions.add(lineColumnIndex());
        functions.add(uuIdColumnIndex());
        return functions;
    }

    private static Function<BookmarkNodeModel, String> lineColumnIndex() {
        return bookmarkPO -> {
            OpenFileDescriptor descriptor = bookmarkPO.getOpenFileDescriptor();
            if (null == descriptor) {
                return null;
            }
            return descriptor.getFile().getPath();
        };
    }

    private static Function<BookmarkNodeModel, String> uuIdColumnIndex() {
        return BookmarkNodeModel::getUuid;
    }


    private List<BookmarkNodeModel> treeToList(BookmarkTree bookmarkTree, List<BookmarkNodeModel> list) {
        if (bookmarkTree == null) {
            return list;
        }
        BookmarkTreeNode bookmarkTreeNode = (BookmarkTreeNode) bookmarkTree.getModel().getRoot();
        if (bookmarkTreeNode == null) {
            return list;
        }
        return PersistenceUtil.treeToList(bookmarkTreeNode);

    }


    @Override
    public void bookmarkChanged(@NotNull AbstractTreeNodeModel model) {
        if (model.isBookmark()) {
            this.insert((BookmarkNodeModel) model);
        }
    }

    @Override
    public void bookmarkRemoved(@NotNull AbstractTreeNodeModel model) {
        if (model.isBookmark()) {
            this.delete((BookmarkNodeModel) model);
        }
    }
}
