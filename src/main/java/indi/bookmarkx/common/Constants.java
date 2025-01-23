package indi.bookmarkx.common;

import com.intellij.openapi.editor.colors.TextAttributesKey;

/**
 * 全局常量类
 *
 * @author Nonoas
 * @date 2024/10/11 16:17
 */
public interface Constants {
    /**
     * plugin.xml 文件中的 key 名
     */
    String PLUGIN_NAME = "Bookmark-X";

    String PLUGIN_ID = "indi.nonoas.bookmarkx";

    /**
     * 插件在IDEA插件官网的id
     */
    String PLUGIN_MARKET_ID = "22013";

    TextAttributesKey TK_BOOKMARK_X = TextAttributesKey.createTextAttributesKey("BOOKMARK_X");
}
