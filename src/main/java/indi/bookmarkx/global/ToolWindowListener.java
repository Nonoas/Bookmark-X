package indi.bookmarkx.global;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class ToolWindowListener implements ToolWindowManagerListener {

    private final Project project;

    public ToolWindowListener(Project project) {
        this.project = project;
    }

    @Override
    public void stateChanged(@NotNull ToolWindowManager toolWindowManager) {
        Set<String> toolWindowIdSet = toolWindowManager.getToolWindowIdSet();
        toolWindowIdSet.forEach(System.out::println);
    }
}