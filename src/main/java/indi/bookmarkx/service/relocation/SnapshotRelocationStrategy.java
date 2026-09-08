package indi.bookmarkx.service.relocation;

import indi.bookmarkx.model.BookmarkNodeModel;

import java.util.List;
import java.util.Optional;

public final class SnapshotRelocationStrategy implements BookmarkRelocationStrategy {

    private static final int LOCAL_SEARCH_DISTANCE = 5;

    @Override
    public String getName() {
        return "snapshot";
    }

    @Override
    public Optional<RelocationResult> relocate(RelocationContext context) {
        List<String> baseLines = context.getBaseLines();
        List<String> currentLines = context.getCurrentLines();
        BookmarkNodeModel bookmark = context.getBookmark();
        if (baseLines.isEmpty() || currentLines.isEmpty() || bookmark == null) {
            return Optional.empty();
        }
        int mappedLine = LineMapping.fromLines(baseLines, currentLines)
                .mapLine(bookmark.getLine(), baseLines.size(), currentLines.size());
        int anchorLine = AnchorScorer.findBestAnchorLineNear(bookmark, currentLines, mappedLine, LOCAL_SEARCH_DISTANCE);
        double confidence = AnchorScorer.score(bookmark, currentLines, anchorLine);
        return Optional.of(new RelocationResult(anchorLine, confidence, getName()));
    }
}
