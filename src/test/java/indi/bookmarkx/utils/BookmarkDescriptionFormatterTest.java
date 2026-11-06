package indi.bookmarkx.utils;

import indi.bookmarkx.model.BookmarkDescriptionFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookmarkDescriptionFormatterTest {
    @Test
    void plainTextEscapesMarkupAndPreservesLineBreaks() {
        String formatted = BookmarkDescriptionFormatter.format(
                "<b>A & B</b>\nnext", BookmarkDescriptionFormat.PLAIN_TEXT
        );

        assertTrue(formatted.contains("&lt;b&gt;A &amp; B&lt;/b&gt;"));
        assertTrue(formatted.contains("<br/>next"));
    }

    @Test
    void htmlModeKeepsMarkup() {
        String html = "<b>A & B</b>";

        assertEquals(html, BookmarkDescriptionFormatter.format(html, BookmarkDescriptionFormat.HTML));
    }

    @Test
    void missingFormatDefaultsToHtml() {
        assertEquals("<i>text</i>", BookmarkDescriptionFormatter.format("<i>text</i>", null));
    }
}
