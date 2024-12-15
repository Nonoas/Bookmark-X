package indi.bookmarkx.global;

import indi.bookmarkx.model.BookmarkNodeModel;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Nonoas
 * @date 2024/12/15
 * @since 2.1.1
 */
public class FileMarksCache {

    /**
     * 标识文件哪些行存在书签
     */
    private final Map<String, Set<BookmarkNodeModel>> fileMarksCache = new ConcurrentHashMap<>();

    public Map<String, Set<BookmarkNodeModel>> getCache() {
        return fileMarksCache;
    }

    public void clear() {
        fileMarksCache.clear();
    }

    public void addBookMark(BookmarkNodeModel model) {
        Optional<String> filePath = model.getFilePath();
        if (filePath.isEmpty()) {
            return;
        }
        String path = filePath.get();
        Set<BookmarkNodeModel> set = fileMarksCache.getOrDefault(path, new HashSet<>());
        set.add(model);
        fileMarksCache.put(path, set);
    }

    public void deleteBookMark(BookmarkNodeModel model) {
        Optional<String> filePath = model.getFilePath();
        if (filePath.isEmpty()) {
            return;
        }
        String path = filePath.get();
        Set<BookmarkNodeModel> set = fileMarksCache.getOrDefault(path, new HashSet<>());
        if (set.isEmpty()) {
            return;
        }
        set.remove(model);
        fileMarksCache.put(path, set);
    }

    public Optional<BookmarkNodeModel> findModel(String path, Integer line) {
        return fileMarksCache.getOrDefault(path, new HashSet<>()).stream()
                .filter(e -> Objects.equals(line, e.getLine()))
                .findAny();
    }
}
