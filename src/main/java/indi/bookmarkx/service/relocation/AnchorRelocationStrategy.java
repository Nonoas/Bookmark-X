package indi.bookmarkx.service.relocation;

import indi.bookmarkx.model.BookmarkNodeModel;

import java.util.List;
import java.util.Optional;

/** Recovers bookmarks when the original snapshot is unavailable or stale. */
public final class AnchorRelocationStrategy implements BookmarkRelocationStrategy {

    @Override
    public String getName() {
        return "anchor";
    }

    @Override
    public Optional<RelocationResult> relocate(RelocationContext context) {
        BookmarkNodeModel bookmark = context.getBookmark();
        if (bookmark == null) {
            return Optional.empty();
        }
        String anchor = TextLineUtil.normalizeLine(bookmark.getAnchorLineText());
        if (anchor.length() < 16) {
            return Optional.empty();
        }
        Optional<RelocationResult> exact = findUniqueLine(context.getCurrentLines(), anchor, false);
        if (exact.isPresent()) {
            return exact;
        }
        // A previously wrapped method signature may now occupy a single line.
        if (anchor.length() >= 32 && (anchor.endsWith(",") || anchor.endsWith("("))
                && anchor.matches("(public|protected|private)\\s+.*\\(.*")) {
            return findUniqueLine(context.getCurrentLines(), anchor, true);
        }
        return Optional.empty();
    }

    private Optional<RelocationResult> findUniqueLine(List<String> lines, String anchor, boolean prefix) {
        int match = -1;
        for (int i = 0; i < lines.size(); i++) {
            String line = TextLineUtil.normalizeLine(lines.get(i));
            if (prefix ? line.startsWith(anchor) : line.equals(anchor)) {
                if (match >= 0) {
                    return Optional.empty();
                }
                match = i;
            }
        }
        return match < 0 ? Optional.empty() : Optional.of(new RelocationResult(match, 0.8, getName()));
    }
}
