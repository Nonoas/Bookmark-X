package indi.bookmarkx.tree;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import indi.bookmarkx.BookmarksManager;
import indi.bookmarkx.common.I18N;
import indi.bookmarkx.model.BookmarkNodeModel;
import indi.bookmarkx.model.GroupNodeModel;
import org.apache.commons.lang3.Validate;
import org.jsoup.internal.StringUtil;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Nonoas
 * @date 2023/6/1
 */
public class BookmarkTree extends JTree {

    /**
     * BookmarkTreeNode 缓存，便于通过 UUID 直接取到节点引用
     */
    private final Map<String, BookmarkTreeNode> nodeCache = new HashMap<>();

    private final GroupNavigator navigator = new GroupNavigator(this);

    private DefaultTreeModel model;

    private Project project;

    public BookmarkTree(Project project) {
        super();
        this.project = project;

        BookmarkTreeNode root = new BookmarkTreeNode(new GroupNodeModel("ROOT"));
        model = new DefaultTreeModel(root);
        setModel(model);

        getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);

        setBorder(JBUI.Borders.empty());
        setBackground(JBColor.WHITE);
        setRootVisible(false);
        setShowsRootHandles(true);

        navigator.activatedGroup = root;

//      TODO 后续需要支持拖拽
//        initDragHandler();
//        initSelectionModel();
        initCellRenderer();
        initTreeListeners();
        initContextMenu();


    }

    private void initDragHandler() {

        setDragEnabled(true);
        // 创建拖拽手势支持
        DragSource dragSource = DragSource.getDefaultDragSource();
        dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, event -> {
            // 获取拖拽的路径
            TreePath path = getSelectionPath();
            if (null == path) {
                return;
            }

            // 获取拖拽的节点
            BookmarkTreeNode draggedNode = (BookmarkTreeNode) path.getLastPathComponent();

            // 创建传输对象
            Transferable transferable = new Transferable() {

                // 定义自定义的 DataFlavor
                private final DataFlavor dataFlavor = new DataFlavor(BookmarkTreeNode.class, "BookmarkTreeNode");

                @Override
                public DataFlavor[] getTransferDataFlavors() {
                    return new DataFlavor[]{dataFlavor};
                }

                @Override
                public boolean isDataFlavorSupported(DataFlavor flavor) {
                    return flavor.equals(dataFlavor);
                }

                @Override
                public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                    if (flavor.equals(dataFlavor)) {
                        return draggedNode;
                    }
                    throw new UnsupportedFlavorException(flavor);
                }
            };

            // 开始拖拽操作
            event.startDrag(null, transferable);
        });

        // 创建拖放目标支持
        DropTarget dropTarget = new DropTarget(this, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent event) {

                // 获取拖拽的节点
                BookmarkTreeNode draggedNode;
                BookmarkTreeNode oldNode;
                try {
                    oldNode = (BookmarkTreeNode) event.getTransferable().getTransferData(new DataFlavor(BookmarkTreeNode.class, "BookmarkTreeNode"));
                    draggedNode = new BookmarkTreeNode((BookmarkNodeModel) oldNode.getUserObject());
                } catch (UnsupportedFlavorException | IOException e) {
                    e.printStackTrace();
                    return;
                }

                // 获取目标节点
                // 获取拖放操作的目标路径
                TreePath targetPath = getClosestPathForLocation(event.getLocation().x, event.getLocation().y);
                BookmarkTreeNode targetNode = (BookmarkTreeNode) targetPath.getLastPathComponent();

                // 如果目标节点是叶子节点，则将目标节点的父节点作为目标节点
                if (targetNode.isBookmark()) {
                    targetNode = (BookmarkTreeNode) targetNode.getParent();
                }

                // 在目标节点下添加拖拽节点
                model.insertNodeInto(draggedNode, targetNode, targetNode.getChildCount());
                model.removeNodeFromParent(oldNode);

                // 展开目标节点以显示新的子节点
                expandPath(targetPath);
            }
        });
        setDropTarget(dropTarget);
    }

    private void initCellRenderer() {
        setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean isLeaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, isLeaf, row, hasFocus);
                // 如果节点被选中，则设置背景色为透明
                setBackgroundSelectionColor(null);
                setBorderSelectionColor(null);

                BookmarkTreeNode node = (BookmarkTreeNode) value;
                Icon icon = node.isBookmark()
                        ? IconLoader.getIcon("icons/bookmark.svg", BookmarkTree.class)
                        : AllIcons.Nodes.Folder;
                setIcon(icon);

                return this;
            }
        });
    }

    private void initTreeListeners() {
        // 选中监听
        addTreeSelectionListener(event -> {
            BookmarkTreeNode selectedNode = (BookmarkTreeNode) getLastSelectedPathComponent();
            if (null == selectedNode) {
                return;
            }
            if (selectedNode.isGroup()) {
                navigator.activatedGroup = selectedNode;
                navigator.activatedBookmark = (BookmarkTreeNode) selectedNode.getChildAt(0);
            } else {
                navigator.activeBookmark(selectedNode);
            }
        });

        // 鼠标点击事件
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 双击事件
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    BookmarkTreeNode selectedNode = getEventSourceNode(e);
                    if (selectedNode != null && selectedNode.isBookmark()) {
                        BookmarkNodeModel bookmark = (BookmarkNodeModel) selectedNode.getUserObject();
                        bookmark.getOpenFileDescriptor().navigate(true);
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
        JBMenuItem imAddGroup = new JBMenuItem(I18N.get("addGroup"));
        // TODO 需要添加可以将某个，目录拉出全局显示标签的按钮
        popupMenu.add(imEdit);
        popupMenu.add(imDel);
        popupMenu.add(imAddGroup);

        imEdit.addActionListener(e -> {
            TreePath path = getSelectionPath();
            if (null == path) {
                return;
            }
            BookmarkTreeNode selectedNode = (BookmarkTreeNode) path.getLastPathComponent();
            if (selectedNode.isBookmark()) {
                BookmarkNodeModel nodeModel = (BookmarkNodeModel) selectedNode.getUserObject();
                BookmarksManager.editBookRemark(nodeModel);
            }
        });

        imDel.addActionListener(e -> {
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

                // 从父节点中删除选定的节点
                if (node.isBookmark()) {
                    BookmarkNodeModel bookmark = (BookmarkNodeModel) node.getUserObject();
                }
                remove(node);
            }
        });

        imAddGroup.addActionListener(e -> {
            // 获取选定的节点
            TreePath[] selectionPaths = BookmarkTree.this.getSelectionPaths();

            @SuppressWarnings("all")
            InputValidatorEx validatorEx = inputString -> {
                if (StringUtil.isBlank(inputString))
                    return I18N.get("groupNameNonNullMessage");
                return null;
            };

            if (null == selectionPaths) {
                return;
            }
            for (TreePath path : selectionPaths) {
                BookmarkTreeNode node = (BookmarkTreeNode) path.getLastPathComponent();
                BookmarkTreeNode parent = (BookmarkTreeNode) node.getParent();

                @SuppressWarnings("all")
                String groupName = Messages.showInputDialog(
                        I18N.get("groupNameInputMessage"),
                        I18N.get("groupName"),
                        null,
                        null,
                        validatorEx
                );
                if (StringUtil.isBlank(groupName)) {
                    return;
                }

                BookmarkTreeNode groupNode = new BookmarkTreeNode(new GroupNodeModel(groupName));
                if (parent != null) {
                    groupNode.add(node);
                    model.insertNodeInto(groupNode, parent, 0);
                    model.nodeStructureChanged(parent);
                }
            }
        });

        // 右键点击事件
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (!SwingUtilities.isRightMouseButton(e)) {
                    return;
                }
                int row = getClosestRowForLocation(e.getX(), e.getY());
                setSelectionRow(row);

                TreePath path = getSelectionPath();
                if (null == path) {
                    return;
                }
                BookmarkTreeNode selectedNode = (BookmarkTreeNode) path.getLastPathComponent();

                if (row >= 0 && row < getRowCount()) {
                    popupMenu.show(BookmarkTree.this, e.getX() + 16, e.getY());
                    imAddGroup.setEnabled(selectedNode.isBookmark());
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

    // 自定义Transferable用于在拖拽操作中传输数据
    static class TreeTransferable implements Transferable {
        private final List<TreePath> paths;

        public TreeTransferable(List<TreePath> paths) {
            this.paths = paths;
        }

        public List<TreePath> getPaths() {
            return paths;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.stringFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(DataFlavor.stringFlavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) {
            if (isDataFlavorSupported(flavor)) {
                return paths;
            }
            return null;
        }
    }

}
