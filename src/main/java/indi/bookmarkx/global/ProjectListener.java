package indi.bookmarkx.global;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.jetbrains.annotations.NotNull;

public class ProjectListener implements ProjectManagerListener {

    @Override
    public void projectClosing(@NotNull Project project) {
        // 处理项目关闭事件
//        System.out.println("Project closing: " + project.getName());
    }


}
