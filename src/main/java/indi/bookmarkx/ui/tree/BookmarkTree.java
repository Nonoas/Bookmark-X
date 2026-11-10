package indi.bookmarkx.ui.tree;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.treeStructure.Tree;
import indi.bookmarkx.BookmarksManager;
import indi.bookmarkx.common.I18N;
import indi.bookmarkx.listener.BookmarkListener;
import indi.bookmarkx.model.AbstractTreeNodeModel;
import indi.bookmarkx.model.BookmarkNodeModel;
import indi.bookmarkx.model.GroupNodeModel;
import indi.bookmarkx.persistence.MySettings;
import indi.bookmarkx.ui.dialog.BookmarkCreatorDialog;
import indi.bookmarkx.ui.dialog.LineAdjustDialog;
import indi.bookmarkx.ui.panel.BookmarkTipPanel;
import indi.bookmarkx.utils.FileLineCounter;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.ChangeListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelListener;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 书签树
 *
 * @author Nonoas
 * @date 2023/6/1
 */
public class BookmarkTree extends Tree implements BookmarkListener {

    private static final Logger log = Logger.getInstance(BookmarkTree.class);

    /**
     * BookmarkTreeNode 缓存，便于通过 UUID 直接取到节点引用
     */
    private final Map<String, BookmarkTreeNode> nodeCache = new HashMap<>();

    private final GroupNavigator navigator = new GroupNavigator(this);

    private DefaultTreeModel model;

    private Project project;

    private TreeMouseMotionAdapter hover;

    private JScrollPane scrollPane;

    private JViewport scrollViewport;

    private final MouseWheelListener scrollPaneWheelListener = event -> clearHoverPreview();

    private final ChangeListener viewportChangeListener = event -> clearHoverPreview();

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
        // 订阅书签变化事件
        project.getMessageBus().connect().subscribe(BookmarkListener.TOPIC, this);

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

        hover = new TreeMouseMotionAdapter(this, project);
        addMouseMotionListener(hover);
        addMouseListener(hover);
        addHierarchyListener(event -> {
            if (!isShowing()) {
                hover.clear();
            }
        });

