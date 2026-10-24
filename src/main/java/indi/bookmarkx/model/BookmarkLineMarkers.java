package indi.bookmarkx.model;

import com.intellij.openapi.editor.markup.RangeHighlighter;
import indi.bookmarkx.ui.MyGutterIconRenderer;

/**
 * Identifies and removes gutter markers owned by Bookmark-X.
 */
public final class BookmarkLineMarkers {

    private BookmarkLineMarkers() {
    }

    public static RangeHighlighter findForModel(RangeHighlighter[] highlighters, BookmarkNodeModel model) {
        for (RangeHighlighter highlighter : highlighters) {
            if (belongsTo(highlighter, model)) {
                return highlighter;
            }
        }
        return null;
    }

    public static void disposeForModel(RangeHighlighter[] highlighters, BookmarkNodeModel model) {
        for (RangeHighlighter highlighter : highlighters) {
            if (belongsTo(highlighter, model)) {
                highlighter.dispose();
            }
        }
    }

    public static void disposeAll(RangeHighlighter[] highlighters) {
        for (RangeHighlighter highlighter : highlighters) {
            if (isBookmarkMarker(highlighter)) {
                highlighter.dispose();
            }
        }
    }

    static boolean belongsTo(RangeHighlighter highlighter, BookmarkNodeModel model) {
        return isBookmarkMarker(highlighter)
                && ((MyGutterIconRenderer) highlighter.getGutterIconRenderer()).getModel() == model;
    }

    private static boolean isBookmarkMarker(RangeHighlighter highlighter) {
        return highlighter != null
                && highlighter.isValid()
                && highlighter.getGutterIconRenderer() instanceof MyGutterIconRenderer;
    }
}
