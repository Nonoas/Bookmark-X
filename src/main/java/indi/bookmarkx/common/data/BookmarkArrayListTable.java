package indi.bookmarkx.common.data;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import indi.bookmarkx.listener.BookmarkListener;
import indi.bookmarkx.model.AbstractTreeNodeModel;
import indi.bookmarkx.model.BookmarkNodeModel;
import indi.bookmarkx.ui.tree.BookmarkTree;
import indi.bookmarkx.ui.tree.BookmarkTreeNode;
import indi.bookmarkx.utils.PersistenceUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

/**
 * A project-level service that maintains an in-memory table of bookmarks for fast lookup.
 * This class serves as both a data container and a bookmark event subscriber to keep
 * the index synchronized with bookmark changes.
 * <p>
 * The table maintains multiple column indices to support efficient querying by different
 * attributes (file path and UUID).
 * <p>
 * <strong>Note:</strong> This service implements {@link Disposable} to properly manage
 * the message bus connection lifecycle and prevent memory leaks.
 *
 * @author codeleep
 * @createTime 2024/03/20 14:58
 */
@Service(Service.Level.PROJECT)
public final class BookmarkArrayListTable extends ArrayListTable<BookmarkNodeModel> implements BookmarkListener {

    /**
     * The message bus connection for subscribing to bookmark events.
     * Stored as a field to ensure proper cleanup when the service is disposed.
     */
    private final MessageBusConnection messageBusConnection;

    /**
     * Returns the singleton instance of this service for the given project.
     *
     * @param project the IntelliJ project instance
     * @return the BookmarkArrayListTable service instance
     */
    public static BookmarkArrayListTable getInstance(Project project) {
        return project.getService(BookmarkArrayListTable.class);
    }

    public BookmarkArrayListTable(Project project) {
        super(new ArrayList<>(), getColumnIndexFunctions());
        // Create a connection that will be automatically disconnected when this service is disposed
        this.messageBusConnection = project.getMessageBus().connect(project);
        this.messageBusConnection.subscribe(TOPIC, this);
    }

    /**
     * Initializes the table data from an existing bookmark tree.
     * This method populates the in-memory table and rebuilds all indices.
     *
     * @param bookmarkTree the bookmark tree containing the data to load
     */
    public void initData(BookmarkTree bookmarkTree) {
        this.dataList = treeToList(bookmarkTree, new ArrayList<>());
        columnIndices.keySet().forEach(super::addColumIndex);
    }

    public List<BookmarkNodeModel> findByFilePath(String filePath) {
        if (filePath == null) {
            return List.of();
        }
        LinkedHashSet<BookmarkNodeModel> bookmarks = new LinkedHashSet<>();
        for (BookmarkNodeModel model : dataList) {
            if (model == null) {
                continue;
            }
            if (Objects.equals(filePath, model.getFilePath().orElse(null))) {
                bookmarks.add(model);
            }
        }
        return new ArrayList<>(bookmarks);
    }

    public Optional<BookmarkNodeModel> findByUuid(String uuid) {
        if (uuid == null) {
            return Optional.empty();
        }
        for (BookmarkNodeModel model : dataList) {
            if (model == null) {
                continue;
            }
            if (uuid.equals(model.getUuid())) {
                return Optional.of(model);
            }
        }
        return Optional.empty();
    }

    public List<BookmarkNodeModel> listAll() {
        return new ArrayList<>(new LinkedHashSet<>(dataList));
    }

    /**
     * Returns the list of column index functions used to build lookup indices.
     * Currently supports indexing by file path and UUID.
     *
     * @return list of index functions
     */
    private static List<Function<BookmarkNodeModel, String>> getColumnIndexFunctions() {
        ArrayList<Function<BookmarkNodeModel, String>> functions = new ArrayList<>();
        functions.add(lineColumnIndex());
        functions.add(uuIdColumnIndex());
        return functions;
    }

    /**
     * Returns a function that extracts the file path from a bookmark for indexing.
     * This enables fast lookup of all bookmarks in a specific file.
     *
     * @return function extracting file path from bookmark
     */
    private static Function<BookmarkNodeModel, String> lineColumnIndex() {
        return bookmarkPO -> {
            OpenFileDescriptor descriptor = bookmarkPO.getOpenFileDescriptor();
            if (null == descriptor) {
                return null;
            }
            return descriptor.getFile().getPath();
        };
    }

    /**
     * Returns a function that extracts the UUID from a bookmark for indexing.
     * This enables fast lookup of bookmarks by their unique identifier.
     *
     * @return function extracting UUID from bookmark
     */
    private static Function<BookmarkNodeModel, String> uuIdColumnIndex() {
        return BookmarkNodeModel::getUuid;
    }

    /**
     * Converts a bookmark tree to a flat list of bookmark models.
     * This is used during initialization to populate the in-memory table.
     *
     * @param bookmarkTree the tree to convert
     * @param list         the list to populate (used for recursion)
     * @return the populated list of bookmark models
     */
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
    public void bookmarkAdded(@NotNull AbstractTreeNodeModel model) {
        bookmarkChanged(model);
    }

    /**
     * Handles bookmark update events by re-indexing the modified bookmark.
     * This ensures the lookup indices remain consistent after a bookmark changes.
     *
     * @param model the bookmark model that was updated
     */
    @Override
    public void bookmarkChanged(@NotNull AbstractTreeNodeModel model) {
        if (model.isBookmark()) {
            this.insert((BookmarkNodeModel) model);
        }
    }

    /**
     * Handles bookmark removal events by removing the bookmark from indices.
     *
     * @param model the bookmark model that was removed
     */
    @Override
    public void bookmarkRemoved(@NotNull AbstractTreeNodeModel model) {
        if (model.isBookmark()) {
            this.delete((BookmarkNodeModel) model);
        }
    }
}
