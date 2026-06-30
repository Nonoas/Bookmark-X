package indi.bookmarkx.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import indi.bookmarkx.BookmarksManager;
import indi.bookmarkx.common.data.BookmarkArrayListTable;
import indi.bookmarkx.listener.BookmarkListener;
import indi.bookmarkx.model.AbstractTreeNodeModel;
import indi.bookmarkx.model.BookmarkNodeModel;
import indi.bookmarkx.model.GroupNodeModel;
import indi.bookmarkx.service.dto.BookmarkDraft;
import indi.bookmarkx.service.dto.BookmarkTreeItemView;
import indi.bookmarkx.service.dto.BookmarkView;
import indi.bookmarkx.ui.panel.BookmarksManagePanel;
import indi.bookmarkx.ui.tree.BookmarkTree;
import indi.bookmarkx.ui.tree.BookmarkTreeNode;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Service(Service.Level.PROJECT)
public final class BookmarkDomainService {

    private static final long TREE_LOAD_TIMEOUT_MS = 5000L;

    private final Project project;

    public BookmarkDomainService(Project project) {
        this.project = project;
    }

    public static BookmarkDomainService getInstance(Project project) {
        return project.getService(BookmarkDomainService.class);
    }

    public List<BookmarkView> listBookmarks(String filePath, List<String> groupPath) {
        BookmarkContext context = prepareContext();
        return onEdt(() -> doListBookmarks(context.tree, normalizeFilePath(filePath), normalizeGroupPath(groupPath)));
    }

    public BookmarkTreeItemView getBookmarkTree() {
        BookmarkContext context = prepareContext();
        return onEdt(() -> toTreeView((BookmarkTreeNode) context.tree.getModel().getRoot()));
    }

    public BookmarkView createBookmark(BookmarkDraft draft) {
        BookmarkContext context = prepareContext();
        return onEdt(() -> {
            BookmarkView view = doCreateBookmark(context, resolveDraft(context, draft, null, false));
            context.manager.persistentSave();
            return view;
        });
    }

    public List<BookmarkView> batchCreateBookmarks(List<BookmarkDraft> drafts, List<String> defaultGroupPath) {
        BookmarkContext context = prepareContext();
        return onEdt(() -> {
            List<BookmarkView> created = new ArrayList<>();
            for (BookmarkDraft draft : emptyIfNull(drafts)) {
                created.add(doCreateBookmark(context, resolveDraft(context, draft, defaultGroupPath, false)));
            }
            context.manager.persistentSave();
            return created;
        });
    }

    public List<BookmarkView> previewBookmarks(List<BookmarkDraft> drafts, List<String> defaultGroupPath) {
        BookmarkContext context = prepareContext();
        return onEdt(() -> {
            List<BookmarkView> previews = new ArrayList<>();
            for (BookmarkDraft draft : emptyIfNull(drafts)) {
                previews.add(toPreviewView(resolveDraft(context, draft, defaultGroupPath, false)));
            }
            return previews;
        });
    }

    public BookmarkView updateBookmark(BookmarkDraft draft) {
        BookmarkContext context = prepareContext();
        return onEdt(() -> {
            BookmarkView view = doUpdateBookmark(context, draft);
            context.manager.persistentSave();
            return view;
        });
    }

    public BookmarkView deleteBookmark(String uuid) {
        BookmarkContext context = prepareContext();
        return onEdt(() -> {
            BookmarkView deleted = doDeleteBookmark(context, uuid);
            context.manager.persistentSave();
            return deleted;
        });
    }

    public List<BookmarkView> deleteBookmarks(List<String> uuids) {
        BookmarkContext context = prepareContext();
        return onEdt(() -> {
            List<BookmarkView> deleted = new ArrayList<>();
            for (String uuid : emptyIfNull(uuids)) {
                deleted.add(doDeleteBookmark(context, uuid));
            }
            context.manager.persistentSave();
            return deleted;
        });
    }

