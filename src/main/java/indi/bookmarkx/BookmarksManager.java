package indi.bookmarkx;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import indi.bookmarkx.common.I18N;
import indi.bookmarkx.common.data.BookmarkArrayListTable;
import indi.bookmarkx.global.FileMarksCache;
import indi.bookmarkx.listener.BookmarkListener;
import indi.bookmarkx.model.AbstractTreeNodeModel;
import indi.bookmarkx.model.BookmarkConverter;
import indi.bookmarkx.model.BookmarkNodeModel;
import indi.bookmarkx.model.po.BookmarkPO;
import indi.bookmarkx.persistence.MyPersistent;
import indi.bookmarkx.ui.dialog.BookmarkCreatorDialog;
import indi.bookmarkx.ui.painter.LineEndPainter;
import indi.bookmarkx.ui.pannel.BookmarksManagePanel;
import indi.bookmarkx.ui.tree.BookmarkTreeNode;
import indi.bookmarkx.utils.FileLineCounter;
import indi.bookmarkx.utils.PersistenceUtil;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultTreeModel;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 项目级别的管理器（命令模式）：用于管理所有「书签UI」的变化，发布书签变更的消息，是一切用户操作的入口，管理所有数据及 UI 的引用
 */
@Service(Service.Level.PROJECT)
public final class BookmarksManager {

    private static final Logger LOG = Logger.getInstance(BookmarksManagePanel.class);

    public Project project;

    private final BookmarksManagePanel toolWindowRootPanel;

    private final BookmarkArrayListTable bookmarkArrayListTable;

    private final FileMarksCache fileMarksCache = new FileMarksCache();

    public BookmarksManager(Project project) {
        this.project = project;
        this.toolWindowRootPanel = BookmarksManagePanel.create(project);
        bookmarkArrayListTable = BookmarkArrayListTable.getInstance(project);
        reload();
    }

    public static BookmarksManager getInstance(Project project) {
        return project.getService(BookmarksManager.class);
    }

    /**
     * 创建一个书签
     *
     * @param project 项目
     * @param editor  编辑器
     * @param file    文件
     */
    public void createBookRemark(Project project, Editor editor, VirtualFile file) {
        CaretModel caretModel = editor.getCaretModel();
        // 获取行号
        int line = caretModel.getLogicalPosition().line;
        String selectedText = caretModel.getCurrentCaret().getSelectedText();
        createBookRemark(project, file, selectedText, line);
    }

    /**
     * 创建书签
     *
     * @param project  项目
     * @param file     添加标签的文件
     * @param descText 描述文本
     * @param line     文件行
     */
    public void createBookRemark(Project project, VirtualFile file, String descText, int line) {
        BookmarkNodeModel bookmarkNodeModel = LineEndPainter.findLine(BookmarkArrayListTable.getInstance(project).getOnlyIndex(file.getPath()), line);
        String defaultName = file.getName();
        String defaultDesc;
        boolean add = true;
        if (bookmarkNodeModel == null) {
            // 获取选中文本
            defaultDesc = Optional.ofNullable(descText).map(text -> (" " + text + " ")).orElse("");
            String uuid = UUID.randomUUID().toString();
            bookmarkNodeModel = new BookmarkNodeModel();
            bookmarkNodeModel.setUuid(uuid);
            bookmarkNodeModel.setLine(line);
            bookmarkNodeModel.setIcon(file.getFileType().getIcon());
            bookmarkNodeModel.setIcon(file.getFileType().getIcon());
            bookmarkNodeModel.setOpenFileDescriptor(new OpenFileDescriptor(project, file, line, 0));
        } else {
            add = false;
            defaultName = bookmarkNodeModel.getName();
            defaultDesc = bookmarkNodeModel.getDesc();
        }
        final BookmarkNodeModel finalBookmarkNodeModel = bookmarkNodeModel;
        final boolean addFlag = add;
        new BookmarkCreatorDialog(project, I18N.get("bookmark.create.title"))
                .defaultName(defaultName)
                .defaultDesc(defaultDesc)
                .showAndCallback((name, desc, lineNumber) -> {
                    finalBookmarkNodeModel.setName(name);
                    finalBookmarkNodeModel.setDesc(desc);
                    bookmarkArrayListTable.insert(finalBookmarkNodeModel);
                    if (addFlag) {
                        submitCreateBookRemark(finalBookmarkNodeModel);
                    } else {
                        if (Objects.nonNull(toolWindowRootPanel)) {
                            toolWindowRootPanel.treeNodesChanged(finalBookmarkNodeModel);
                        }
                    }

                });
    }

