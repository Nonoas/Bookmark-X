package indi.bookmarkx.model;

public enum BookmarkDescriptionFormat {
    PLAIN_TEXT,
    HTML;

    public static BookmarkDescriptionFormat orDefault(BookmarkDescriptionFormat format) {
        return format == null ? HTML : format;
    }
}
