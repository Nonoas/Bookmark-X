package indi.worktool;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import indi.worktool.model.po.BookmarkPO;
import org.jetbrains.annotations.NotNull;

/**
 * @author Nonoas
 * @date 2023/6/5
 */
@State(
        name = "SuperBookmarkState",
        storages = {@Storage("SuperBookmarkState.xml")}
)
public class MyPersistent implements PersistentStateComponent<BookmarkPO> {

    private static final Logger LOG = Logger.getInstance(MyPersistent.class);

    private BookmarkPO state;

    public static MyPersistent getInstance(Project project) {
        return project.getService(MyPersistent.class);
    }

    public void setState(BookmarkPO state) {
        this.state = state;
    }

    @Override
    public @NotNull BookmarkPO getState() {
        LOG.debug("获取：" + state);

        if (state == null) {
            state = new BookmarkPO();
            state.setName("ROOT");
            state.setBookmark(false);
        }
        return state;
    }

    @Override
    public void loadState(@NotNull BookmarkPO state) {
        LOG.debug("加载：" + state);
        this.state = state;
    }
}