    public List<BookmarkView> reorderInGroup(List<String> groupPath, List<String> orderedUuids) {
        BookmarkContext context = prepareContext();
        return onEdt(() -> {
            BookmarkTreeNode groupNode = requireGroupNode(context.tree, groupPath);
            assertFlatBookmarkGroup(groupNode);
            List<String> currentOrder = listDirectBookmarkUuids(groupNode);
            List<String> desiredOrder = BookmarkOrderPlanner.reorder(currentOrder, orderedUuids);
            applyBookmarkOrder(context.tree, groupNode, desiredOrder);
            context.manager.persistentSave();
            return listBookmarksInGroup(groupNode);
        });
    }

    public List<BookmarkView> moveInGroup(List<String> groupPath, String uuid, int targetIndex) {
        BookmarkContext context = prepareContext();
        return onEdt(() -> {
            BookmarkTreeNode groupNode = requireGroupNode(context.tree, groupPath);
            assertFlatBookmarkGroup(groupNode);
            List<String> currentOrder = listDirectBookmarkUuids(groupNode);
            List<String> desiredOrder = BookmarkOrderPlanner.move(currentOrder, uuid, targetIndex);
            applyBookmarkOrder(context.tree, groupNode, desiredOrder);
            context.manager.persistentSave();
            return listBookmarksInGroup(groupNode);
        });
    }

    public List<BookmarkView> upsertReadingTour(List<String> defaultGroupPath, List<BookmarkDraft> drafts) {
        BookmarkContext context = prepareContext();
        return onEdt(() -> {
            List<BookmarkView> bookmarks = new ArrayList<>();
            List<String> orderedUuids = new ArrayList<>();
            BookmarkTreeNode targetGroup = null;
            for (BookmarkDraft draft : emptyIfNull(drafts)) {
                ResolvedDraft resolved = resolveDraft(context, draft, defaultGroupPath, true);
                BookmarkView view = resolved.existingNode == null
                        ? doCreateBookmark(context, resolved)
                        : doUpdateBookmark(context, toUpdateDraft(resolved));
                bookmarks.add(view);
                orderedUuids.add(view.getUuid());
                if (targetGroup == null) {
                    targetGroup = ensureGroupNode(context.tree, resolved.groupPath);
                }
            }
            if (targetGroup != null) {
                assertFlatBookmarkGroup(targetGroup);
                applyBookmarkOrder(context.tree, targetGroup, BookmarkOrderPlanner.reorder(listDirectBookmarkUuids(targetGroup), orderedUuids));
            }
            context.manager.persistentSave();
            return bookmarks;
        });
    }

    private BookmarkView doCreateBookmark(BookmarkContext context, ResolvedDraft resolved) {
        BookmarkTreeNode existingNode = resolved.existingNode;
        if (existingNode != null) {
            BookmarkDraft updateDraft = toUpdateDraft(resolved);
            return doUpdateBookmark(context, updateDraft);
        }

        BookmarkTreeNode groupNode = ensureGroupNode(context.tree, resolved.groupPath);
        BookmarkNodeModel bookmarkNodeModel = new BookmarkNodeModel();
        bookmarkNodeModel.setUuid(UUID.randomUUID().toString());
        bookmarkNodeModel.setName(resolved.name);
        bookmarkNodeModel.setDesc(resolved.desc);
        bookmarkNodeModel.setLine(resolved.internalLine);
        bookmarkNodeModel.setIcon(resolved.file.getFileType().getIcon());
        bookmarkNodeModel.setOpenFileDescriptor(new OpenFileDescriptor(project, resolved.file, resolved.internalLine, 0));

        BookmarkTreeNode treeNode = new BookmarkTreeNode(bookmarkNodeModel);
        context.tree.insertNodeInto(treeNode, groupNode, groupNode.getChildCount());
        refreshBookmarkIndices(groupNode);
        publishEvent(listener -> listener.bookmarkAdded(bookmarkNodeModel));
        return toBookmarkView(treeNode);
    }

