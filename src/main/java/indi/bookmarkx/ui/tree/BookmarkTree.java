package indi.bookmarkx.ui.tree;

import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.treeStructure.Tree;
import indi.bookmarkx.common.I18N;
import indi.bookmarkx.common.data.BookmarkArrayListTable;
import indi.bookmarkx.model.AbstractTreeNodeModel;
import indi.bookmarkx.model.BookmarkNodeModel;
import indi.bookmarkx.model.GroupNodeModel;
import indi.bookmarkx.ui.dialog.BookmarkCreatorDialog;
import indi.bookmarkx.ui.pannel.BookmarkTipPanel;
import org.apache.commons.lang3.Validate;

import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 书签树
 *
 * @author Nonoas
 * @date 2023/6/1
 */
public class BookmarkTree extends Tree {

    /**
     * BookmarkTreeNode 缓存，便于通过 UUID 直接取到节点引用
     */
    private final Map<String, BookmarkTreeNode> nodeCache = new HashMap<>();

    private final GroupNavigator navigator = new GroupNavigator(this);

    private DefaultTreeModel model;

    private Project project;

    private BookmarkArrayListTable bookmarkArrayListTable;

    public BookmarkTree(Project project) {
        super();
        initData(project);
        initView();
        initDragHandler();
        initCellRenderer();
        initTreeListeners();
        initContextMenu();
    }

    private void initView() {
        TreeSpeedSearch treeSpeedSearch = new TreeSpeedSearch(this);
        treeSpeedSearch.setCanExpand(true);

        setShowsRootHandles(true);
    }

    private void initData(Project project) {
        this.project = project;
        bookmarkArrayListTable = BookmarkArrayListTable.getInstance(project);

        BookmarkTreeNode root = new BookmarkTreeNode(new GroupNodeModel(project.getName()));
        model = new DefaultTreeModel(root);
        setModel(model);

        getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
        navigator.activatedGroup = root;
    }

    private void initDragHandler() {
        setDragEnabled(true);
        setDropMode(DropMode.ON_OR_INSERT);
        setTransferHandler(new DragHandler());
    }

    private void initCellRenderer() {
        setCellRenderer(new BmkTreeCellRenderer());
    }

