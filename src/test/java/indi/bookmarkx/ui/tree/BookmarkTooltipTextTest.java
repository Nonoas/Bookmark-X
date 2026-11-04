package indi.bookmarkx.ui.tree;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BookmarkTooltipTextTest {
    @Test
    void escapesMarkupRatherThanRenderingImagesOrLargeHtml() {
        String text = BookmarkTooltipText.format("<img src='x'>", "<h1>large & bold</h1>");
        assertFalse(text.contains("<img"));
        assertFalse(text.contains("<h1>"));
        assertTrue(text.contains("&lt;h1&gt;large &amp; bold&lt;/h1&gt;"));
    }

    @Test
    void boundsLongDescriptionsAndNamesWithoutBreakingEmoji() {
        String text = BookmarkTooltipText.format("a".repeat(100), "😀".repeat(200)).replace("<br>", "");
        assertTrue(text.contains("a".repeat(48) + "…"));
        assertTrue(text.contains("😀".repeat(120) + "…"));
        assertFalse(text.contains("😀".repeat(121)));
    }

    @Test
    void normalizesWhitespaceAndOmitsEmptyDescriptions() {
        assertTrue(BookmarkTooltipText.format("name", " first\n\t second ").contains("first second"));
        assertFalse(BookmarkTooltipText.format("name", null).contains("<br>"));
        assertFalse(BookmarkTooltipText.format(null, "  ").contains("<br>"));
    }
}
