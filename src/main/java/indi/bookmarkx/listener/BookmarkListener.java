package indi.bookmarkx.listener;

import com.intellij.util.messages.Topic;
import indi.bookmarkx.model.AbstractTreeNodeModel;
import org.jetbrains.annotations.NotNull;

/**
 * 书签变化监听器
 *
 * @author huangshengsheng
 * @date 2024/12/27 18:01
 */
public interface BookmarkListener {
    Topic<BookmarkListener> TOPIC = Topic.create("Bookmarks", BookmarkListener.class);

    default void bookmarkAdded(@NotNull AbstractTreeNodeModel model) {
    }

    default void bookmarkRemoved(@NotNull AbstractTreeNodeModel model) {
    }

    default void bookmarkChanged(@NotNull AbstractTreeNodeModel model) {
    }

    default void bookmarksOrderChanged() {
    }
}
