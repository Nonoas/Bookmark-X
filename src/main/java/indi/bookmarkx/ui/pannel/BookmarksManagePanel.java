package indi.bookmarkx.ui.pannel;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import indi.bookmarkx.BookmarksManager;
import indi.bookmarkx.MyPersistent;
import indi.bookmarkx.common.data.BookmarkArrayListTable;
import indi.bookmarkx.model.BookmarkNodeModel;
import indi.bookmarkx.model.po.BookmarkPO;
import indi.bookmarkx.ui.tree.BookmarkTree;
import indi.bookmarkx.ui.tree.BookmarkTreeNode;
import indi.bookmarkx.utils.PersistenceUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.BorderLayout;

/**
 * 标签目录面板
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

        reInit(project);

    }

    public void reInit(Project project) {
        loadTree(project);
    }

    private void loadTree(Project project) {
        ProgressManager.getInstance().run(new TreeLoadTask(project, tree));
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

    class TreeLoadTask extends Task.Backgroundable {

        private final Project project;
        private final BookmarkTree tree;
        private DefaultTreeModel treeModel;

        public TreeLoadTask(Project project, BookmarkTree tree) {
            super(project, "Loading Tree Data");
            this.project = project;
            this.tree = tree;
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            try {
                MyPersistent persistent = MyPersistent.getInstance(project);
                BookmarkPO rootPO = persistent.getState();
                BookmarkTreeNode root = PersistenceUtil.generateTreeNode(rootPO, project);
                treeModel = new DefaultTreeModel(root);
            } catch (Exception e) {
                // 错误处理
                LOG.error("初始化标签树失败", e);
            }
            LOG.info("初始化标签树成功");
        }

        @Override
        public void onSuccess() {
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
                treeModel.nodeStructureChanged((TreeNode) treeModel.getRoot());

                BookmarkArrayListTable bookmarkArrayListTable = BookmarkArrayListTable.getInstance(project);
                bookmarkArrayListTable.initData(tree);
                treeLoaded = true;
            });
        }
    }

}
