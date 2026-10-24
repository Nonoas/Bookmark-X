package indi.bookmarkx.service.branch;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@Service(Service.Level.PROJECT)
@State(
        name = "BookmarkX.branchState",
        storages = {@Storage("BookmarkX.branchState.xml")}
)
public final class BookmarkBranchState implements PersistentStateComponent<BookmarkBranchState.StateData> {

    private StateData state = new StateData();

    public static BookmarkBranchState getInstance(Project project) {
        return project.getService(BookmarkBranchState.class);
    }

    @Override
    public @NotNull StateData getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull StateData state) {
        this.state = state;
    }

    public String getActiveBranchKey() {
        return StringUtils.defaultString(state.activeBranchKey);
    }

    public void setActiveBranch(GitBranchContext context) {
        if (context == null) {
            state.activeBranchKey = "";
            state.activeBranchDisplayName = "";
            return;
        }
        state.activeBranchKey = StringUtils.defaultString(context.getBranchKey());
        state.activeBranchDisplayName = StringUtils.defaultString(context.getDisplayName());
    }

    @XmlRootElement
    public static class StateData {
        public String activeBranchKey = "";
        public String activeBranchDisplayName = "";
    }
}