    private BookmarkView doUpdateBookmark(BookmarkContext context, BookmarkDraft draft) {
        if (draft == null || StringUtil.isEmptyOrSpaces(draft.getUuid())) {
            throw new IllegalArgumentException("Bookmark UUID is required for update");
        }
        BookmarkTreeNode bookmarkNode = requireBookmarkNode(context.tree, draft.getUuid());
        BookmarkNodeModel bookmark = (BookmarkNodeModel) bookmarkNode.getUserObject();
        BookmarkTreeNode currentGroup = (BookmarkTreeNode) bookmarkNode.getParent();

        if (draft.getName() != null) {
            bookmark.setName(draft.getName());
        }
        if (draft.getDesc() != null) {
            bookmark.setDesc(draft.getDesc());
        }
        if (draft.getLine() != null) {
            int newInternalLine = normalizeLine(resolveFile(bookmark.getFilePath()
                    .orElseThrow(() -> new IllegalArgumentException("Bookmark file does not exist"))), draft.getLine());
            BookmarkTreeNode duplicate = findBookmarkNodeByLocation(context.tree,
                    bookmark.getFilePath().orElse(null),
                    newInternalLine);
            if (duplicate != null && duplicate != bookmarkNode) {
                throw new IllegalArgumentException("Another bookmark already exists at the requested file and line");
            }
            bookmark.updateBookmarkLine(newInternalLine, false);
        }

        List<String> requestedGroupPath = draft.getGroupPath() == null ? null : normalizeGroupPath(draft.getGroupPath());
        if (requestedGroupPath != null) {
            BookmarkTreeNode targetGroup = ensureGroupNode(context.tree, requestedGroupPath);
            if (targetGroup != currentGroup) {
                context.tree.moveNode(bookmarkNode, targetGroup, targetGroup.getChildCount());
                refreshBookmarkIndices(currentGroup);
                refreshBookmarkIndices(targetGroup);
            } else {
                refreshBookmarkIndices(currentGroup);
            }
        } else {
            refreshBookmarkIndices(currentGroup);
        }

        publishEvent(listener -> listener.bookmarkChanged(bookmark));
        return toBookmarkView(bookmarkNode);
    }

    private BookmarkView doDeleteBookmark(BookmarkContext context, String uuid) {
        BookmarkTreeNode bookmarkNode = requireBookmarkNode(context.tree, uuid);
        BookmarkTreeNode parent = (BookmarkTreeNode) bookmarkNode.getParent();
        BookmarkView deleted = toBookmarkView(bookmarkNode);
        AbstractTreeNodeModel model = (AbstractTreeNodeModel) bookmarkNode.getUserObject();
        context.tree.removeNodeFromParent(bookmarkNode);
        refreshBookmarkIndices(parent);
        publishEvent(listener -> listener.bookmarkRemoved(model));
        return deleted;
    }

    private List<BookmarkView> doListBookmarks(BookmarkTree tree, String filePath, List<String> groupPath) {
        List<BookmarkView> bookmarks = new ArrayList<>();
        BookmarkTreeNode root = (BookmarkTreeNode) tree.getModel().getRoot();
        collectBookmarks(root, bookmarks, filePath, groupPath);
        return bookmarks;
    }

