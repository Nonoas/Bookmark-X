package indi.bookmarkx.service.relocation;

import indi.bookmarkx.model.BookmarkNodeModel;
import indi.bookmarkx.utils.CollectionUtil;

import java.util.List;

final class AnchorScorer {

    private static final int CONTEXT_RADIUS = 3;

    private AnchorScorer() {
    }

    static double score(BookmarkNodeModel bookmark, List<String> lines, int line) {
        if (bookmark == null || lines == null || line < 0 || line >= lines.size()) {
            return 0.0;
        }
        double score = 0.0;
        String anchorHash = bookmark.getAnchorLineHash();
        if (anchorHash != null && anchorHash.equals(TextLineUtil.hashLine(lines.get(line)))) {
            score += 0.6;
        }
        String anchorText = bookmark.getAnchorLineText();
        if (anchorText != null && TextLineUtil.normalizeLine(anchorText).equals(TextLineUtil.normalizeLine(lines.get(line)))) {
            score += 0.2;
        }
        String beforeHash = bookmark.getBeforeContextHash();
        if (beforeHash != null && beforeHash.equals(TextLineUtil.hashContext(lines, line - CONTEXT_RADIUS, line))) {
            score += 0.1;
        }
        String afterHash = bookmark.getAfterContextHash();
        if (afterHash != null && afterHash.equals(TextLineUtil.hashContext(lines, line + 1, line + 1 + CONTEXT_RADIUS))) {
            score += 0.1;
        }
        return score;
    }

    static int findBestAnchorLine(BookmarkNodeModel bookmark, List<String> lines, int preferredLine) {
        if (bookmark == null || CollectionUtil.isEmpty(lines) || bookmark.getAnchorLineHash() == null) {
            return preferredLine;
        }
        int bestLine = preferredLine;
        double bestScore = score(bookmark, lines, preferredLine);
        for (int i = 0; i < lines.size(); i++) {
            double score = score(bookmark, lines, i);
            if (score > bestScore || (score == bestScore && Math.abs(i - preferredLine) < Math.abs(bestLine - preferredLine))) {
                bestScore = score;
                bestLine = i;
            }
        }
        return bestLine;
    }

    static int findBestAnchorLineNear(BookmarkNodeModel bookmark, List<String> lines, int preferredLine, int maxDistance) {
        if (bookmark == null || CollectionUtil.isEmpty(lines) || bookmark.getAnchorLineHash() == null) {
            return preferredLine;
        }
        int safePreferredLine = Math.max(0, Math.min(preferredLine, lines.size() - 1));
        int bestLine = safePreferredLine;
        double bestScore = score(bookmark, lines, safePreferredLine);
        int from = Math.max(0, safePreferredLine - Math.max(0, maxDistance));
        int to = Math.min(lines.size() - 1, safePreferredLine + Math.max(0, maxDistance));
        for (int i = from; i <= to; i++) {
            double score = score(bookmark, lines, i);
            if (score > bestScore || (score == bestScore && Math.abs(i - safePreferredLine) < Math.abs(bestLine - safePreferredLine))) {
                bestScore = score;
                bestLine = i;
            }
        }
        return bestLine;
    }
}
