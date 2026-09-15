package indi.bookmarkx.service.branch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitBranchDetectorTest {

    @TempDir
    Path tempDir;

    @Test
    void detectsBranchFromGitHeadRef() throws Exception {
        Path root = tempDir.resolve("repo");
        Files.createDirectories(root.resolve(".git"));
        Files.writeString(root.resolve(".git/HEAD"), "ref: refs/heads/main\n", StandardCharsets.UTF_8);

        GitBranchContext context = GitBranchDetector.detect(root.toFile()).orElseThrow();

        assertEquals("main", context.getDisplayName());
        assertEquals("main", context.getRoots().get(0).getBranchName());
        assertFalse(context.getRoots().get(0).isDetached());
    }

    @Test
    void detectsDetachedHeadWithStableKey() throws Exception {
        Path root = tempDir.resolve("repo");
        Files.createDirectories(root.resolve(".git"));
        Files.writeString(root.resolve(".git/HEAD"),
                "0123456789abcdef0123456789abcdef01234567\n",
                StandardCharsets.UTF_8);

        GitBranchContext first = GitBranchDetector.detect(root.toFile()).orElseThrow();
        GitBranchContext second = GitBranchDetector.detect(root.toFile()).orElseThrow();

        assertTrue(first.getRoots().get(0).isDetached());
        assertEquals("detached:0123456789ab", first.getDisplayName());
        assertEquals(first.getBranchKey(), second.getBranchKey());
    }

    @Test
    void resolvesGitdirFile() throws Exception {
        Path worktree = tempDir.resolve("worktree");
        Path gitDir = tempDir.resolve("main/.git/worktrees/worktree");
        Files.createDirectories(worktree);
        Files.createDirectories(gitDir);
        Files.writeString(worktree.resolve(".git"),
                "gitdir: ../main/.git/worktrees/worktree\n",
                StandardCharsets.UTF_8);
        Files.writeString(gitDir.resolve("HEAD"), "ref: refs/heads/feature/x\n", StandardCharsets.UTF_8);

        GitBranchContext context = GitBranchDetector.detect(worktree.toFile()).orElseThrow();

        assertEquals("feature/x", context.getDisplayName());
        assertEquals("feature/x", context.getRoots().get(0).getBranchName());
    }
}
