package indi.bookmarkx.ui.panel;

import org.junit.jupiter.api.Test;

import java.awt.Dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookmarkTipPanelTest {
    @Test
    void keepsContentSizeWhenItFits() {
        assertEquals(
                new Dimension(320, 140),
                BookmarkTipPanel.boundedSize(new Dimension(320, 140), new Dimension(520, 360))
        );
    }

    @Test
    void capsPopupWidthAndHeight() {
        assertEquals(
                new Dimension(520, 360),
                BookmarkTipPanel.boundedSize(new Dimension(900, 700), new Dimension(520, 360))
        );
    }
}
