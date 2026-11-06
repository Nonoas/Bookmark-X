package indi.bookmarkx.ui.tree;

import org.junit.jupiter.api.Test;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookmarkPopupPositionTest {
    @Test
    void placesPopupImmediatelyToTheRightOfTheMouse() {
        Point location = BookmarkTree.TreeMouseMotionAdapter.choosePopupLocation(
                new Point(180, 120),
                new Dimension(200, 100),
                new Rectangle(0, 0, 1200, 800),
                8
        );

        assertEquals(new Point(188, 112), location);
    }

    @Test
    void fallsBackToTheLeftAndStaysOnScreen() {
        Point location = BookmarkTree.TreeMouseMotionAdapter.choosePopupLocation(
                new Point(1150, 760),
                new Dimension(200, 100),
                new Rectangle(0, 0, 1200, 800),
                8
        );

        assertEquals(new Point(942, 700), location);
    }
}