    public void editBookRemark(AbstractTreeNodeModel nodeModel) {
        int lineNumber = -1;
        int maxLineNumber = -1;
        if (nodeModel instanceof BookmarkNodeModel) {
            BookmarkNodeModel bookmarkModel = (BookmarkNodeModel) nodeModel;
            lineNumber = bookmarkModel.getLine() + 1; // 界面显示从1开始
            OpenFileDescriptor descriptor = bookmarkModel.getOpenFileDescriptor();
            maxLineNumber = FileLineCounter.getFileMaxLine(descriptor);
        }

        new BookmarkCreatorDialog(project, I18N.get("bookmark.create.title"), lineNumber, maxLineNumber)
                .defaultName(nodeModel.getName())
                .defaultDesc(nodeModel.getDesc())
                .showAndCallback((name, desc, lineNo) -> {
                    nodeModel.setName(name);
                    nodeModel.setDesc(desc);

                    // 如果是书签节点且行号有修改，更新行号
                    if (nodeModel instanceof BookmarkNodeModel && lineNo != null) {
                        BookmarkNodeModel bookmarkModel = (BookmarkNodeModel) nodeModel;
                        int oldLine = bookmarkModel.getLine();
                        if (oldLine != lineNo) {
                            bookmarkModel.setLine(lineNo);
                            // 更新文件描述符的行号
                            OpenFileDescriptor descriptor = bookmarkModel.getOpenFileDescriptor();
                            if (descriptor != null) {
                                bookmarkModel.setOpenFileDescriptor(
                                        new OpenFileDescriptor(descriptor.getProject(), descriptor.getFile(), lineNo, 0)
                                );
                            }
                            bookmarkModel.release();
                            bookmarkModel.createLineMarker();
                            // getBookmarkPublisher(project).bookmarkRemoved(nodeModel);
                            // getBookmarkPublisher(project).bookmarkAdded(nodeModel);
                        }
                    }

                    getBookmarkPublisher(project).bookmarkChanged(nodeModel);
                });
    }

    /**
     * 删除书签
     *
     * @param model 需要删除的书签
     */
    public void removeBookRemark(AbstractTreeNodeModel model) {
        getBookmarkPublisher(project).bookmarkRemoved(model);
    }

    @NotNull
    private BookmarkListener getBookmarkPublisher(Project project) {
        return project.getMessageBus().syncPublisher(BookmarkListener.TOPIC);
    }

    private void submitCreateBookRemark(BookmarkNodeModel bookmarkModel) {
        //  The toolWindowRootPanel may be null the first time IDEA is opened
        if (Objects.isNull(toolWindowRootPanel)) {
            MyPersistent persistent = MyPersistent.getInstance(project);
            persistent.getState().getChildren().add(BookmarkConverter.convertToPO(bookmarkModel));
        } else {
            afterCreateSubmit(bookmarkModel);
        }

    }

    private void afterCreateSubmit(BookmarkNodeModel bookmarkModel) {
        addToTree(bookmarkModel);
    }

    /**
     * 持久化保存
     */
    public void persistentSave() {
        PersistenceUtil.persistentSave(project, toolWindowRootPanel.tree());
    }

    private void addToTree(BookmarkNodeModel bookmarkModel) {
        toolWindowRootPanel.addAndGet(bookmarkModel);
        getBookmarkPublisher(project).bookmarkAdded(bookmarkModel);
    }

    public void prev() {
        toolWindowRootPanel.prev();
    }

    public void next() {
        toolWindowRootPanel.next();
    }

