//package indi.bookmarkx.command;
//
//import com.intellij.openapi.fileEditor.OpenFileDescriptor;
//import com.intellij.openapi.project.Project;
//import com.intellij.openapi.vfs.VirtualFile;
//import indi.bookmarkx.model.BookmarkNodeModel;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.UUID;
//
///**
// * Command to create a new bookmark.
// * Encapsulates the business logic for bookmark creation,
// * separating it from UI concerns.
// */
//public class BookmarkCreator {
//
//    private final Project project;
//    private final VirtualFile file;
//    private final String name;
//    private final String desc;
//    private final int line;
//
//    /**
//     * Creates a new command instance.
//     *
//     * @param project the current project
//     * @param file the target file
//     * @param name bookmark name
//     * @param desc bookmark description
//     * @param line target line number (0-based)
//     */
//    public BookmarkCreator(@NotNull Project project,
//                                 @NotNull VirtualFile file,
//                                 @NotNull String name,
//                                 @NotNull String desc,
//                                 int line) {
//        this.project = project;
//        this.file = file;
//        this.name = name;
//        this.desc = desc;
//        this.line = line;
//    }
//
//    /**
//     * Executes the command to create a bookmark.
//     * This method performs the following steps:
//     * 1. Creates the bookmark model
//     * 2. Persists the bookmark to storage
//     * 3. Updates the in-memory cache/index
//     * 4. Publishes events to notify UI components
//     *
//     * @return the created bookmark model
//     */
//    @NotNull
//    public BookmarkNodeModel execute() {
//        // Step 1: Create bookmark model
//        BookmarkNodeModel bookmark = createBookmarkModel();
//
//        // Step 2: Save to repository (persistent storage)
//        repository.save(bookmark);
//
//        // Step 3: Publish event for UI updates
//        eventPublisher.publishBookmarkAdded(bookmark);
//
//        return bookmark;
//    }
//
//    /**
//     * Creates and configures the bookmark model.
//     *
//     * @return configured bookmark model
//     */
//    @NotNull
//    private BookmarkNodeModel createBookmarkModel() {
//        BookmarkNodeModel bookmark = new BookmarkNodeModel();
//        bookmark.setUuid(UUID.randomUUID().toString());
//        bookmark.setName(name);
//        bookmark.setDesc(desc);
//        bookmark.setLine(line);
//        bookmark.setIcon(file.getFileType().getIcon());
//        bookmark.setOpenFileDescriptor(
//                new OpenFileDescriptor(project, file, line, 0)
//        );
//        return bookmark;
//    }
//}