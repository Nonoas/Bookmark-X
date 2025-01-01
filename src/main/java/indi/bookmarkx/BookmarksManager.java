package indi.bookmarkx;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
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
import indi.bookmarkx.ui.tree.BookmarkTree;
import indi.bookmarkx.ui.tree.BookmarkTreeNode;
import indi.bookmarkx.utils.PersistenceUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultTreeModel;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 项目级别的管理器（命令模式）：用于管理所有「书签UI」的变化，是一切用户操作的入口，管理所有数据及 UI 的引用
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
                .showAndCallback((name, desc) -> {
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
        new BookmarkCreatorDialog(project, I18N.get("bookmark.create.title"))
                .defaultName(nodeModel.getName())
                .defaultDesc(nodeModel.getDesc())
                .showAndCallback((name, desc) -> {
                    nodeModel.setName(name);
                    nodeModel.setDesc(desc);
                    getBookmarkPublisher(project).bookmarkChanged(nodeModel);
                });
    }

    @NotNull
    private static BookmarkListener getBookmarkPublisher(Project project) {
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
        BookmarkListener listener = getBookmarkPublisher(this.project);
        listener.bookmarkAdded(bookmarkModel);
    }

    public static void add(BookmarkPopup p) {

    }

    public void prev() {
        toolWindowRootPanel.prev();
    }

    public void next() {
        toolWindowRootPanel.next();
    }

    public void select(BookmarkPopup popup) {
        popup.select(true);
        popup.navigate();
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
                List<BookmarkNodeModel> bookmarkNodeModels = PersistenceUtil.treeToList(root);
                reIntiFileMarksCache(bookmarkNodeModels);
            } catch (Exception e) {
                // 错误处理
                LOG.error("初始化标签树失败", e);
            }
            LOG.info("初始化标签树成功");
        }

        private void reIntiFileMarksCache(List<BookmarkNodeModel> bookmarkNodeModels) {
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
        }
    }
}
