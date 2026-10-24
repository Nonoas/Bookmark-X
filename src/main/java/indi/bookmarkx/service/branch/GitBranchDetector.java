package indi.bookmarkx.service.branch;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class GitBranchDetector {

    private static final String GIT_VCS_NAME = "Git";
    private static final String GITDIR_PREFIX = "gitdir:";
    private static final String HEAD_REF_PREFIX = "ref:";
    private static final String HEAD_BRANCH_PREFIX = "refs/heads/";
    private static final long GIT_TIMEOUT_SECONDS = 5L;

    private GitBranchDetector() {
    }

    public static Optional<GitBranchContext> detect(Project project) {
        if (project == null || project.isDisposed()) {
            return Optional.empty();
        }
        List<File> roots = detectConfiguredGitRoots(project);
        if (roots.isEmpty() && !StringUtil.isEmptyOrSpaces(project.getBasePath())) {
            File gitRoot = findGitRoot(new File(project.getBasePath()));
            if (gitRoot != null) {
                roots.add(gitRoot);
            }
        }
        return detectRoots(roots);
    }

    static Optional<GitBranchContext> detect(File projectBaseDir) {
        File gitRoot = findGitRoot(projectBaseDir);
        if (gitRoot == null) {
            return Optional.empty();
        }
        return detectRoots(List.of(gitRoot));
    }

    static Optional<GitBranchContext> detectRoots(List<File> candidateRoots) {
        if (candidateRoots == null || candidateRoots.isEmpty()) {
            return Optional.empty();
        }
        Map<String, GitBranchContext.RootBranch> branches = new LinkedHashMap<>();
        for (File candidateRoot : candidateRoots) {
            readRootBranch(candidateRoot).ifPresent(branch -> branches.put(normalizePath(branch.getRoot()), branch));
        }
        if (branches.isEmpty()) {
            return Optional.empty();
        }
        List<GitBranchContext.RootBranch> sortedRoots = branches.values().stream()
                .sorted(Comparator.comparing(branch -> normalizePath(branch.getRoot())))
                .collect(Collectors.toList());
        String rawKey = sortedRoots.stream()
                .map(branch -> normalizePath(branch.getRoot()) + "@" + branch.getKeyIdentity())
                .collect(Collectors.joining("\n"));
        return Optional.of(new GitBranchContext(sortedRoots, sha256(rawKey), buildDisplayName(sortedRoots)));
    }

    static Optional<List<String>> listLocalBranches(File root) {
        if (root == null) {
            return Optional.empty();
        }
        try {
            String output = runGit(normalizeFile(root), "for-each-ref", "--format=%(refname:short)", "refs/heads");
            List<String> branches = output.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .sorted()
                    .collect(Collectors.toList());
            return Optional.of(branches);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static List<File> detectConfiguredGitRoots(Project project) {
        List<File> roots = new ArrayList<>();
        try {
            VcsRoot[] vcsRoots = ProjectLevelVcsManager.getInstance(project).getAllVcsRoots();
            for (VcsRoot vcsRoot : vcsRoots) {
                AbstractVcs vcs = vcsRoot.getVcs();
                VirtualFile path = vcsRoot.getPath();
                if (vcs == null || path == null || !GIT_VCS_NAME.equalsIgnoreCase(vcs.getName())) {
                    continue;
                }
                roots.add(new File(path.getPath()));
            }
        } catch (Exception ignored) {
            // Fall back to filesystem detection below.
        }
        return roots;
    }

    private static Optional<GitBranchContext.RootBranch> readRootBranch(File root) {
        if (root == null) {
            return Optional.empty();
        }
        File normalizedRoot = normalizeFile(root);
        File dotGit = new File(normalizedRoot, ".git");
        File gitDir = resolveGitDir(dotGit);
        if (gitDir == null) {
            return Optional.empty();
        }
        File headFile = new File(gitDir, "HEAD");
        if (!headFile.isFile()) {
            return Optional.empty();
        }
        try {
            String head = firstLine(headFile);
            if (StringUtil.isEmptyOrSpaces(head)) {
                return Optional.empty();
            }
            if (head.startsWith(HEAD_REF_PREFIX)) {
                String ref = head.substring(HEAD_REF_PREFIX.length()).trim();
                String branchName = ref.startsWith(HEAD_BRANCH_PREFIX)
                        ? ref.substring(HEAD_BRANCH_PREFIX.length())
                        : ref;
                return Optional.of(new GitBranchContext.RootBranch(
                        normalizedRoot,
                        normalizeFile(gitDir),
                        branchName,
                        ref,
                        false
                ));
            }
            String identity = head.trim();
            return Optional.of(new GitBranchContext.RootBranch(
                    normalizedRoot,
                    normalizeFile(gitDir),
                    null,
                    identity,
                    true
            ));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static File resolveGitDir(File dotGit) {
        if (dotGit == null) {
            return null;
        }
        if (dotGit.isDirectory()) {
            return dotGit;
        }
        if (!dotGit.isFile()) {
            return null;
        }
        try {
            String line = firstLine(dotGit);
            if (!line.startsWith(GITDIR_PREFIX)) {
                return null;
            }
            String gitDirPath = line.substring(GITDIR_PREFIX.length()).trim();
            if (StringUtil.isEmptyOrSpaces(gitDirPath)) {
                return null;
            }
            File gitDir = new File(gitDirPath);
            if (!gitDir.isAbsolute()) {
                gitDir = new File(dotGit.getParentFile(), gitDirPath);
            }
            return gitDir.isDirectory() ? normalizeFile(gitDir) : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static File findGitRoot(File start) {
        File current = start == null || start.isFile() ? start == null ? null : start.getParentFile() : start;
        while (current != null) {
            if (new File(current, ".git").exists()) {
                return normalizeFile(current);
            }
            current = current.getParentFile();
        }
        return null;
    }

    private static String firstLine(File file) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        return lines.isEmpty() ? "" : lines.get(0).trim();
    }

    private static String runGit(File root, String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("-C");
        command.add(root.getPath());
        Collections.addAll(command, args);
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
        if (!process.waitFor(GIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("git command timed out");
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException(output.toString());
        }
        return output.toString();
    }

    private static String buildDisplayName(List<GitBranchContext.RootBranch> roots) {
        if (roots.size() == 1) {
            return roots.get(0).getDisplayName();
        }
        return roots.stream()
                .map(branch -> rootName(branch.getRoot()) + ":" + branch.getDisplayName())
                .collect(Collectors.joining(", "));
    }

    private static String rootName(File root) {
        String name = root.getName();
        return StringUtil.isEmptyOrSpaces(name) ? normalizePath(root) : name;
    }

    private static File normalizeFile(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            return file.getAbsoluteFile();
        }
    }

    private static String normalizePath(File file) {
        return normalizeFile(file).getPath().replace('\\', '/');
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
