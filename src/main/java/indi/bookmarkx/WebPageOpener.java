package indi.bookmarkx;

import java.awt.Desktop;
import java.net.URI;

/**
 * @author Nonoas
 * @date 2023/5/31
 */
public class WebPageOpener {

    /**
     * 打开 url 指定的网页
     *
     * @param url 打开网页
     */
    public static void openWebPage(String url) {
        try {
            URI uri = new URI(url);
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(uri);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

