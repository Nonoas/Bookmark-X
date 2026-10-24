package indi.bookmarkx.service.relocation;

import indi.bookmarkx.utils.CollectionUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class LineMapping {

    private static final int MAX_ISOLATED_MATCH_DELTA_CHANGE = 5;

    private final List<int[]> matchedPairs;
    private final List<DiffHunk> diffHunks;

    private LineMapping(List<int[]> matchedPairs) {
        this.matchedPairs = matchedPairs;
        this.diffHunks = Collections.emptyList();
    }

    private LineMapping(List<int[]> matchedPairs, List<DiffHunk> diffHunks) {
        this.matchedPairs = matchedPairs;
        this.diffHunks = diffHunks;
    }

    static LineMapping fromLines(List<String> oldLines, List<String> newLines) {
        if (CollectionUtil.isEmpty(oldLines) || CollectionUtil.isEmpty(newLines)) {
            return new LineMapping(Collections.emptyList());
        }
        if ((long) oldLines.size() * (long) newLines.size() > 4_000_000L) {
            return new LineMapping(Collections.emptyList());
        }
        int oldSize = oldLines.size();
        int newSize = newLines.size();
        int[][] lcs = new int[oldSize + 1][newSize + 1];
        for (int i = oldSize - 1; i >= 0; i--) {
            for (int j = newSize - 1; j >= 0; j--) {
                if (TextLineUtil.normalizeLine(oldLines.get(i)).equals(TextLineUtil.normalizeLine(newLines.get(j)))) {
                    lcs[i][j] = lcs[i + 1][j + 1] + 1;
                } else {
                    lcs[i][j] = Math.max(lcs[i + 1][j], lcs[i][j + 1]);
                }
            }
        }
        List<int[]> pairs = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < oldSize && j < newSize) {
            if (TextLineUtil.normalizeLine(oldLines.get(i)).equals(TextLineUtil.normalizeLine(newLines.get(j)))) {
                pairs.add(new int[]{i, j});
                i++;
                j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                i++;
            } else {
                j++;
            }
        }
        return new LineMapping(pairs);
    }

    static LineMapping fromUnifiedDiff(String diffText) {
        List<int[]> pairs = new ArrayList<>();
        List<DiffHunk> hunks = new ArrayList<>();
        int oldLine = 0;
        int newLine = 0;
        for (String rawLine : TextLineUtil.splitLines(diffText)) {
            if (rawLine.startsWith("@@")) {
                HunkRange range = HunkRange.parse(rawLine);
                if (range == null) {
                    continue;
                }
                int oldStart = range.oldCount == 0 ? range.oldStart : range.oldStart - 1;
                int newStart = range.newCount == 0 ? range.newStart : range.newStart - 1;
                hunks.add(new DiffHunk(oldStart, range.oldCount, newStart, range.newCount));
                oldLine = range.oldStart - 1;
                newLine = range.newStart - 1;
                continue;
            }
            if (rawLine.startsWith("+") && !rawLine.startsWith("+++")) {
                newLine++;
                continue;
            }
            if (rawLine.startsWith("-") && !rawLine.startsWith("---")) {
                oldLine++;
                continue;
            }
            if (rawLine.startsWith(" ") || rawLine.isEmpty()) {
                pairs.add(new int[]{oldLine, newLine});
                oldLine++;
                newLine++;
            }
        }
        return new LineMapping(pairs, hunks);
    }

    int mapLine(int oldLine, int oldLineCount, int newLineCount) {
        if (oldLine < 0) {
            return 0;
        }
        if (!diffHunks.isEmpty()) {
            return mapLineByHunks(oldLine, newLineCount);
        }
        if (matchedPairs.isEmpty()) {
            return Math.min(Math.max(oldLine, 0), Math.max(newLineCount - 1, 0));
        }

        int[] previous = null;
        int[] next = null;
        for (int index = 0; index < matchedPairs.size(); index++) {
            int[] pair = matchedPairs.get(index);
            if (pair[0] == oldLine) {
                if (isIsolatedFarMatch(pair, previous, nextPair(index))) {
                    break;
                }
                return clamp(pair[1], newLineCount);
            }
            if (pair[0] < oldLine) {
                previous = pair;
            } else {
                next = pair;
                break;
            }
        }

        if (previous != null && next != null) {
            int oldGap = Math.max(1, next[0] - previous[0]);
            int newGap = Math.max(1, next[1] - previous[1]);
            int offset = oldLine - previous[0];
            return clamp(previous[1] + Math.round((float) offset * newGap / oldGap), newLineCount);
        }
        if (previous != null) {
            return clamp(previous[1] + oldLine - previous[0], newLineCount);
        }
        int deltaAtStart = next[1] - next[0];
        return clamp(oldLine + deltaAtStart, newLineCount);
    }

    private int[] nextPair(int currentIndex) {
        int nextIndex = currentIndex + 1;
        if (nextIndex >= matchedPairs.size()) {
            return null;
        }
        return matchedPairs.get(nextIndex);
    }

    private boolean isIsolatedFarMatch(int[] pair, int[] previous, int[] next) {
        int delta = pair[1] - pair[0];
        boolean differsFromPrevious = previous != null
                && Math.abs(delta - (previous[1] - previous[0])) > MAX_ISOLATED_MATCH_DELTA_CHANGE;
        boolean differsFromNext = next != null
                && Math.abs(delta - (next[1] - next[0])) > MAX_ISOLATED_MATCH_DELTA_CHANGE;
        if (previous != null && next != null) {
            return differsFromPrevious && differsFromNext;
        }
        if (previous != null) {
            return differsFromPrevious;
        }
        return next != null && differsFromNext;
    }

    private int mapLineByHunks(int oldLine, int newLineCount) {
        int delta = 0;
        for (DiffHunk hunk : diffHunks) {
            int oldStart = hunk.oldStart;
            int oldEnd = hunk.oldStart + hunk.oldCount;
            if (oldLine < oldStart) {
                return clamp(oldLine + delta, newLineCount);
            }
            if (oldLine >= oldStart && oldLine < oldEnd) {
                if (hunk.newCount <= 0) {
                    return clamp(hunk.newStart, newLineCount);
                }
                int offset = Math.min(oldLine - oldStart, hunk.newCount - 1);
                return clamp(hunk.newStart + offset, newLineCount);
            }
            delta += hunk.newCount - hunk.oldCount;
        }
        return clamp(oldLine + delta, newLineCount);
    }

    private int clamp(int line, int lineCount) {
        if (lineCount <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(line, lineCount - 1));
    }

    private static final class HunkRange {
        private final int oldStart;
        private final int oldCount;
        private final int newStart;
        private final int newCount;

        private HunkRange(int oldStart, int oldCount, int newStart, int newCount) {
            this.oldStart = oldStart;
            this.oldCount = oldCount;
            this.newStart = newStart;
            this.newCount = newCount;
        }

        private static HunkRange parse(String header) {
            String[] parts = header.split(" ");
            if (parts.length < 3) {
                return null;
            }
            int[] oldRange = parseRange(parts[1]);
            int[] newRange = parseRange(parts[2]);
            return new HunkRange(oldRange[0], oldRange[1], newRange[0], newRange[1]);
        }

        private static int[] parseRange(String token) {
            String value = token.substring(1);
            int count = 1;
            int comma = value.indexOf(',');
            if (comma >= 0) {
                count = Integer.parseInt(value.substring(comma + 1));
                value = value.substring(0, comma);
            }
            return new int[]{Math.max(1, Integer.parseInt(value)), Math.max(0, count)};
        }
    }

    private static final class DiffHunk {
        private final int oldStart;
        private final int oldCount;
        private final int newStart;
        private final int newCount;

        private DiffHunk(int oldStart, int oldCount, int newStart, int newCount) {
            this.oldStart = oldStart;
            this.oldCount = oldCount;
            this.newStart = newStart;
            this.newCount = newCount;
        }
    }
}