        // 鼠标点击事件
        addMouseListener(new DoubleClickAdapter(this));

    }

    @Override
    public void addNotify() {
        super.addNotify();
        bindScrollListeners();
    }

    @Override
    public void removeNotify() {
        unbindScrollListeners();
        clearHoverPreview();
        super.removeNotify();
    }

    private void bindScrollListeners() {
        JScrollPane pane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
        JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
        if (pane == scrollPane && viewport == scrollViewport) {
            return;
        }
        unbindScrollListeners();
        scrollPane = pane;
        scrollViewport = viewport;
        if (scrollPane != null) {
            scrollPane.addMouseWheelListener(scrollPaneWheelListener);
        }
        if (scrollViewport != null) {
            scrollViewport.addChangeListener(viewportChangeListener);
        }
    }

    private void unbindScrollListeners() {
        if (scrollPane != null) {
            scrollPane.removeMouseWheelListener(scrollPaneWheelListener);
            scrollPane = null;
        }
        if (scrollViewport != null) {
            scrollViewport.removeChangeListener(viewportChangeListener);
            scrollViewport = null;
        }
    }

    private void clearHoverPreview() {
        if (hover != null) {
            hover.clear();
        }
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
            BookmarksManager.getInstance(project).editBookRemark(nodeModel);
        });

        imDel.addActionListener(e -> {
            int result = Messages.showOkCancelDialog(project,
                    I18N.get("bookmark.delete.msg"),
                    I18N.get("bookmark.delete.ok"),
                    I18N.get("delete"),
                    I18N.get("cancel"),
                    Messages.getQuestionIcon());

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

            BookmarkCreatorDialog.BookmarkDialogResult result = new BookmarkCreatorDialog(project, I18N.get("group.create.title"))
                    .showAndGetResult();
            if (!result.isOk()) {
                return;
            }

            String uuid = UUID.randomUUID().toString();
            groupNodeModel.setUuid(uuid);
            groupNodeModel.setName(result.getName());
            groupNodeModel.setDesc(result.getDesc());

            // 新的分组节点
            BookmarkTreeNode groupNode = new BookmarkTreeNode(groupNodeModel);
            model.insertNodeInto(groupNode, parent, parent.getChildCount());
            BookmarkTree.this.model.nodeChanged(selectedNode);

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

        // 批量调整行号菜单项
        JPopupMenu.Separator batchAdjustLineSeparator = new JPopupMenu.Separator();
        JBMenuItem batchAdjustLineItem = new JBMenuItem(I18N.get("bookmark.batchAdjustLine"));
        batchAdjustLineItem.addActionListener(e -> {
            TreePath[] selectionPaths = BookmarkTree.this.getSelectionPaths();
            if (!this.showBatchAdjustMenu()) {
                return;
            }
            LineAdjustDialog dialog = new LineAdjustDialog(project);
            if (dialog.showAndGet()) {
                int adjustValue = dialog.getAdjustValue();
                if (adjustValue == 0) {
                    return;
                }

                // 调整所有选中书签的行号
                for (TreePath path : selectionPaths) {
                    BookmarkTreeNode node = (BookmarkTreeNode) path.getLastPathComponent();
                    BookmarkNodeModel model = (BookmarkNodeModel) node.getUserObject();

                    // 计算新行号，确保不小于0
                    int newLine = Math.max(0, model.getLine() + adjustValue);
                    int maxLine = FileLineCounter.getFileMaxLine(model.getOpenFileDescriptor());
                    if (maxLine > 0) {
                        newLine = Math.min(newLine, maxLine - 1);// 从0开始
                    }
                    model.updateBookmarkLine(newLine, false);
                }
                BookmarksManager.getInstance(project).persistentSave();
            }
        });

        popupMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                boolean showBatchAdjust = showBatchAdjustMenu();
                if (showBatchAdjust && popupMenu.getComponentIndex(batchAdjustLineItem) == -1) {
                    popupMenu.add(batchAdjustLineSeparator);
                    popupMenu.add(batchAdjustLineItem);
                }
            }

            @Override
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                popupMenu.remove(batchAdjustLineSeparator);
                popupMenu.remove(batchAdjustLineItem);
            }

            @Override
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                popupMenu.remove(batchAdjustLineSeparator);
                popupMenu.remove(batchAdjustLineItem);
            }
        });
    }

    private boolean showBatchAdjustMenu() {
        TreePath[] selectionPaths = this.getSelectionPaths();
        if (selectionPaths == null || selectionPaths.length < 2) {
            return false;
        }
        // 检查是否所有选中项都是书签
        for (TreePath path : selectionPaths) {
            BookmarkTreeNode node = (BookmarkTreeNode) path.getLastPathComponent();
            if (!node.isBookmark()) {
                return false;
            }
        }
        return true;
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
    private void remove(@NotNull BookmarkTreeNode node) {
        if (node.isBookmark()) {
            BookmarksManager.getInstance(project).removeBookRemark((BookmarkNodeModel) node.getUserObject());
            return;
        }
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            remove((BookmarkTreeNode) node.getChildAt(i));
        }
        // 删除完所有书签节点之后，删除分组节点
        this.model.removeNodeFromParent(node);
    }

    public BookmarkTreeNode getNodeByModel(BookmarkNodeModel nodeModel) {
        String uuid = nodeModel.getUuid();
        return nodeCache.get(uuid);
    }

    /**
     * 在标签树中展开、选中并滚动到指定书签。
     *
     * @param bookmark 要定位的书签
     * @return 找到对应树节点时返回 {@code true}
     */
    public boolean selectBookmark(BookmarkNodeModel bookmark) {
        BookmarkTreeNode node = getNodeByModel(bookmark);
        if (node == null) {
            return false;
        }
        TreePath path = new TreePath(node.getPath());
        TreePath parentPath = path.getParentPath();
        if (parentPath != null) {
            expandPath(parentPath);
        }
        setSelectionPath(path);
        scrollPathToVisible(path);
        requestFocusInWindow();
        return true;
    }

    private void addToCache(BookmarkTreeNode node) {
        AbstractTreeNodeModel userObject = (AbstractTreeNodeModel) node.getUserObject();
        if (userObject.getUuid() != null) {
            nodeCache.put(userObject.getUuid(), node);
        }
    }

    private void removeFromCache(BookmarkTreeNode node) {
        if (node == null) {
            return;
        }
        Object userObject = node.getUserObject();
        if (!(userObject instanceof AbstractTreeNodeModel)) {
            return;
        }
        String uuid = ((AbstractTreeNodeModel) userObject).getUuid();
        if (uuid != null) {
            nodeCache.remove(uuid);
        }
    }

    public void insertNodeInto(BookmarkTreeNode node, BookmarkTreeNode parent, int index) {
        model.insertNodeInto(node, parent, index);
        if (node.isBookmark()) {
            addToCache(node);
        }
    }

    public void moveNode(BookmarkTreeNode node, BookmarkTreeNode parent, int index) {
        if (node == null || parent == null) {
            return;
        }
        if (node.getParent() != null) {
            model.removeNodeFromParent(node);
        }
        model.insertNodeInto(node, parent, index);
        if (node.isBookmark()) {
            addToCache(node);
        }
    }

    public void removeNodeFromParent(BookmarkTreeNode node) {
        if (node == null || node.getParent() == null) {
            return;
        }
        removeNodeCacheRecursive(node);
        model.removeNodeFromParent(node);
    }

    private void removeNodeCacheRecursive(BookmarkTreeNode node) {
        if (node.isBookmark()) {
            removeFromCache(node);
            return;
        }
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            removeNodeCacheRecursive((BookmarkTreeNode) node.getChildAt(i));
        }
    }

    @Override
    public void setModel(TreeModel newModel) {
        this.model = (DefaultTreeModel) newModel;
        Object root = model.getRoot();
        if (!(root instanceof BookmarkTreeNode)) {
            super.setModel(model);
            return;
        }
        navigator.activatedGroup = (BookmarkTreeNode) root;
        nodeCache.clear();
        loadNodeCache((BookmarkTreeNode) root);
        super.setModel(model);
    }

    /**
     * 加载当前节点下的所有节点到缓存
     *
     * @param node 当前节点
     */
    private void loadNodeCache(BookmarkTreeNode node) {
        AbstractTreeNodeModel model = (AbstractTreeNodeModel) node.getUserObject();
        if (node.isBookmark()) {
            nodeCache.put(model.getUuid(), node);
            return;
        }
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            loadNodeCache((BookmarkTreeNode) node.getChildAt(i));
        }
    }

    @Override
    public DefaultTreeModel getModel() {
        return this.model;
    }

    public GroupNavigator getGroupNavigator() {
        return this.navigator;
    }

    public BookmarkTreeNode getActivatedGroupNode() {
        return navigator.ensureActivatedGroup();
    }

    public BookmarkTreeNode getGroupForBookmark(BookmarkNodeModel bookmark) {
        BookmarkTreeNode node = getNodeByModel(bookmark);
        if (node == null || !(node.getParent() instanceof BookmarkTreeNode)) {
            return null;
        }
        BookmarkTreeNode parent = (BookmarkTreeNode) node.getParent();
        return parent.isGroup() ? parent : null;
    }

    public List<BookmarkTreeNode> getGroupNodes() {
        List<BookmarkTreeNode> groups = new ArrayList<>();
        BookmarkTreeNode root = (BookmarkTreeNode) model.getRoot();
        Enumeration<TreeNode> nodes = root.preorderEnumeration();
        while (nodes.hasMoreElements()) {
            TreeNode node = nodes.nextElement();
            if (node instanceof BookmarkTreeNode && ((BookmarkTreeNode) node).isGroup()) {
                groups.add((BookmarkTreeNode) node);
            }
        }
        return groups;
    }

    public String getGroupPath(BookmarkTreeNode group) {
        if (group == null || !group.isGroup()) {
            return "";
        }
        List<String> names = new ArrayList<>();
        for (TreeNode node : group.getPath()) {
            if (!(node instanceof BookmarkTreeNode)) {
                continue;
            }
            AbstractTreeNodeModel nodeModel = (AbstractTreeNodeModel) ((BookmarkTreeNode) node).getUserObject();
            String name = nodeModel.getName();
            if (name != null && !name.trim().isEmpty()) {
                names.add(name);
            }
        }
        return String.join(" / ", names);
    }

    public boolean moveBookmarkToGroup(BookmarkNodeModel bookmark, BookmarkTreeNode targetGroup) {
        BookmarkTreeNode bookmarkNode = getNodeByModel(bookmark);
        if (bookmarkNode == null || targetGroup == null || !targetGroup.isGroup()) {
            return false;
        }
        if (targetGroup.getRoot() != model.getRoot()) {
            return false;
        }
        BookmarkTreeNode currentGroup = (BookmarkTreeNode) bookmarkNode.getParent();
        if (currentGroup == targetGroup) {
            return false;
        }
        moveNode(bookmarkNode, targetGroup, targetGroup.getChildCount());
        refreshBookmarkIndices(currentGroup);
        refreshBookmarkIndices(targetGroup);
        return true;
    }

    private void refreshBookmarkIndices(BookmarkTreeNode group) {
        if (group == null) {
            return;
        }
        for (int i = 0; i < group.getChildCount(); i++) {
            BookmarkTreeNode child = (BookmarkTreeNode) group.getChildAt(i);
            if (child.isBookmark()) {
                ((BookmarkNodeModel) child.getUserObject()).setIndex(group.getIndex(child));
            }
        }
    }

    public BookmarkTreeNode getNodeForRow(int row) {
        TreePath path = getPathForRow(row);
        if (path != null) {
            return (BookmarkTreeNode) path.getLastPathComponent();
        } else {
            return null;
        }
    }

    @Override
    public void bookmarkAdded(@NotNull AbstractTreeNodeModel model) {
        if (model.isGroup()) {
            return;
        }
        BookmarkTreeNode treeNode = new BookmarkTreeNode(model);
        if (nodeCache.containsKey(model.getUuid())) {
            this.model.nodeChanged(nodeCache.get(model.getUuid()));
            return;
        }
        this.add(treeNode);

        BookmarkNodeModel bookmarkNodeModel = (BookmarkNodeModel) model;
        bookmarkNodeModel.setIndex(treeNode.getParent().getIndex(treeNode));
        BookmarkTreeNode nodeByModel = getNodeByModel(bookmarkNodeModel);
        this.model.nodeChanged(nodeByModel);
    }

    @Override
    public void bookmarkChanged(@NotNull AbstractTreeNodeModel model) {
        if (model.isGroup()) {
            return;
        }
        BookmarkNodeModel bookmarkNodeModel = (BookmarkNodeModel) model;
        BookmarkTreeNode node = nodeCache.get(bookmarkNodeModel.getUuid());
        if (node == null) {
            return;
        }
        this.model.nodeChanged(node);
    }

    @Override
    public void bookmarkRemoved(@NotNull AbstractTreeNodeModel model) {
        if (model.isGroup()) {
            return;
        }
        BookmarkNodeModel bookmarkNodeModel = (BookmarkNodeModel) model;
        BookmarkTreeNode node = nodeCache.remove(bookmarkNodeModel.getUuid());
        if (node == null || node.getParent() == null) {
            return;
        }
        this.model.removeNodeFromParent(node);
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
            OpenFileDescriptor openFileDescriptor = model.getOpenFileDescriptor();
            if (null == openFileDescriptor) {
                log.warn("Can't find open file descriptor for " + model.getName());
                return;
            }
            openFileDescriptor.navigate(true);
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

    /**
     * 节点拖拽处理器
     */
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
            if (!support.isDrop() || !support.isDataFlavorSupported(NodesTransferable.NODES_FLAVOR)) {
                return false;
            }
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
            if (!canImport(support)) return false;
            JTree.DropLocation location = (JTree.DropLocation) support.getDropLocation();
            BookmarkTree tree = (BookmarkTree) support.getComponent();
            TreePath destination = location.getPath();
            BookmarkTreeNode target = (BookmarkTreeNode) destination.getLastPathComponent();
            try {
                int[] rows = (int[]) support.getTransferable().getTransferData(NodesTransferable.NODES_FLAVOR);
                List<BookmarkTreeNode> nodes = Arrays.stream(rows).sorted()
                        .mapToObj(tree::getNodeForRow).collect(Collectors.toList());
                if (!BookmarkTreeOrder.move(tree.getModel(), nodes, target, location.getChildIndex())) {
                    return false;
                }
                tree.expandPath(destination);
                tree.setSelectionPaths(nodes.stream().map(node -> new TreePath(node.getPath())).toArray(TreePath[]::new));
                tree.repaint();
                BookmarksManager.getInstance(tree.project).persistentSave();
                return true;
            } catch (Exception exception) {
                log.error(exception);
                return false;
            }
        }

    }

    /**
     * 鼠标选父监听
     */
    static class TreeMouseMotionAdapter extends MouseAdapter {
        private static final int POINTER_POLL_INTERVAL_MS = 100;
        private static final int POINTER_EXIT_GRACE_MS = 350;
        private static final int POPUP_DISMISS_DELAY_MS = 5000;
        private static final int POPUP_GAP = 8;

        private Timer timer;
        private Timer dismissTimer;
        private Timer pointerMonitorTimer;
        private TreePath hoveredPath;
        private final JTree tree;
        private final Project project;
        private JBPopup popup;
        private long pointerOutsideSince = -1L;
        private Point latestMouseLocation;

        TreeMouseMotionAdapter(JTree tree, Project project) {
            this.tree = tree;
            this.project = project;
        }

        @Override
        public void mouseMoved(MouseEvent event) {
            latestMouseLocation = event.getPoint();
            int delay = MySettings.getInstance().getTipDelay();
            TreePath path = tree.getPathForLocation(event.getX(), event.getY());
            if (delay < 0 || event.getModifiersEx() != 0) {
                clear();
                return;
            }
            if (path == null) {
                if (popup == null) {
                    clear();
                }
                return;
            }
            if (Objects.equals(path, hoveredPath)) {
                return;
            }
            clear();
            hoveredPath = path;
            latestMouseLocation = event.getPoint();
            // Avoid instant popups while moving through the bookmark list.
            timer = new Timer(Math.max(600, delay), ignored -> showTooltip(path));
            timer.setRepeats(false);
            timer.start();
        }

        @Override
        public void mouseExited(MouseEvent event) {
            if (popup == null) {
                clear();
            } else if (pointerOutsideSince < 0) {
                pointerOutsideSince = System.currentTimeMillis();
            }
        }

        @Override
        public void mousePressed(MouseEvent event) {
            clear();
        }

        @Override
        public void mouseDragged(MouseEvent event) {
            clear();
        }

        void clear() {
            hoveredPath = null;
            if (timer != null) {
                timer.stop();
                timer = null;
            }
            if (dismissTimer != null) {
                dismissTimer.stop();
                dismissTimer = null;
            }
            if (pointerMonitorTimer != null) {
                pointerMonitorTimer.stop();
                pointerMonitorTimer = null;
            }
            pointerOutsideSince = -1L;
            if (popup != null) {
                popup.cancel();
                popup = null;
            }
        }

        private void showTooltip(TreePath path) {
            Point location = latestMouseLocation == null ? null : new Point(latestMouseLocation);
            if (project.isDisposed() || !tree.isShowing() || !Objects.equals(path, hoveredPath)
                    || MySettings.getInstance().getTipDelay() < 0
                    || location == null
                    || !Objects.equals(path, tree.getPathForLocation(location.x, location.y))) {
                clear();
                return;
            }
            BookmarkTreeNode node = (BookmarkTreeNode) path.getLastPathComponent();
            AbstractTreeNodeModel model = (AbstractTreeNodeModel) node.getUserObject();
            BookmarkTipPanel tipPanel = new BookmarkTipPanel(project, model);
            tipPanel.fitPopupToContent();
            popup = JBPopupFactory.getInstance().createComponentPopupBuilder(
                            tipPanel, null)
                    .setTitle(I18N.get("bookmark.tip.title"))
                    .setFocusable(true)
                    .setRequestFocus(false)
                    .setResizable(true)
                    .setMovable(true)
                    .setCancelOnClickOutside(true)
                    .setCancelOnWindowDeactivation(true)
                    .createPopup();
            popup.showInScreenCoordinates(tree, popupLocation(location, tipPanel.getPreferredSize()));
            startPointerMonitor();
            dismissTimer = new Timer(POPUP_DISMISS_DELAY_MS, ignored -> dismissPopupUnlessHovered());
            dismissTimer.setRepeats(false);
            dismissTimer.start();
        }

        private Point popupLocation(Point mouseLocation, Dimension popupSize) {
            Point screenLocation = new Point(mouseLocation);
            SwingUtilities.convertPointToScreen(screenLocation, tree);
            return choosePopupLocation(screenLocation, popupSize, usableScreenBounds(tree), POPUP_GAP);
        }

        static Point choosePopupLocation(Point mouseLocation, Dimension popupSize,
                                         Rectangle screenBounds, int gap) {
            int screenRight = screenBounds.x + screenBounds.width;
            int screenBottom = screenBounds.y + screenBounds.height;
            int rightX = mouseLocation.x + gap;
            int leftX = mouseLocation.x - popupSize.width - gap;

            int x;
            if (rightX + popupSize.width <= screenRight) {
                x = rightX;
            } else if (leftX >= screenBounds.x) {
                x = leftX;
            } else {
                int rightSpace = screenRight - mouseLocation.x;
                int leftSpace = mouseLocation.x - screenBounds.x;
                x = rightSpace >= leftSpace
                        ? Math.max(screenBounds.x, screenRight - popupSize.width)
                        : screenBounds.x;
            }
            int preferredY = mouseLocation.y - gap;
            int maxY = Math.max(screenBounds.y, screenBottom - popupSize.height);
            int y = Math.max(screenBounds.y, Math.min(preferredY, maxY));
            return new Point(x, y);
        }

        private static Rectangle usableScreenBounds(Component component) {
            GraphicsConfiguration configuration = component.getGraphicsConfiguration();
            if (configuration == null) {
                return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            }
            Rectangle bounds = new Rectangle(configuration.getBounds());
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
            bounds.x += insets.left;
            bounds.y += insets.top;
            bounds.width -= insets.left + insets.right;
            bounds.height -= insets.top + insets.bottom;
            return bounds;
        }

        private void startPointerMonitor() {
            pointerOutsideSince = -1L;
            pointerMonitorTimer = new Timer(POINTER_POLL_INTERVAL_MS, ignored -> {
                if (popup == null || !popup.isVisible()) {
                    if (pointerMonitorTimer != null) {
                        pointerMonitorTimer.stop();
                        pointerMonitorTimer = null;
                    }
                    return;
                }
                if (isPointerInside(hoverSurface()) || isPointerInside(popupSurface())) {
                    pointerOutsideSince = -1L;
                    return;
                }
                long now = System.currentTimeMillis();
                if (pointerOutsideSince < 0) {
                    pointerOutsideSince = now;
                } else if (now - pointerOutsideSince >= POINTER_EXIT_GRACE_MS) {
                    clear();
                }
            });
            pointerMonitorTimer.start();
        }

        private Component hoverSurface() {
            Container viewport = SwingUtilities.getAncestorOfClass(JViewport.class, tree);
            return viewport == null ? tree : viewport;
        }

        private Component popupSurface() {
            if (popup == null) {
                return null;
            }
            Component content = popup.getContent();
            Window window = SwingUtilities.getWindowAncestor(content);
            return window == null ? content : window;
        }

        private void dismissPopupUnlessHovered() {
            if (popup != null && isPointerInside(popupSurface())) {
                dismissTimer.restart();
                return;
            }
            closePopupKeepingHoverPath();
        }

        private void closePopupKeepingHoverPath() {
            if (dismissTimer != null) {
                dismissTimer.stop();
                dismissTimer = null;
            }
            if (pointerMonitorTimer != null) {
                pointerMonitorTimer.stop();
                pointerMonitorTimer = null;
            }
            pointerOutsideSince = -1L;
            if (popup != null) {
                popup.cancel();
                popup = null;
            }
        }

        private static boolean isPointerInside(Component component) {
            if (component == null || !component.isShowing()) {
                return false;
            }
            try {
                PointerInfo pointerInfo = MouseInfo.getPointerInfo();
                if (pointerInfo == null) {
                    return false;
                }
                Point location = pointerInfo.getLocation();
                SwingUtilities.convertPointFromScreen(location, component);
                return component.contains(location);
            } catch (HeadlessException | IllegalComponentStateException | SecurityException ignored) {
                return false;
            }
        }
    }

    /**
     * 鼠标双击事件适配器
     */
    static class DoubleClickAdapter extends MouseAdapter {
        // 假设 log 变量已在外部或父类中正确声明和初始化
        private static final Logger log = Logger.getInstance(DoubleClickAdapter.class);

        // 记录上一次点击的时间（毫秒）
        private long lastClickTime = 0;
        // 记录上一次点击的坐标
        private Point lastClickPoint = new Point();

        // 可以根据用户体验调整这些阈值
        // 设置双击时间阈值（毫秒）。标准通常是 300ms
        private static final int DOUBLE_CLICK_TIME_THRESHOLD = 300;
        // 设置双击距离阈值（像素）。防止鼠标轻微移动导致双击失败
        private static final int DOUBLE_CLICK_DISTANCE_THRESHOLD = 5;

        private final JTree tree;

        DoubleClickAdapter(JTree tree) {
            this.tree = tree;
        }

        /**
         * 将双击判断逻辑从 mouseClicked 转移到更可靠的 mousePressed
         */
        @Override
        public void mousePressed(MouseEvent e) {
            // 1. 只处理左键事件
            if (!SwingUtilities.isLeftMouseButton(e)) {
                return;
            }

            long currentTime = e.getWhen();
            Point currentPoint = e.getPoint();

            // 2. 检查时间间隔是否满足双击要求
            if (currentTime - lastClickTime < DOUBLE_CLICK_TIME_THRESHOLD) {

                // 3. 检查距离是否满足双击要求
                double distance = currentPoint.distance(lastClickPoint);

                if (distance < DOUBLE_CLICK_DISTANCE_THRESHOLD) {

                    // *** 手动识别为双击！ ***
                    log.debug("【手动识别】进入鼠标双击逻辑");
                    log.debug("【手动识别】识别为双击");

                    // 执行双击动作
                    performDoubleClickAction(e);

                    // 4. 重置状态：将 lastClickTime 设为 0，防止用户快速三连击被误判为两次双击
                    lastClickTime = 0;
                    return;
                }
            }

            // 5. 如果不是双击，则记录本次点击，作为下一次判断的基础
            lastClickTime = currentTime;
            lastClickPoint = currentPoint;
        }

        // 移除原有的 mouseClicked 方法，因为它不再是主逻辑

        /**
         * 封装双击成功后的实际导航逻辑
         */
        private void performDoubleClickAction(MouseEvent e) {
            // 关键：使用 getClosestPathForLocation(e.getX(), e.getY())
            // 替代 getSelectionPath()，直接通过坐标获取节点，避免选区丢失问题。
            TreePath path = tree.getClosestPathForLocation(e.getX(), e.getY());

            if (Objects.isNull(path)) {
                log.debug("【手动识别】退出鼠标双击逻辑：path 为空");
                return;
            }

            // 可选：强制选中路径，提供视觉反馈
            tree.setSelectionPath(path);

            BookmarkTreeNode selectedNode = (BookmarkTreeNode) path.getLastPathComponent();
            if (selectedNode != null && selectedNode.isBookmark()) {
                BookmarkNodeModel bookmark = (BookmarkNodeModel) selectedNode.getUserObject();

                OpenFileDescriptor fileDescriptor = bookmark.getOpenFileDescriptor();
                if (null == fileDescriptor) {
                    log.debug("【手动识别】退出鼠标双击逻辑：fileDescriptor为空");
                    return;
                }
                fileDescriptor.navigate(true);
            }
            log.debug("【手动识别】退出鼠标双击逻辑，导航成功");
        }
    }

}