    private void initTreeListeners() {
        // 选中监听
        addTreeSelectionListener(event -> {
            int selectionCount = getSelectionCount();
            BookmarkTreeNode selectedNode = (BookmarkTreeNode) getLastSelectedPathComponent();
            if (selectionCount != 1 || null == selectedNode) {
                return;
            }

            if (selectedNode.isGroup()) {
                navigator.activeGroup(selectedNode);
            } else {
                navigator.activeBookmark(selectedNode);
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                // Get the selected node
                TreePath path = getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    // Show tooltip for the node
                    showToolTip(getToolTipText(e), e);
                } else {
                    if (this.lastPopup != null) {
                        lastPopup.cancel();
                    }
                }
            }

            private AbstractTreeNodeModel getToolTipText(MouseEvent e) {
                TreePath path = getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    BookmarkTreeNode selectedNode = (BookmarkTreeNode) path.getLastPathComponent();
                    if (selectedNode != null) {
                        return (AbstractTreeNodeModel) selectedNode.getUserObject();
                    }
                }
                return null;
            }

            private JBPopup lastPopup;
            private AbstractTreeNodeModel lastAbstractTreeNodeModel;

            private void showToolTip(AbstractTreeNodeModel abstractTreeNodeModel, MouseEvent e) {
                if (abstractTreeNodeModel == null) {
                    return;
                }
                if (lastAbstractTreeNodeModel == abstractTreeNodeModel) {
                    return;
                }
                if (this.lastPopup != null) {
                    lastPopup.cancel();
                }
                lastAbstractTreeNodeModel = abstractTreeNodeModel;

                JBPopupFactory popupFactory = JBPopupFactory.getInstance();
                lastPopup = popupFactory.createComponentPopupBuilder(new BookmarkTipPanel(lastAbstractTreeNodeModel), null)
                        .setFocusable(true)
                        .setResizable(true)
                        .setRequestFocus(true)
                        .createPopup();

                Point adjustedLocation = new Point(e.getLocationOnScreen().x + 5, e.getLocationOnScreen().y + 10); // Adjust position
                lastPopup.show(RelativePoint.fromScreen(adjustedLocation));
            }

        });


        // 鼠标点击事件
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 双击事件
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    TreePath path = BookmarkTree.this.getSelectionPath();
                    if (Objects.isNull(path)) {
                        return;
                    }
                    BookmarkTreeNode selectedNode = (BookmarkTreeNode) path.getLastPathComponent();
                    if (selectedNode != null && selectedNode.isBookmark()) {
                        BookmarkNodeModel bookmark = (BookmarkNodeModel) selectedNode.getUserObject();

                        OpenFileDescriptor fileDescriptor = bookmark.getOpenFileDescriptor();
                        if (null == fileDescriptor) {
                            return;
                        }
                        fileDescriptor.navigate(true);
                    }
                }

            }
        });

    }

    /**
     * 初始化右键菜单
     */
    private void initContextMenu() {
        JBPopupMenu popupMenu = new JBPopupMenu();
        JBMenuItem imEdit = new JBMenuItem(I18N.get("bookmark.edit"));
        JBMenuItem imDel = new JBMenuItem(I18N.get("bookmark.delete"));
        JBMenuItem imAddGroup = new JBMenuItem(I18N.get("bookmark.addGroup"));
        // TODO 需要添加可以将某个，目录拉出全局显示标签的按钮
        popupMenu.add(imEdit);
        popupMenu.add(imDel);
        popupMenu.add(imAddGroup);

        JBPopupMenu popupMenuRoot = new JBPopupMenu();
        JBMenuItem imAddGroupRoot = new JBMenuItem(I18N.get("bookmark.addGroup"));
        popupMenuRoot.add(imAddGroupRoot);

        imEdit.addActionListener(e -> {
            TreePath path = getSelectionPath();
            if (null == path) {
                return;
            }
            BookmarkTreeNode selectedNode = (BookmarkTreeNode) path.getLastPathComponent();
            AbstractTreeNodeModel nodeModel = (AbstractTreeNodeModel) selectedNode.getUserObject();

            new BookmarkCreatorDialog(project, I18N.get("bookmark.create.title"))
                    .defaultName(nodeModel.getName())
                    .defaultDesc(nodeModel.getDesc())
                    .showAndCallback((name, desc) -> {
                        nodeModel.setName(name);
                        nodeModel.setDesc(desc);
                        if (selectedNode.isBookmark()) {
                            bookmarkArrayListTable.insert((BookmarkNodeModel) nodeModel);
                        }
                        BookmarkTree.this.model.nodeChanged(selectedNode);
                    });
        });

        imDel.addActionListener(e -> {
            int result = Messages.showOkCancelDialog(project, "是否删除选中的标签", "删除确认", "删除", "取消", Messages.getQuestionIcon());
            if (result == Messages.CANCEL) {
                return;
            }
            // 获取选定的节点
            TreePath[] selectionPaths = BookmarkTree.this.getSelectionPaths();
            if (selectionPaths == null) {
                return;
            }
            for (TreePath path : selectionPaths) {
                BookmarkTreeNode node = (BookmarkTreeNode) path.getLastPathComponent();
                BookmarkTreeNode parent = (BookmarkTreeNode) node.getParent();
                if (null == parent) {
                    continue;
                }
                this.remove(node);
            }
        });

        ActionListener addGroupListener = e -> {
            // 获取选定的节点
            BookmarkTreeNode selectedNode = (BookmarkTreeNode) BookmarkTree.this.getLastSelectedPathComponent();
            if (null == selectedNode) {
                return;
            }

            BookmarkTreeNode parent;
            if (selectedNode.isGroup()) {
                parent = selectedNode;
            } else {
                parent = (BookmarkTreeNode) selectedNode.getParent();
            }

            final GroupNodeModel groupNodeModel = new GroupNodeModel();

            new BookmarkCreatorDialog(project, I18N.get("group.create.title"))
                    .showAndCallback((name, desc) -> {
                        groupNodeModel.setName(name);
                        groupNodeModel.setDesc(desc);

                        // 新的分组节点
                        BookmarkTreeNode groupNode = new BookmarkTreeNode(groupNodeModel);
                        model.insertNodeInto(groupNode, parent, 0);

                        BookmarkTree.this.model.nodeChanged(selectedNode);
                    });
        };

        imAddGroup.addActionListener(addGroupListener);
        imAddGroupRoot.addActionListener(addGroupListener);

        // 右键点击事件
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (!SwingUtilities.isRightMouseButton(e)) {
                    return;
                }
                int row = getClosestRowForLocation(e.getX(), e.getY());
                if (row < 0) {
                    return;
                }
                if (!isRowSelected(row)) {
                    setSelectionRow(row);
                }

                if (0 == row) {
                    popupMenuRoot.show(BookmarkTree.this, e.getX() + 16, e.getY());
                } else if (row < getRowCount()) {
                    popupMenu.show(BookmarkTree.this, e.getX() + 16, e.getY());
                }
            }
        });
    }


    public BookmarkTreeNode getEventSourceNode(MouseEvent event) {
        int row = getRowForLocation(event.getX(), event.getY());
        return row >= 0
                ? (BookmarkTreeNode) getPathForRow(row).getLastPathComponent()
                : null;
    }

    /**
     * 向当前激活的分组添加指定节点，并刷新树结构
     *
     * @param node 要添加的节点
     */
    public void add(BookmarkTreeNode node) {

        navigator.activatedBookmark = node;
        BookmarkTreeNode parent = navigator.ensureActivatedGroup();

        model.insertNodeInto(node, parent, parent.getChildCount());
        // 定位到新增的节点并使其可见
        scrollPathToVisible(new TreePath(node.getPath()));

        addToCache(node);
    }

    /**
     * 删除指定节点，并刷新树结构
     *
     * @param node 要删除的节点
     */
    public void remove(BookmarkTreeNode node) {
        model.removeNodeFromParent(node);
        Object userObject = node.getUserObject();
        if (userObject instanceof BookmarkNodeModel) {
            bookmarkArrayListTable.delete((BookmarkNodeModel) node.getUserObject());
        }
        removeFromCache(node);
    }

    public BookmarkTreeNode getNodeByModel(BookmarkNodeModel nodeModel) {
        String uuid = nodeModel.getUuid();
        return nodeCache.get(uuid);
    }

    private void addToCache(BookmarkTreeNode node) {
        BookmarkNodeModel userObject = (BookmarkNodeModel) node.getUserObject();
        nodeCache.put(userObject.getUuid(), node);
    }

    /**
     * 递归从缓存删除节点，确保不要内存泄露
     *
     * @param node 递归根节点
     */
    private void removeFromCache(BookmarkTreeNode node) {
        if (node.isBookmark()) {
            BookmarkNodeModel userObject = (BookmarkNodeModel) node.getUserObject();
            nodeCache.remove(userObject.getUuid());
            return;
        }
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            removeFromCache((BookmarkTreeNode) node.getChildAt(i));
        }
    }

    @Override
    public void setModel(TreeModel newModel) {
        this.model = (DefaultTreeModel) newModel;
        Object root = model.getRoot();
        if (root instanceof BookmarkTreeNode) {
            navigator.activatedGroup = (BookmarkTreeNode) root;
        }
        super.setModel(model);
    }

    @Override
    public DefaultTreeModel getModel() {
        return this.model;
    }

    public GroupNavigator getGroupNavigator() {
        return this.navigator;
    }

    public BookmarkTreeNode getNodeForRow(int row) {
        TreePath path = getPathForRow(row);
        if (path != null) {
            return (BookmarkTreeNode) path.getLastPathComponent();
        } else {
            return null;
        }
    }

    /**
     * 标签树的导航器，与快捷键绑定，用于遍历当前选中的分组下的标签，当前分组的下级分组不会被遍历
     */
    public static class GroupNavigator {

        private final BookmarkTree tree;

        private BookmarkTreeNode activatedGroup;
        private BookmarkTreeNode activatedBookmark;

        GroupNavigator(BookmarkTree tree) {
            this.tree = tree;
        }

        public void pre() {
            BookmarkTreeNode group = ensureActivatedGroup();
            if (0 == group.getBookmarkChildCount()) {
                return;
            }

            BookmarkTreeNode bookmark = ensureActivatedBookmark();
            int index = preTreeNodeIndex(group, bookmark);
            navigateTo(index);
        }

        public void next() {
            BookmarkTreeNode group = ensureActivatedGroup();
            if (0 == group.getBookmarkChildCount()) {
                return;
            }

            BookmarkTreeNode bookmark = ensureActivatedBookmark();
            int index = nextTreeNodeIndex(group, bookmark);
            navigateTo(index);
        }

        public void activeGroup(BookmarkTreeNode node) {
            activatedGroup = node;
            if (node.getChildCount() > 0) {
                activatedBookmark = (BookmarkTreeNode) node.getChildAt(0);
            } else {
                activatedBookmark = null;
            }
        }

        public void activeBookmark(BookmarkTreeNode node) {
            activatedBookmark = node;
            activatedGroup = (BookmarkTreeNode) node.getParent();

            TreePath treePath = new TreePath(node.getPath());
            tree.setSelectionPath(treePath);

            if (!tree.isVisible(treePath)) {
                tree.scrollPathToVisible(treePath);
            }
        }

        /**
         * 确保 {@code activatedGroup} 一定是一个在当前树上的节点，
         * 所有读取 {@code activatedGroup} 值的地方都应该调用这个方法，
         * 避免当 {@code activatedGroup} 指向的节点已经从当前的 tree 中移除
         *
         * @return 激活的节点或者根节点
         */
        private BookmarkTreeNode ensureActivatedGroup() {
            if (null == activatedGroup) {
                return (BookmarkTreeNode) tree.getModel().getRoot();
            }
            TreeNode[] path = activatedGroup.getPath();
            int row = tree.getRowForPath(new TreePath(path));
            if (row < 0) {
                return (BookmarkTreeNode) tree.getModel().getRoot();
            }
            return activatedGroup;
        }

        /**
         * 确保 {@code activatedBookmark} 一定是一个在当前树上的节点，
         * 所有读取 {@code activatedBookmark} 值的地方都应该调用这个方法，
         * 避免当 {@code activatedBookmark} 指向的节点已经从当前的 tree 中移除
         *
         * @return 激活的节点 或者 {@code null}
         */
        private BookmarkTreeNode ensureActivatedBookmark() {
            if (null == activatedBookmark) {
                return null;
            }
            TreeNode[] path = activatedBookmark.getPath();
            int row = tree.getRowForPath(new TreePath(path));

            return row < 0 ? null : activatedBookmark;
        }

        private void navigateTo(int index) {
            Validate.isTrue(index >= 0, "index must be greater than 0");

            BookmarkTreeNode nextNode = (BookmarkTreeNode) activatedGroup.getChildAt(index);
            activeBookmark(nextNode);

            BookmarkNodeModel model = (BookmarkNodeModel) nextNode.getUserObject();
            model.getOpenFileDescriptor().navigate(true);
        }

        private int preTreeNodeIndex(BookmarkTreeNode activeGroup, BookmarkTreeNode activatedBookmark) {
            Validate.isTrue(activeGroup.getBookmarkChildCount() > 0, "activeGroup has no child");
            if (null == activatedBookmark) {
                return activeGroup.firstChildIndex();
            }
            int currIndex = activeGroup.getIndex(activatedBookmark);
            int groupSize = activeGroup.getChildCount();

            BookmarkTreeNode node;
            do {
                currIndex = (currIndex - 1 + groupSize) % groupSize;
                node = (BookmarkTreeNode) activeGroup.getChildAt(currIndex);
            } while (node.isGroup());
            return currIndex;
        }

        private int nextTreeNodeIndex(BookmarkTreeNode activeGroup, BookmarkTreeNode activatedBookmark) {
            Validate.isTrue(activeGroup.getBookmarkChildCount() > 0, "activeGroup has no child");
            if (null == activatedBookmark) {
                return activeGroup.firstChildIndex();
            }

            int currIndex = activeGroup.getIndex(activatedBookmark);
            BookmarkTreeNode node;
            do {
                currIndex = (currIndex + 1) % activeGroup.getChildCount();
                node = (BookmarkTreeNode) activeGroup.getChildAt(currIndex);
            } while (node.isGroup());
            return currIndex;
        }

    }

    // 自定义传输对象
    static class NodesTransferable implements Transferable {

        public static final DataFlavor NODES_FLAVOR = new DataFlavor(int[].class, "Tree Rows");

        private final int[] rows;

        public NodesTransferable(int[] rows) {
            this.rows = rows;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{NODES_FLAVOR};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(NODES_FLAVOR);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (isDataFlavorSupported(flavor)) {
                return rows;
            } else {
                throw new UnsupportedFlavorException(flavor);
            }
        }
    }

    static class DragHandler extends TransferHandler {

        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            BookmarkTree tree = (BookmarkTree) c;
            int[] paths = tree.getSelectionRows();
            if (paths != null && paths.length > 0) {
                return new NodesTransferable(paths);
            }
            return null;
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            if (action != MOVE) {
                return;
            }
            super.exportDone(source, data, action);
        }

        @Override
        public boolean canImport(TransferSupport support) {
            JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
            TreePath destPath = dl.getPath();
            if (destPath == null) {
                return false;
            }
            BookmarkTreeNode targetNode = (BookmarkTreeNode) destPath.getLastPathComponent();
            return targetNode != null && targetNode.isGroup();
        }

        @Override
        public boolean importData(TransferSupport support) {
            JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
            BookmarkTree tree = (BookmarkTree) support.getComponent();
            TreePath destPath = dl.getPath();
            BookmarkTreeNode targetNode = (BookmarkTreeNode) destPath.getLastPathComponent();

            try {
                Transferable transferable = support.getTransferable();
                int[] rows = (int[]) transferable.getTransferData(NodesTransferable.NODES_FLAVOR);
                DefaultTreeModel model = tree.getModel();

                List<BookmarkTreeNode> nodes = Arrays.stream(rows)
                        .mapToObj(tree::getNodeForRow)
                        .collect(Collectors.toList());

                int childIndex = dl.getChildIndex();

                System.out.println(childIndex);
                System.out.println(targetNode);

                if (-1 == childIndex) {
                    for (BookmarkTreeNode node : nodes) {
                        // 目标节点不能是拖动节点的后代，拖动节点不能是目标节点的直接子代
                        if (!targetNode.isNodeAncestor(node) && !targetNode.isNodeChild(node)) {
                            model.removeNodeFromParent(node);
                            model.insertNodeInto(node, targetNode, targetNode.getChildCount());
                        }
                    }
                } else {
                    Collections.reverse(nodes);
                    for (BookmarkTreeNode node : nodes) {
                        // 目标节点不能是拖动节点的后代，拖动节点不能是目标节点的直接子代
                        if (!targetNode.isNodeAncestor(node)) {
                            if (targetNode.isNodeChild(node)) {
                                int index = targetNode.getIndex(node);
                                if (childIndex > index) {
                                    childIndex = childIndex - 1;
                                }
                            }
                            model.removeNodeFromParent(node);
                            model.insertNodeInto(node, targetNode, childIndex);
                        }
                    }
                }
                tree.expandPath(destPath);
                return true;

            } catch (Exception e) {
                e.printStackTrace();
            }

            return false;
        }
    }

}
