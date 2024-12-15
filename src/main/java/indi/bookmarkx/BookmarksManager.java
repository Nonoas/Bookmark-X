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
import indi.bookmarkx.model.po.BookmarkPO;
import indi.bookmarkx.persistence.MyPersistent;
import indi.bookmarkx.ui.dialog.BookmarkCreatorDialog;
import indi.bookmarkx.model.BookmarkConverter;
import indi.bookmarkx.model.BookmarkNodeModel;
import indi.bookmarkx.ui.painter.LineEndPainter;
import indi.bookmarkx.ui.tree.BookmarkTreeNode;
import indi.bookmarkx.ui.pannel.BookmarksManagePanel;
import indi.bookmarkx.utils.PersistenceUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultTreeModel;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 项目级别的管理器：用于管理所有「书签UI」的变化
 */
@Service(Service.Level.PROJECT)
public final class BookmarksManager {

    private static final Logger LOG = Logger.getInstance(BookmarksManagePanel.class);

    public Project project;

    private BookmarksManagePanel toolWindowRootPanel;

    private final BookmarkArrayListTable bookmarkArrayListTable;

    /**
     * 标识文件哪些行存在书签
     */
    private final Map<String, Set<BookmarkNodeModel>> fileMarksCache = new ConcurrentHashMap<>();

    public BookmarksManager(Project project) {
        this.project = project;
        bookmarkArrayListTable = BookmarkArrayListTable.getInstance(project);
        initData();
    }

    private void initData() {
        ProgressManager.getInstance().run(new TreeLoadTask(project, this));
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
        int column = caretModel.getLogicalPosition().column;

        BookmarkNodeModel bookmarkNodeModel = LineEndPainter.findLine(BookmarkArrayListTable.getInstance(project).getOnlyIndex(file.getPath()), line);
        String defaultName = file.getName();
        String defaultDesc;
        boolean add = true;
        if (bookmarkNodeModel == null) {
            // 获取选中文本
            String selectedText = caretModel.getCurrentCaret().getSelectedText();
            defaultDesc = selectedText == null ? "" : (" " + selectedText + " ");
            String uuid = UUID.randomUUID().toString();
            bookmarkNodeModel = new BookmarkNodeModel();
            bookmarkNodeModel.setUuid(uuid);
            bookmarkNodeModel.setLine(line);
            bookmarkNodeModel.setColumn(column);
            bookmarkNodeModel.setIcon(file.getFileType().getIcon());
            bookmarkNodeModel.setIcon(file.getFileType().getIcon());
            bookmarkNodeModel.setOpenFileDescriptor(new OpenFileDescriptor(project, file, line, column));
        }else {
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
                        submitCreateBookRemark(finalBookmarkNodeModel, editor);
                    } else {
                        if (Objects.nonNull(toolWindowRootPanel)) {
                            toolWindowRootPanel.treeNodesChanged(finalBookmarkNodeModel);
                        }
                    }

                });
    }

    private void submitCreateBookRemark(BookmarkNodeModel bookmarkModel, Editor editor) {
        //  The toolWindowRootPanel may be null the first time IDEA is opened
        if (Objects.isNull(toolWindowRootPanel)) {
            MyPersistent persistent = MyPersistent.getInstance(project);
            persistent.getState().getChildren().add(BookmarkConverter.convertToPO(bookmarkModel));
        } else {
            afterCreateSubmit(bookmarkModel, editor);
        }

    }

    private void afterCreateSubmit(BookmarkNodeModel bookmarkModel, Editor editor) {
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

    public void setToolWindowRootPanel(@NotNull BookmarksManagePanel panel) {
        this.toolWindowRootPanel = panel;
    }

    /**
     * 重新加载标签树
     */
    public void reload() {
        toolWindowRootPanel.reInit(project);
    }

    public Map<String, Set<BookmarkNodeModel>> getFileMarksCache() {
        return fileMarksCache;
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
                intiFileMarksLinesCache(bookmarkNodeModels);
            } catch (Exception e) {
                // 错误处理
                LOG.error("初始化标签树失败", e);
            }
            LOG.info("初始化标签树成功");
        }

        private void intiFileMarksLinesCache(List<BookmarkNodeModel> bookmarkNodeModels) {
            for (BookmarkNodeModel model : bookmarkNodeModels) {
                OpenFileDescriptor openFileDescriptor = model.getOpenFileDescriptor();
                if (openFileDescriptor == null) {
                    continue;
                }
                String path = openFileDescriptor.getFile().getPath();
                Map<String, Set<BookmarkNodeModel>> linesCache = bookmarksManager.getFileMarksCache();
                Set<BookmarkNodeModel> models = linesCache.getOrDefault(path, new HashSet<>());
                models.add(model);
                linesCache.put(path, models);
            }
        }

        @Override
        public void onSuccess() {

        }
    }
}
