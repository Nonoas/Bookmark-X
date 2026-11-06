package indi.bookmarkx.persistence;

import indi.bookmarkx.model.BookmarkDescriptionFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MySettingsTest {
    @Test
    void remembersLastDescriptionFormat() {
        MySettings settings = new MySettings();

        assertEquals(BookmarkDescriptionFormat.HTML, settings.getLastDescriptionFormat());

        settings.setLastDescriptionFormat(BookmarkDescriptionFormat.PLAIN_TEXT);

        assertEquals(BookmarkDescriptionFormat.PLAIN_TEXT, settings.getLastDescriptionFormat());
    }

    @Test
    void invalidPersistedFormatFallsBackToHtml() {
        MySettings settings = new MySettings();
        MySettings.State state = new MySettings.State();
        state.lastDescriptionFormat = "UNKNOWN";
        settings.loadState(state);

        assertEquals(BookmarkDescriptionFormat.HTML, settings.getLastDescriptionFormat());
    }
}
