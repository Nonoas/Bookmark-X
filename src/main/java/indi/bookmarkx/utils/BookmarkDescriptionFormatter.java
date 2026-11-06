package indi.bookmarkx.utils;

import indi.bookmarkx.model.BookmarkDescriptionFormat;

public final class BookmarkDescriptionFormatter {
    private BookmarkDescriptionFormatter() {
    }

    public static String format(String value, BookmarkDescriptionFormat format) {
        String text = value == null ? "" : value;
        if (BookmarkDescriptionFormat.orDefault(format) == BookmarkDescriptionFormat.HTML) {
            return text;
        }
        text = escapeHtml(text).replace("\r\n", "\n").replace("\r", "\n");
        text = text.replaceAll("\n{2,}", "</p><p>").replace("\n", "<br/>");
        return "<p>" + text + "</p>";
    }

    public static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