    private void collectBookmarks(BookmarkTreeNode node, List<BookmarkView> bookmarks, String filePath, List<String> groupPath) {
        if (node.isBookmark()) {
            BookmarkView view = toBookmarkView(node);
            boolean matchFile = filePath == null || Objects.equals(filePath, view.getFilePath());
            boolean matchGroup = groupPath == null || Objects.equals(groupPath, view.getGroupPath());
            if (matchFile && matchGroup) {
                bookmarks.add(view);
            }
            return;
        }
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            collectBookmarks((BookmarkTreeNode) node.getChildAt(i), bookmarks, filePath, groupPath);
        }
    }

    private BookmarkTreeItemView toTreeView(BookmarkTreeNode node) {
        BookmarkTreeItemView view = new BookmarkTreeItemView();
        AbstractTreeNodeModel model = (AbstractTreeNodeModel) node.getUserObject();
        view.setUuid(model.getUuid());
        view.setName(model.getName());
        view.setDesc(model.getDesc());
        view.setType(node.isBookmark() ? "bookmark" : "group");
        if (node.isBookmark()) {
            BookmarkView bookmarkView = toBookmarkView(node);
            view.setFilePath(bookmarkView.getFilePath());
            view.setLine(bookmarkView.getLine());
            view.setIndexInGroup(bookmarkView.getIndexInGroup());
            view.setGroupPath(bookmarkView.getGroupPath());
            return view;
        }

        List<BookmarkTreeItemView> children = new ArrayList<>();
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            children.add(toTreeView((BookmarkTreeNode) node.getChildAt(i)));
        }
        view.setGroupPath(groupPathOf(node));
        view.setChildren(children);
        return view;
    }

    private BookmarkView toBookmarkView(BookmarkTreeNode node) {
        BookmarkNodeModel model = (BookmarkNodeModel) node.getUserObject();
        BookmarkView view = new BookmarkView();
        view.setUuid(model.getUuid());
        view.setName(model.getName());
        view.setDesc(model.getDesc());
        view.setFilePath(model.getFilePath().orElse(null));
        view.setLine(model.getLine() + 1);
        view.setIndexInGroup(node.getParent() == null ? 0 : node.getParent().getIndex(node));
        view.setGroupPath(groupPathOf((BookmarkTreeNode) node.getParent()));
        return view;
    }

    private BookmarkView toPreviewView(ResolvedDraft resolved) {
        BookmarkView preview = new BookmarkView();
        preview.setUuid(resolved.existingNode == null ? null : ((BookmarkNodeModel) resolved.existingNode.getUserObject()).getUuid());
        preview.setName(resolved.name);
        preview.setDesc(resolved.desc);
        preview.setFilePath(resolved.file.getPath());
        preview.setLine(resolved.internalLine + 1);
        preview.setIndexInGroup(-1);
        preview.setGroupPath(resolved.groupPath);
        return preview;
    }

    private BookmarkDraft toUpdateDraft(ResolvedDraft resolved) {
        BookmarkDraft draft = new BookmarkDraft();
        BookmarkNodeModel existing = (BookmarkNodeModel) resolved.existingNode.getUserObject();
        draft.setUuid(existing.getUuid());
        draft.setName(resolved.name);
        draft.setDesc(resolved.desc);
        draft.setLine(resolved.internalLine + 1);
        draft.setGroupPath(resolved.groupPath);
        return draft;
    }

    private BookmarkTreeNode ensureGroupNode(BookmarkTree tree, List<String> groupPath) {
        BookmarkTreeNode current = (BookmarkTreeNode) tree.getModel().getRoot();
        for (String segment : normalizeGroupPath(groupPath)) {
            BookmarkTreeNode next = findDirectChildGroup(current, segment);
            if (next == null) {
                GroupNodeModel groupNodeModel = new GroupNodeModel();
                groupNodeModel.setUuid(UUID.randomUUID().toString());
                groupNodeModel.setName(segment);
                groupNodeModel.setDesc("");
                next = new BookmarkTreeNode(groupNodeModel);
                tree.insertNodeInto(next, current, current.getChildCount());
            }
            current = next;
        }
        return current;
    }

    private BookmarkTreeNode requireGroupNode(BookmarkTree tree, List<String> groupPath) {
        BookmarkTreeNode current = (BookmarkTreeNode) tree.getModel().getRoot();
        for (String segment : normalizeGroupPath(groupPath)) {
            BookmarkTreeNode next = findDirectChildGroup(current, segment);
            if (next == null) {
                throw new IllegalArgumentException("Bookmark group does not exist: " + String.join("/", normalizeGroupPath(groupPath)));
            }
            current = next;
        }
        return current;
    }

    private BookmarkTreeNode requireBookmarkNode(BookmarkTree tree, String uuid) {
        BookmarkTreeNode node = findBookmarkNode(tree, uuid);
        if (node == null) {
            throw new IllegalArgumentException("Bookmark does not exist: " + uuid);
        }
        return node;
    }

    private BookmarkTreeNode findBookmarkNode(BookmarkTree tree, String uuid) {
        if (StringUtil.isEmptyOrSpaces(uuid)) {
            return null;
        }
        return findBookmarkNodeRecursive((BookmarkTreeNode) tree.getModel().getRoot(), uuid);
    }

    private BookmarkTreeNode findBookmarkNodeRecursive(BookmarkTreeNode node, String uuid) {
        if (node.isBookmark()) {
            BookmarkNodeModel model = (BookmarkNodeModel) node.getUserObject();
            return Objects.equals(model.getUuid(), uuid) ? node : null;
        }
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            BookmarkTreeNode found = findBookmarkNodeRecursive((BookmarkTreeNode) node.getChildAt(i), uuid);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private BookmarkTreeNode findBookmarkNodeByLocation(BookmarkTree tree, String filePath, int internalLine) {
        if (filePath == null) {
            return null;
        }
        for (BookmarkNodeModel model : BookmarkArrayListTable.getInstance(project).findByFilePath(filePath)) {
            if (model.getLine() == internalLine) {
                return tree.getNodeByModel(model);
            }
        }
        return null;
    }

    private BookmarkTreeNode findDirectChildGroup(BookmarkTreeNode parent, String name) {
        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            BookmarkTreeNode child = (BookmarkTreeNode) parent.getChildAt(i);
            if (child.isGroup()) {
                AbstractTreeNodeModel model = (AbstractTreeNodeModel) child.getUserObject();
                if (Objects.equals(name, model.getName())) {
                    return child;
                }
            }
        }
        return null;
    }

    private void applyBookmarkOrder(BookmarkTree tree, BookmarkTreeNode groupNode, List<String> orderedUuids) {
        List<BookmarkTreeNode> bookmarkNodes = listDirectBookmarkNodes(groupNode);
        if (bookmarkNodes.size() != orderedUuids.size()) {
            throw new IllegalArgumentException("Target group bookmark count changed while applying order");
        }
        for (BookmarkTreeNode node : new ArrayList<>(bookmarkNodes)) {
            tree.removeNodeFromParent(node);
        }
        for (int i = 0; i < orderedUuids.size(); i++) {
            String uuid = orderedUuids.get(i);
            BookmarkTreeNode node = findByUuid(bookmarkNodes, uuid);
            tree.insertNodeInto(node, groupNode, i);
        }
        refreshBookmarkIndices(groupNode);
    }

    private BookmarkTreeNode findByUuid(List<BookmarkTreeNode> nodes, String uuid) {
        for (BookmarkTreeNode node : nodes) {
            BookmarkNodeModel model = (BookmarkNodeModel) node.getUserObject();
            if (Objects.equals(uuid, model.getUuid())) {
                return node;
            }
        }
        throw new IllegalArgumentException("Bookmark UUID does not belong to the target group: " + uuid);
    }

    private List<BookmarkView> listBookmarksInGroup(BookmarkTreeNode groupNode) {
        List<BookmarkView> views = new ArrayList<>();
        for (BookmarkTreeNode node : listDirectBookmarkNodes(groupNode)) {
            views.add(toBookmarkView(node));
        }
        return views;
    }

    private List<BookmarkTreeNode> listDirectBookmarkNodes(BookmarkTreeNode groupNode) {
        List<BookmarkTreeNode> nodes = new ArrayList<>();
        int childCount = groupNode.getChildCount();
        for (int i = 0; i < childCount; i++) {
            BookmarkTreeNode child = (BookmarkTreeNode) groupNode.getChildAt(i);
            if (child.isBookmark()) {
                nodes.add(child);
            }
        }
        return nodes;
    }

    private List<String> listDirectBookmarkUuids(BookmarkTreeNode groupNode) {
        List<String> uuids = new ArrayList<>();
        for (BookmarkTreeNode node : listDirectBookmarkNodes(groupNode)) {
            uuids.add(((BookmarkNodeModel) node.getUserObject()).getUuid());
        }
        return uuids;
    }

    private void assertFlatBookmarkGroup(BookmarkTreeNode groupNode) {
        int childCount = groupNode.getChildCount();
        for (int i = 0; i < childCount; i++) {
            if (((BookmarkTreeNode) groupNode.getChildAt(i)).isGroup()) {
                throw new IllegalArgumentException("Reordering is only supported for groups that directly contain bookmarks");
            }
        }
    }

    private void refreshBookmarkIndices(BookmarkTreeNode groupNode) {
        if (groupNode == null) {
            return;
        }
        int childCount = groupNode.getChildCount();
        for (int i = 0; i < childCount; i++) {
            BookmarkTreeNode child = (BookmarkTreeNode) groupNode.getChildAt(i);
            if (child.isBookmark()) {
                BookmarkNodeModel bookmarkNodeModel = (BookmarkNodeModel) child.getUserObject();
                bookmarkNodeModel.setIndex(groupNode.getIndex(child));
            }
        }
    }

    private ResolvedDraft resolveDraft(BookmarkContext context, BookmarkDraft draft, List<String> defaultGroupPath, boolean allowExistingUuid) {
        if (draft == null) {
            throw new IllegalArgumentException("Bookmark draft cannot be null");
        }
        VirtualFile file = resolveFile(draft.getFilePath());
        int internalLine = normalizeLine(file, requireLine(draft));
        List<String> groupPath = draft.getGroupPath() == null || draft.getGroupPath().isEmpty()
                ? normalizeGroupPath(defaultGroupPath)
                : normalizeGroupPath(draft.getGroupPath());
        String name = StringUtil.isEmptyOrSpaces(draft.getName()) ? file.getName() : draft.getName().trim();
        String desc = draft.getDesc() == null ? "" : draft.getDesc();

        BookmarkTreeNode existingNode = null;
        if (allowExistingUuid && !StringUtil.isEmptyOrSpaces(draft.getUuid())) {
            existingNode = findBookmarkNode(context.tree, draft.getUuid());
        }
        if (existingNode == null) {
            existingNode = findBookmarkNodeByLocation(context.tree, file.getPath(), internalLine);
        }
        return new ResolvedDraft(file, internalLine, name, desc, groupPath, existingNode);
    }

    private int requireLine(BookmarkDraft draft) {
        if (draft.getLine() == null) {
            throw new IllegalArgumentException("Bookmark line is required");
        }
        return draft.getLine();
    }

    private VirtualFile resolveFile(String rawPath) {
        String normalizedPath = normalizeFilePath(rawPath);
        if (normalizedPath == null) {
            throw new IllegalArgumentException("Bookmark filePath is required");
        }
        VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(normalizedPath));
        if (file == null) {
            file = LocalFileSystem.getInstance().findFileByPath(normalizedPath);
        }
        if (file == null) {
            throw new IllegalArgumentException("Bookmark file does not exist: " + normalizedPath);
        }
        return file;
    }

    private String normalizeFilePath(String rawPath) {
        if (StringUtil.isEmptyOrSpaces(rawPath)) {
            return null;
        }
        String path = rawPath.trim().replace("$PROJECT_DIR$", Optional.ofNullable(project.getBasePath()).orElse(""));
        File file = new File(path);
        if (!file.isAbsolute()) {
            String basePath = project.getBasePath();
            if (StringUtil.isEmptyOrSpaces(basePath)) {
                throw new IllegalArgumentException("Project base path is unavailable");
            }
            file = new File(basePath, path);
        }
        return file.getAbsolutePath().replace('\\', '/');
    }

    private int normalizeLine(VirtualFile file, int oneBasedLine) {
        if (oneBasedLine < 1) {
            throw new IllegalArgumentException("Bookmark line must be >= 1");
        }
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) {
            return oneBasedLine - 1;
        }
        int maxLine = Math.max(document.getLineCount(), 1);
        return Math.min(oneBasedLine, maxLine) - 1;
    }

    private List<String> normalizeGroupPath(List<String> groupPath) {
        if (groupPath == null) {
            return Collections.emptyList();
        }
        List<String> normalized = new ArrayList<>();
        for (String segment : groupPath) {
            if (StringUtil.isEmptyOrSpaces(segment)) {
                continue;
            }
            normalized.add(segment.trim());
        }
        return normalized;
    }

    private List<String> groupPathOf(BookmarkTreeNode groupNode) {
        if (groupNode == null) {
            return Collections.emptyList();
        }
        List<String> segments = new ArrayList<>();
        BookmarkTreeNode current = groupNode;
        while (current != null && current.getParent() != null) {
            if (current.isGroup()) {
                AbstractTreeNodeModel model = (AbstractTreeNodeModel) current.getUserObject();
                segments.add(model.getName());
            }
            current = (BookmarkTreeNode) current.getParent();
        }
        Collections.reverse(segments);
        return segments;
    }

    private BookmarkContext prepareContext() {
        BookmarksManager manager = onEdt(() -> BookmarksManager.getInstance(project));
        BookmarksManagePanel panel = manager.getToolWindowRootPanel();
        waitForTreeLoad(panel);
        BookmarkTree tree = onEdt(panel::tree);
        return new BookmarkContext(manager, tree);
    }

    private void waitForTreeLoad(BookmarksManagePanel panel) {
        if (panel.isTreeLoaded()) {
            return;
        }
        if (ApplicationManager.getApplication().isDispatchThread()) {
            throw new IllegalStateException("Bookmark tree is still loading");
        }
        long deadline = System.currentTimeMillis() + TREE_LOAD_TIMEOUT_MS;
        while (!panel.isTreeLoaded() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for bookmark tree to load", e);
            }
        }
        if (!panel.isTreeLoaded()) {
            throw new IllegalStateException("Bookmark tree did not finish loading in time");
        }
    }

    private void publishEvent(BookmarkEventConsumer consumer) {
        BookmarkListener listener = project.getMessageBus().syncPublisher(BookmarkListener.TOPIC);
        consumer.accept(listener);
    }

    private <T> T onEdt(ThrowableSupplier<T> supplier) {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            return getOrThrow(supplier);
        }
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<RuntimeException> error = new AtomicReference<>();
        ApplicationManager.getApplication().invokeAndWait(() -> {
            try {
                result.set(supplier.get());
            } catch (RuntimeException ex) {
                error.set(ex);
            } catch (Exception ex) {
                error.set(new IllegalStateException(ex));
            }
        });
        if (error.get() != null) {
            throw error.get();
        }
        return result.get();
    }

    private <T> T getOrThrow(ThrowableSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private <T> List<T> emptyIfNull(List<T> items) {
        return items == null ? List.of() : items;
    }

    @FunctionalInterface
    private interface ThrowableSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    private interface BookmarkEventConsumer {
        void accept(BookmarkListener listener);
    }

    private static final class BookmarkContext {
        private final BookmarksManager manager;
        private final BookmarkTree tree;

        private BookmarkContext(BookmarksManager manager, BookmarkTree tree) {
            this.manager = manager;
            this.tree = tree;
        }
    }

    private static final class ResolvedDraft {
        private final VirtualFile file;
        private final int internalLine;
        private final String name;
        private final String desc;
        private final List<String> groupPath;
        private final BookmarkTreeNode existingNode;

        private ResolvedDraft(VirtualFile file, int internalLine, String name, String desc,
                              List<String> groupPath, BookmarkTreeNode existingNode) {
            this.file = file;
            this.internalLine = internalLine;
            this.name = name;
            this.desc = desc;
            this.groupPath = groupPath;
            this.existingNode = existingNode;
        }
    }
}
