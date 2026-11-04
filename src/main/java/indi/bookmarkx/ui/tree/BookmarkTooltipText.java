package indi.bookmarkx.ui.tree;

/** Bounded, escaped text for passive hover previews. */
final class BookmarkTooltipText {
    private BookmarkTooltipText() {
    }

    static String format(String name, String description) {
        String title = wrap(abbreviate(name, 48));
        String summary = wrap(abbreviate(description, 120));
        return "<html><div style='width:240px'><b>" + title + "</b>"
                + (summary.isEmpty() ? "" : "<br>" + summary) + "</div></html>";
    }

    private static String abbreviate(String value, int limit) {
        if (value == null) {
            return "";
        }
        String text = value.replaceAll("\\s+", " ").trim();
        return text.codePointCount(0, text.length()) <= limit ? text
                : text.substring(0, text.offsetByCodePoints(0, limit)) + "…";
    }

    private static String wrap(String value) {
        StringBuilder html = new StringBuilder();
        while (value.codePointCount(0, value.length()) > 30) {
            int end = value.offsetByCodePoints(0, 30);
            int space = value.lastIndexOf(' ', end);
            if (space > end / 2) {
                end = space;
            }
            html.append(escape(value.substring(0, end))).append("<br>");
            value = value.substring(end).stripLeading();
        }
        return html.append(escape(value)).toString();
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
