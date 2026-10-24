package indi.bookmarkx.service.relocation;

import indi.bookmarkx.model.BookmarkNodeModel;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotRelocationStrategyTest {

    @Test
    void relocatesBookmarkBySnapshotDiffAndAnchor() {
        BookmarkNodeModel bookmark = bookmarkAt(1, "target();");
        RelocationContext context = new RelocationContext(
                null,
                null,
                bookmark,
                TextLineUtil.splitLines("before\ntarget();\nafter"),
                TextLineUtil.splitLines("before\ninserted\ntarget();\nafter")
        );

        Optional<RelocationResult> result = new SnapshotRelocationStrategy().relocate(context);

        assertTrue(result.isPresent());
        assertEquals(2, result.get().getLine());
        assertTrue(result.get().getConfidence() >= 0.6);
    }

    @Test
    void prefersContextWhenAnchorLineIsDuplicated() {
        BookmarkNodeModel bookmark = bookmarkAt(2, "target();");
        bookmark.setBeforeContextHash(TextLineUtil.hashContext(TextLineUtil.splitLines("alpha\ncontext\ntarget();\nomega"), 0, 2));
        bookmark.setAfterContextHash(TextLineUtil.hashContext(TextLineUtil.splitLines("alpha\ncontext\ntarget();\nomega"), 3, 4));
        RelocationContext context = new RelocationContext(
                null,
                null,
                bookmark,
                TextLineUtil.splitLines("alpha\ncontext\ntarget();\nomega"),
                TextLineUtil.splitLines("target();\nnoise\nalpha\ncontext\ntarget();\nomega")
        );

        Optional<RelocationResult> result = new SnapshotRelocationStrategy().relocate(context);

        assertTrue(result.isPresent());
        assertEquals(4, result.get().getLine());
    }

    @Test
    void doesNotJumpToDistantDuplicateWhenMappedLineWasEdited() {
        BookmarkNodeModel bookmark = bookmarkAt(14, "        Arrays.sort(s);");
        String baseText = linesWithRepeatedSort("        Arrays.sort(s);");
        String currentText = linesWithRepeatedSort("        Arrays.srt(s);");
        RelocationContext context = new RelocationContext(
                null,
                null,
                bookmark,
                TextLineUtil.splitLines(baseText),
                TextLineUtil.splitLines(currentText)
        );

        Optional<RelocationResult> result = new SnapshotRelocationStrategy().relocate(context);

        assertTrue(result.isPresent());
        assertEquals(14, result.get().getLine());
        assertTrue(result.get().getConfidence() < 0.6);
    }

    @Test
    void doesNotJumpToDistantDuplicateWhenBookmarkLineWasDeleted() {
        BookmarkNodeModel bookmark = bookmarkAt(14, "        Arrays.sort(s);");
        String baseText = linesWithRepeatedSort("        Arrays.sort(s);");
        String currentText = linesWithDeletedLine15();
        RelocationContext context = new RelocationContext(
                null,
                null,
                bookmark,
                TextLineUtil.splitLines(baseText),
                TextLineUtil.splitLines(currentText)
        );

        Optional<RelocationResult> result = new SnapshotRelocationStrategy().relocate(context);

        assertTrue(result.isPresent());
        assertEquals(14, result.get().getLine());
        assertTrue(result.get().getConfidence() < 0.6);
    }

    private BookmarkNodeModel bookmarkAt(int line, String lineText) {
        BookmarkNodeModel bookmark = new BookmarkNodeModel();
        bookmark.setLine(line);
        bookmark.setAnchorLineText(lineText);
        bookmark.setAnchorLineHash(TextLineUtil.hashLine(lineText));
        return bookmark;
    }

    private String linesWithRepeatedSort(String line15) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 35; i++) {
            if (i == 14) {
                builder.append(line15);
            } else if (i == 29) {
                builder.append("        Arrays.sort(s);");
            } else {
                builder.append("line ").append(i + 1);
            }
            if (i < 34) {
                builder.append('\n');
            }
        }
        return builder.toString();
    }

    private String linesWithDeletedLine15() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 35; i++) {
            if (i == 14) {
                continue;
            }
            if (i == 29) {
                builder.append("        Arrays.sort(s);");
            } else {
                builder.append("line ").append(i + 1);
            }
            if (i < 34) {
                builder.append('\n');
            }
        }
        return builder.toString();
    }
}
