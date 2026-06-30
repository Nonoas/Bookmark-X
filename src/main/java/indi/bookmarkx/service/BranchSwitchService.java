package indi.bookmarkx.service;

import com.intellij.openapi.project.Project;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单的 BranchSwitchService 实现，用于在构建时提供缺失的类以通过编译。
 * 注意：此实现只是一个占位/默认实现；若项目需要真实的分支切换检测逻辑，请替换为实际实现并完善注册为 IDE 服务（plugin.xml 或 @Service）。
 */
public class BranchSwitchService {

    private final Project project;
    private volatile boolean branchSwitchInProgress = false;

    private static final ConcurrentHashMap<Project, BranchSwitchService> INSTANCES = new ConcurrentHashMap<>();

    private BranchSwitchService(Project project) {
        this.project = project;
    }

    /**
     * 返回与 Project 关联的单例实例（仅在内存中缓存）。
     */
    public static BranchSwitchService getInstance(Project project) {
        if (project == null) return null;
        return INSTANCES.computeIfAbsent(project, BranchSwitchService::new);
    }

    /**
     * 用于判断是否有分支切换正在进行。
     * 当前占位实现始终返回字段值（默认 false）。
     */
    public boolean isBranchSwitchInProgress() {
        return branchSwitchInProgress;
    }

    /**
     * 允许在其它代码中设置分支切换状态（如果需要）。
     */
    public void setBranchSwitchInProgress(boolean branchSwitchInProgress) {
        this.branchSwitchInProgress = branchSwitchInProgress;
    }
}
