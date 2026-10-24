package indi.bookmarkx.model;

import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import indi.bookmarkx.ui.MyGutterIconRenderer;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class BookmarkLineMarkersTest {

    @Test
    void releasesEveryMarkerForModelRegardlessOfSavedLine() {
        BookmarkNodeModel bookmark = new BookmarkNodeModel();
        bookmark.setLine(10_000);
        MarkerStub first = new MarkerStub(new MyGutterIconRenderer(bookmark), true);
        MarkerStub second = new MarkerStub(new MyGutterIconRenderer(bookmark), true);
        MarkerStub unrelated = new MarkerStub(new OtherRenderer(), true);

        BookmarkLineMarkers.disposeForModel(new RangeHighlighter[]{first.highlighter, second.highlighter, unrelated.highlighter}, bookmark);

        assertEquals(1, first.disposeCount);
        assertEquals(1, second.disposeCount);
        assertEquals(0, unrelated.disposeCount);
    }

    @Test
    void findsValidMarkerAfterCachedMarkerWasDisposed() {
        BookmarkNodeModel bookmark = new BookmarkNodeModel();
        MarkerStub disposed = new MarkerStub(new MyGutterIconRenderer(bookmark), false);
        MarkerStub active = new MarkerStub(new MyGutterIconRenderer(bookmark), true);

        assertSame(active.highlighter,
                BookmarkLineMarkers.findForModel(new RangeHighlighter[]{disposed.highlighter, active.highlighter}, bookmark));
    }

    @Test
    void removesOrphanedBookmarkMarkersWithoutTouchingOtherGutterMarkers() {
        MarkerStub oldBookmark = new MarkerStub(new MyGutterIconRenderer(new BookmarkNodeModel()), true);
        MarkerStub currentBookmark = new MarkerStub(new MyGutterIconRenderer(new BookmarkNodeModel()), true);
        MarkerStub unrelated = new MarkerStub(new OtherRenderer(), true);

        BookmarkLineMarkers.disposeAll(new RangeHighlighter[]{oldBookmark.highlighter, currentBookmark.highlighter, unrelated.highlighter});

        assertEquals(1, oldBookmark.disposeCount);
        assertEquals(1, currentBookmark.disposeCount);
        assertEquals(0, unrelated.disposeCount);
    }

    private static final class MarkerStub {
        private final RangeHighlighter highlighter;
        private boolean valid;
        private int disposeCount;

        private MarkerStub(GutterIconRenderer renderer, boolean valid) {
            this.valid = valid;
            this.highlighter = (RangeHighlighter) Proxy.newProxyInstance(
                    RangeHighlighter.class.getClassLoader(),
                    new Class<?>[]{RangeHighlighter.class},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "isValid":
                                return this.valid;
                            case "getGutterIconRenderer":
                                return renderer;
                            case "dispose":
                                this.valid = false;
                                this.disposeCount++;
                                return null;
                            default:
                                throw new UnsupportedOperationException(method.getName());
                        }
                    }
            );
        }
    }

    private static final class OtherRenderer extends GutterIconRenderer {
        @Override
        public Icon getIcon() {
            return new ImageIcon();
        }

        @Override
        public boolean equals(Object other) {
            return this == other;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
    }
}
