package indi.bookmarkx.ui.pannel;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.SideBorder;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import indi.bookmarkx.BookmarksManager;
import indi.bookmarkx.MySettingsConfigurable;
import indi.bookmarkx.common.data.BookmarkArrayListTable;
import indi.bookmarkx.global.FileMarksCache;
import indi.bookmarkx.listener.BookmarkListener;
import indi.bookmarkx.listener.SettingsListener;
import indi.bookmarkx.model.AbstractTreeNodeModel;
import indi.bookmarkx.model.BookmarkNodeModel;
import indi.bookmarkx.persistence.MySettings;
import indi.bookmarkx.ui.tree.BookmarkTree;
import indi.bookmarkx.ui.tree.BookmarkTreeNode;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

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

    private Project project;

    private BookmarksManagePanel(Project project) {
        this.project = project;
        tree = new BookmarkTree(project);
        setLayout(new BorderLayout());

        // 1. 初始化 Splitter
        JBSplitter jbSplitter = new JBSplitter(false, "BookmarkX.Splitter", 0.3f);
        JBScrollPane treeScrollPane = new JBScrollPane(tree);
        treeScrollPane.setBorder(JBUI.Borders.empty());
        jbSplitter.setFirstComponent(treeScrollPane);

        // 2. 初始化详情面板
        JBScrollPane descScrollPane = new JBScrollPane();
        descScrollPane.setBorder(IdeBorderFactory.createBorder(SideBorder.LEFT));

        // 3. 提取初始状态设置
        updateLayoutBasedOnSettings(jbSplitter, descScrollPane);

        // 4. 订阅设置变更
        project.getMessageBus().connect().subscribe(SettingsListener.TOPIC,
                () -> ApplicationManager.getApplication().invokeLater(() -> updateLayoutBasedOnSettings(jbSplitter, descScrollPane))
        );

        // 5. 监听树选择（比鼠标点击体验更好，支持键盘）
        tree.addTreeSelectionListener(e -> {
            if (MySettings.getInstance().getDescShowType() == MySettingsConfigurable.DescShowType.SPLIT_PANE) {
                refreshDescription(e.getPath(), descScrollPane);
            }
        });

        add(jbSplitter, BorderLayout.CENTER);
        setBorder(JBUI.Borders.empty(2));
    }

    private void updateLayoutBasedOnSettings(JBSplitter splitter, JBScrollPane descPane) {
        boolean isSplit = MySettings.getInstance().getDescShowType() == MySettingsConfigurable.DescShowType.SPLIT_PANE;
        splitter.setSecondComponent(isSplit ? descPane : null);
        if (isSplit) {
            refreshDescription(tree.getSelectionPath(), descPane);
        }
        splitter.revalidate();
        splitter.repaint();
    }

    private void refreshDescription(TreePath path, JBScrollPane scrollPane) {
        if (path == null) return;
        BookmarkTreeNode selectedNode = (BookmarkTreeNode) path.getLastPathComponent();
        if (null == selectedNode) {
            return;
        }
        Object userObject = selectedNode.getUserObject();
        if (userObject instanceof AbstractTreeNodeModel) {
            // 使用 setViewportView 是正确的，但可以考虑在这里加入策略模式切换布局
            scrollPane.setViewportView(new BookmarkTipPanel(project, (AbstractTreeNodeModel) userObject));
        }
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
        private final Project project;

        public TreeDataChangeListener(Project project) {
            manager = BookmarksManager.getInstance(project);
            this.project = project;
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

                refreshFile((BookmarkNodeModel) model);
            });
        }

        private void refreshFile(BookmarkNodeModel model) {
            VirtualFile virtualFile = model.getOpenFileDescriptor().getFile();
            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            if (null == psiFile) {
                return;
            }
            DaemonCodeAnalyzer daemonCodeAnalyzer = DaemonCodeAnalyzer.getInstance(project);
            daemonCodeAnalyzer.restart(psiFile);
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

                refreshFile((BookmarkNodeModel) model);
            });
        }
    }

}
