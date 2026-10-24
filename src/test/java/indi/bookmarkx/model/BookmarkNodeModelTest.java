package indi.bookmarkx.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;

class BookmarkNodeModelTest {

    @Test
    void skipsLineMarkersWhenFileDescriptorIsMissing() {
        BookmarkNodeModel bookmark = new BookmarkNodeModel();
        bookmark.setLine(3);

        assertNull(bookmark.getCachedDocument());
        assertNull(bookmark.findMyHighlighter());
        assertDoesNotThrow(bookmark::release);
        assertDoesNotThrow(bookmark::createLineMarker);
    }
}
