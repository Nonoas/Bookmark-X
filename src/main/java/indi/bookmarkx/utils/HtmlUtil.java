package indi.bookmarkx.utils;

/**
 * TODO 类描述
 *
 * @author Nonoas
 * @date 2024/3/24 10:58
 */
public class HtmlUtil {
    private HtmlUtil() {

    }

    public static String wrapText(String tag, String text) {
        return String.format("<%s>%s</%s>", tag, text, tag);
    }
}