    /**
     * 重新加载标签树
     */
    public void reload() {
        ProgressManager.getInstance().run(new TreeLoadTask(project, this));
    }

    public FileMarksCache getFileMarksCache() {
        return fileMarksCache;
    }

    public BookmarksManagePanel getToolWindowRootPanel() {
        return toolWindowRootPanel;
    }

    /**
     * 数据加载任务
     */
    static class TreeLoadTask extends Task.Backgroundable {

        private final Project project;
        private DefaultTreeModel treeModel;
        private final BookmarksManager bookmarksManager;

        public TreeLoadTask(Project project, BookmarksManager bookmarksManager) {
            super(project, "Loading Tree Data");
            this.project = project;
            this.bookmarksManager = bookmarksManager;
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            try {
                MyPersistent persistent = MyPersistent.getInstance(project);
                BookmarkPO rootPO = persistent.getState();
                BookmarkTreeNode root = PersistenceUtil.generateTreeNode(rootPO, project);
                treeModel = new DefaultTreeModel(root);

                // 初始化文件到标签签行缓存
                reIntiFileMarksCache(root);
                registerFileGutterIconListener();
            } catch (Exception e) {
                // 错误处理
                LOG.error("初始化标签树失败", e);
            }
            LOG.info("初始化标签树成功");
        }

        private void reIntiFileMarksCache(BookmarkTreeNode root) {
            List<BookmarkNodeModel> bookmarkNodeModels = PersistenceUtil.treeToList(root);
            FileMarksCache fileMarksCache = bookmarksManager.getFileMarksCache();
            fileMarksCache.clear();
            for (BookmarkNodeModel model : bookmarkNodeModels) {
                OpenFileDescriptor openFileDescriptor = model.getOpenFileDescriptor();
                if (openFileDescriptor == null) {
                    continue;
                }
                fileMarksCache.addBookMark(model);
            }
        }

        @Override
        public void onSuccess() {
            bookmarksManager.getToolWindowRootPanel().reInit(treeModel, project);
            // 获取当前打开的文件
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
            VirtualFile[] openFiles = fileEditorManager.getOpenFiles();
            for (VirtualFile file : openFiles) {
                Set<BookmarkNodeModel> models = bookmarksManager.fileMarksCache.getBookmarks(file.getPath());
                if (CollectionUtils.isEmpty(models)) {
                    return;
                }
                models.forEach(BookmarkNodeModel::createLineMarker);
            }
        }

        public void registerFileGutterIconListener() {
            MessageBusConnection connection = project.getMessageBus().connect();
            connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
                @Override
                public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                    Set<BookmarkNodeModel> models = bookmarksManager.fileMarksCache.getBookmarks(file.getPath());
                    if (CollectionUtils.isEmpty(models)) {
                        return;
                    }
                    models.forEach(BookmarkNodeModel::createLineMarker);
                }

                @Override
                public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                    VirtualFile file = event.getNewFile();
                    if (null == file) {
                        return;
                    }
                    String path = file.getPath();
                    Set<BookmarkNodeModel> models = bookmarksManager.fileMarksCache.getBookmarks(path);
                    if (CollectionUtils.isEmpty(models)) {
                        return;
                    }
                    models.forEach(BookmarkNodeModel::createLineMarker);
                }
            });

            // 监听书签用来控制GutterIcon
            connection.subscribe(BookmarkListener.TOPIC, new BookmarkListener() {
                @Override
                public void bookmarkAdded(@NotNull AbstractTreeNodeModel model) {
                    if (!model.isBookmark()) {
                        return;
                    }
                    BookmarkNodeModel bookmarkNodeModel = (BookmarkNodeModel) model;
                    bookmarkNodeModel.createLineMarker();
                }

                @Override
                public void bookmarkRemoved(@NotNull AbstractTreeNodeModel model) {
                    if (!model.isBookmark()) {
                        return;
                    }
                    BookmarkNodeModel bookmarkNodeModel = (BookmarkNodeModel) model;
                    bookmarkNodeModel.release();
                }
            });
        }
    }
}
