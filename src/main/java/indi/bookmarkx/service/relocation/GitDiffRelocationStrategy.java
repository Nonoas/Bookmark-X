package indi.bookmarkx.service.relocation;

import com.intellij.openapi.diagnostic.Logger;
import indi.bookmarkx.model.BookmarkNodeModel;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class GitDiffRelocationStrategy implements BookmarkRelocationStrategy {

    private static final Logger LOG = Logger.getInstance(GitDiffRelocationStrategy.class);
    private static final long TIMEOUT_SECONDS = 5L;
    private static final int LOCAL_SEARCH_DISTANCE = 5;

    @Override
    public String getName() {
        return "git";
    }

    @Override
    public Optional<RelocationResult> relocate(RelocationContext context) {
        BookmarkNodeModel bookmark = context.getBookmark();
        if (bookmark == null
                || StringUtils.isBlank(bookmark.getGitRootPath())
                || StringUtils.isBlank(bookmark.getGitRelativePath())
                || StringUtils.isBlank(bookmark.getGitCommit())) {
            return Optional.empty();
        }
        try {
            String currentCommit = runGit(bookmark.getGitRootPath(), "rev-parse", "HEAD").trim();
            if (!bookmark.getGitCommit().equals(currentCommit)) {
                return Optional.empty();
            }
            String diff = runGit(bookmark.getGitRootPath(), "diff", "--unified=0", "--", bookmark.getGitRelativePath());
            if (StringUtils.isBlank(diff)) {
                return Optional.empty();
            }
            int mappedLine = LineMapping.fromUnifiedDiff(diff)
                    .mapLine(bookmark.getLine(), Integer.MAX_VALUE, context.getCurrentLines().size());
            int anchorLine = AnchorScorer.findBestAnchorLineNear(bookmark, context.getCurrentLines(), mappedLine, LOCAL_SEARCH_DISTANCE);
            double confidence = AnchorScorer.score(bookmark, context.getCurrentLines(), anchorLine);
            return Optional.of(new RelocationResult(anchorLine, confidence, getName()));
        } catch (Exception e) {
            LOG.debug("Git relocation is unavailable", e);
            return Optional.empty();
        }
    }

    static Optional<GitInfo> inspectCleanTrackedFile(File projectBaseDir, File file) {
        File gitRoot = findGitRoot(file);
        if (gitRoot == null) {
            gitRoot = findGitRoot(projectBaseDir);
        }
        if (gitRoot == null) {
            return Optional.empty();
        }
        try {
            String relativePath = gitRoot.toPath().relativize(file.toPath()).toString().replace('\\', '/');
            runGit(gitRoot.getAbsolutePath(), "ls-files", "--error-unmatch", "--", relativePath);
            String status = runGit(gitRoot.getAbsolutePath(), "status", "--porcelain", "--", relativePath);
            if (StringUtils.isNotBlank(status)) {
                return Optional.empty();
            }
            String commit = runGit(gitRoot.getAbsolutePath(), "rev-parse", "HEAD").trim();
            return Optional.of(new GitInfo(gitRoot.getAbsolutePath().replace('\\', '/'), relativePath, commit));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static File findGitRoot(File start) {
        File current = start == null || start.isFile() ? start == null ? null : start.getParentFile() : start;
        while (current != null) {
            File git = new File(current, ".git");
            if (git.exists()) {
                return current;
            }
            current = current.getParentFile();
        }
        return null;
    }

    private static String runGit(String gitRoot, String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("-C");
        command.add(gitRoot);
        for (String arg : args) {
            command.add(arg);
        }
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        }
        if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("git command timed out");
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException(output.toString());
        }
        return output.toString();
    }

    static final class GitInfo {
        private final String rootPath;
        private final String relativePath;
        private final String commit;

        private GitInfo(String rootPath, String relativePath, String commit) {
            this.rootPath = rootPath;
            this.relativePath = relativePath;
            this.commit = commit;
        }

        String getRootPath() {
            return rootPath;
        }

        String getRelativePath() {
            return relativePath;
        }

        String getCommit() {
            return commit;
        }
    }
}
