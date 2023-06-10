package indi.bookmarkx;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import indi.bookmarkx.model.BookmarkNodeModel;
import indi.bookmarkx.model.po.BookmarkPO;
import indi.bookmarkx.tree.BookmarkTree;
import indi.bookmarkx.tree.BookmarkTreeNode;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.util.concurrent.ExecutionException;

/**
 * 标签目录面板
 *
 * @author Nonoas
 * @date 2023/6/1
 */
public class BookmarksManagePanel extends JPanel {

    private final BookmarkTree tree;

    private final Project project;

    /**
     * 标记 tree 是否已经从持久化文件加载完成
     */
    private boolean treeLoaded = false;

    private final JEditorPane jepDesc = new JEditorPane();

    private BookmarksManagePanel(Project project) {

        this.project = project;

        tree = new BookmarkTree(project);

        setLayout(new BorderLayout());

        JBScrollPane scrollPane = new JBScrollPane(tree);

        jepDesc.setBorder(JBUI.Borders.customLineTop(UIUtil.getSeparatorShadow()));
        jepDesc.setEditable(false);
        scrollPane.setBorder(JBUI.Borders.empty());

        add(scrollPane, BorderLayout.CENTER);
        add(jepDesc, BorderLayout.SOUTH);

        // 设置边框样式
        setBorder(JBUI.Borders.empty(2));

        // 设置背景色
        setBackground(JBColor.WHITE);

        reInit(project);

    }

    public void reInit(Project project) {
        loadTree(project);
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

    /**
     * 创建一个属于项目 project 的标签管理面板
     *
     * @param project 项目
     * @return 一个属于项目 project 的标签管理面板实例
     */
    public static BookmarksManagePanel create(Project project) {
        return new BookmarksManagePanel(project);
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
                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            BookmarksManager manager = BookmarksManager.getInstance(project);
                            manager.persistentSave();
                            return null;
                        }
                    }.execute();
                }
            });

            tree.setModel(treeModel);
            treeModel.nodeStructureChanged((TreeNode) treeModel.getRoot());

            tree.addTreeSelectionListener(event -> {
                BookmarkTreeNode selectedNode = (BookmarkTreeNode) tree.getLastSelectedPathComponent();
                if (selectedNode != null && selectedNode.isBookmark()) {
                    BookmarkNodeModel bookmark = (BookmarkNodeModel) selectedNode.getUserObject();
                    jepDesc.setText(bookmark.getDesc());
                } else {
                    jepDesc.setText("");
                }
            });

            treeLoaded = true;
        }
    }

}
