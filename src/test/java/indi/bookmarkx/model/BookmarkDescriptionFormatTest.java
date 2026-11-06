package indi.bookmarkx.model;

import com.google.gson.Gson;
import indi.bookmarkx.model.po.BookmarkPO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookmarkDescriptionFormatTest {
    @Test
    void legacyBookmarksDefaultToHtml() {
        BookmarkPO bookmark = new Gson().fromJson(
                "{\"bookmark\":true,\"name\":\"legacy\"}", BookmarkPO.class
        );

        assertEquals(BookmarkDescriptionFormat.HTML, bookmark.getDescriptionFormat());
    }

    @Test
    void htmlFormatSurvivesPersistenceRoundTrip() {
        BookmarkPO bookmark = new BookmarkPO();
        bookmark.setBookmark(true);
        bookmark.setDescriptionFormat(BookmarkDescriptionFormat.HTML);

        BookmarkPO restored = new Gson().fromJson(new Gson().toJson(bookmark), BookmarkPO.class);

        assertEquals(BookmarkDescriptionFormat.HTML, restored.getDescriptionFormat());
    }
}
