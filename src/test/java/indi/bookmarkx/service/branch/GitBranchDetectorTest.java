package indi.bookmarkx.service.branch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
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

    @Test
    void listsLocalBranchesFromRealGitRepository() throws Exception {
        assumeGitAvailable();
        Path root = createRealGitRepo();
        runGit(root, "branch", "feature");

        Optional<List<String>> branches = GitBranchDetector.listLocalBranches(root.toFile());

        assertTrue(branches.isPresent());
        assertTrue(branches.get().contains("main"));
        assertTrue(branches.get().contains("feature"));

        runGit(root, "branch", "-D", "feature");
        branches = GitBranchDetector.listLocalBranches(root.toFile());

        assertTrue(branches.isPresent());
        assertTrue(branches.get().contains("main"));
        assertFalse(branches.get().contains("feature"));
    }

    private Path createRealGitRepo() throws Exception {
        Path root = tempDir.resolve("real-repo");
        Files.createDirectories(root);
        runGit(root, "init");
        runGit(root, "config", "user.email", "bookmark-x@example.invalid");
        runGit(root, "config", "user.name", "Bookmark X Test");
        runGit(root, "checkout", "-b", "main");
        Files.writeString(root.resolve("README.md"), "test\n", StandardCharsets.UTF_8);
        runGit(root, "add", "README.md");
        runGit(root, "commit", "-m", "init");
        return root;
    }

    private void assumeGitAvailable() {
        try {
            runGit(tempDir, "--version");
        } catch (Exception e) {
            assumeTrue(false, "git executable is not available");
        }
    }

    private String runGit(Path directory, String... args) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(buildGitCommand(args))
                .directory(directory.toFile())
                .redirectErrorStream(true);
        Process process = builder.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        }
        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("git command timed out");
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException(output.toString());
        }
        return output.toString();
    }

    private List<String> buildGitCommand(String... args) {
        List<String> command = new java.util.ArrayList<>();
        command.add("git");
        command.addAll(List.of(args));
        return command;
    }
}
