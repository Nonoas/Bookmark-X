package indi.bookmarkx.ui.pannel;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import indi.bookmarkx.BookmarksManager;
import indi.bookmarkx.common.data.BookmarkArrayListTable;
import indi.bookmarkx.global.FileMarksCache;
import indi.bookmarkx.listener.BookmarkListener;
import indi.bookmarkx.model.AbstractTreeNodeModel;
import indi.bookmarkx.model.BookmarkNodeModel;
import indi.bookmarkx.ui.tree.BookmarkTree;
import indi.bookmarkx.ui.tree.BookmarkTreeNode;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;

/**
 * 标签树目录面板
 *
 * @author Nonoas
 * @date 2023/6/1
 */
public class BookmarksManagePanel extends JPanel {

    private static final Logger LOG = Logger.getInstance(BookmarksManagePanel.class);

    private final BookmarkTree tree;

    /**
     * 标记 tree 是否已经从持久化文件加载完成
     */
    private volatile boolean treeLoaded = false;

    private BookmarksManagePanel(Project project) {

        tree = new BookmarkTree(project);

        setLayout(new BorderLayout());

        JBScrollPane scrollPane = new JBScrollPane(tree);

        scrollPane.setBorder(JBUI.Borders.empty());

        add(scrollPane, BorderLayout.CENTER);

        // 设置边框样式
        setBorder(JBUI.Borders.empty(2));

        // 设置背景色
        setBackground(JBColor.WHITE);

    }

    public void reInit(DefaultTreeModel treeModel, Project project) {
        loadTree(treeModel, project);
    }

    private void loadTree(DefaultTreeModel treeModel, Project project) {
        if (treeModel == null) {
            return;
        }
        treeModel.addTreeModelListener(new TreeModelListener() {
            @Override
            public void treeNodesChanged(TreeModelEvent e) {
                persistenceSave();
            }

            @Override
            public void treeNodesInserted(TreeModelEvent e) {
                // printEventDetails(e);
                persistenceSave();
            }

            @Override
            public void treeNodesRemoved(TreeModelEvent e) {
                persistenceSave();
            }

            @Override
            public void treeStructureChanged(TreeModelEvent e) {
                // do nothing
            }

            private void persistenceSave() {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    BookmarksManager manager = BookmarksManager.getInstance(project);
                    manager.persistentSave();
                });
            }


        });

        ApplicationManager.getApplication().invokeLater(() -> {
            tree.setModel(treeModel);
            project.getMessageBus().connect().subscribe(TreeDataChangeListener.TOPIC, new TreeDataChangeListener(project));
            treeModel.nodeStructureChanged((TreeNode) treeModel.getRoot());
            BookmarkArrayListTable bookmarkArrayListTable = BookmarkArrayListTable.getInstance(project);
            bookmarkArrayListTable.initData(tree);
            treeLoaded = true;
        });
    }

    public void prev() {
        BookmarkTree.GroupNavigator navigator = tree.getGroupNavigator();
        navigator.pre();
    }

    public void next() {
        BookmarkTree.GroupNavigator navigator = tree.getGroupNavigator();
        navigator.next();
    }

    public void addAndGet(BookmarkNodeModel bookmarkModel) {
        if (!treeLoaded) {
            return;
        }
        BookmarkTreeNode treeNode = new BookmarkTreeNode(bookmarkModel);
        tree.add(treeNode);
        bookmarkModel.setIndex(treeNode.getParent().getIndex(treeNode));
    }

    public BookmarkTree tree() {
        return tree;
    }

    public void treeNodesChanged(BookmarkNodeModel model) {
        BookmarkTreeNode nodeByModel = tree.getNodeByModel(model);
        tree.getModel().nodeChanged(nodeByModel);
    }

    /**
     * 创建一个属于项目 project 的标签管理面板
     *
     * @param project 项目
     * @return 一个属于项目 project 的标签管理面板实例
     */
    public static BookmarksManagePanel create(Project project) {
        return new BookmarksManagePanel(project);
    }

    public static class TreeDataChangeListener implements BookmarkListener {
        private final BookmarksManager manager;

        public TreeDataChangeListener(Project project) {
            manager = BookmarksManager.getInstance(project);
        }

        @Override
        public void bookmarkAdded(@NotNull AbstractTreeNodeModel model) {
            if (model.isGroup()) {
                return;
            }
            BookmarkNodeModel bookmarkNodeModel = (BookmarkNodeModel) model;
            bookmarkNodeModel.getFilePath().ifPresent(e -> {
                FileMarksCache fileMarksCache = manager.getFileMarksCache();
                fileMarksCache.addBookMark((BookmarkNodeModel) model);
            });
        }

        @Override
        public void bookmarkRemoved(@NotNull AbstractTreeNodeModel model) {
            if (model.isGroup()) {
                return;
            }
            BookmarkNodeModel bookmarkNodeModel = (BookmarkNodeModel) model;
            bookmarkNodeModel.getFilePath().ifPresent(e -> {
                FileMarksCache fileMarksCache = manager.getFileMarksCache();
                fileMarksCache.deleteBookMark((BookmarkNodeModel) model);
            });
        }
    }

}
