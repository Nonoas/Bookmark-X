package indi.worktool;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import indi.worktool.model.BookmarkNodeModel;
import indi.worktool.model.po.BookmarkPO;
import indi.worktool.tree.BookmarkTree;
import indi.worktool.tree.BookmarkTreeNode;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ExecutionException;

/**
 * 标签目录面板
 *
 * @author Nonoas
 * @date 2023/6/1
 */
public class BookmarksManagePanel extends JPanel {

    private static volatile BookmarksManagePanel INSTANCE;

    private final BookmarkTree tree = new BookmarkTree();

    /**
     * 标记 tree 是否已经从持久化文件加载完成
     */
    private boolean treeLoaded = false;

    private final JEditorPane jepDesc = new JEditorPane();

    private BookmarksManagePanel(Project project) {
        setLayout(new BorderLayout());

        JBScrollPane scrollPane = new JBScrollPane(tree);

        loadTree(project);

        jepDesc.setBorder(JBUI.Borders.customLineTop(UIUtil.getSeparatorShadow()));
        jepDesc.setEditable(false);
        scrollPane.setBorder(JBUI.Borders.empty());

        add(scrollPane, BorderLayout.CENTER);
        add(jepDesc, BorderLayout.SOUTH);

        // 设置边框样式
        setBorder(JBUI.Borders.empty(2));

        // 设置背景色
        setBackground(JBColor.WHITE);

    }

    private void loadTree(Project project) {
        new TreeLoadWorker(project, tree).execute();
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

    public static BookmarksManagePanel getInstance(Project project) {
        if (null == INSTANCE) {
            synchronized (BookmarksManagePanel.class) {
                if (null == INSTANCE) {
                    INSTANCE = new BookmarksManagePanel(project);
                }
            }
        }
        return INSTANCE;
    }

    public static BookmarksManagePanel getInstance() {
        return INSTANCE;
    }

    public class TreeLoadWorker extends SwingWorker<DefaultTreeModel, Void> {

        private final Project project;

        private final BookmarkTree tree;

        TreeLoadWorker(Project project, BookmarkTree tree) {
            this.tree = tree;
            this.project = project;
        }

        @Override
        protected DefaultTreeModel doInBackground() throws Exception {
            MyPersistent persistent = MyPersistent.getInstance(project);
            BookmarkPO rootPO = persistent.getState();
            BookmarkTreeNode root = PersistenceUtil.generateTreeNode(rootPO, project);
            System.out.println("doInBackground获取" + root);
            return new DefaultTreeModel(root);
        }

        @Override
        protected void done() {
            DefaultTreeModel treeModel;
            try {
                treeModel = get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                return;
            }

            treeModel.addTreeModelListener(new TreeModelListener() {
                @Override
                public void treeNodesChanged(TreeModelEvent e) {
                    persistenceSave();
                }

                @Override
                public void treeNodesInserted(TreeModelEvent e) {
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
                    BookmarkPO po = PersistenceUtil.getPersistenceObject(tree);
                    BookmarksManager.persistentSave(po);
                }
            });

            tree.setModel(treeModel);
            treeModel.nodeStructureChanged((TreeNode) treeModel.getRoot());

            tree.addTreeSelectionListener(event -> {
                BookmarkTreeNode selectedNode = (BookmarkTreeNode) tree.getLastSelectedPathComponent();
                if (selectedNode != null && selectedNode.isBookmark()) {
                    BookmarkNodeModel bookmark = (BookmarkNodeModel) selectedNode.getUserObject();
                    jepDesc.setText(bookmark.getDesc());
                }
            });

            treeLoaded = true;
        }
    }

}
