package indi.bookmarkx.common;

import com.intellij.AbstractBundle;
import indi.bookmarkx.persistence.MySettings;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * 国际化文本获取
 *
 * @author Nonoas
 * @date 2023/6/3
 */
public class I18N extends AbstractBundle {
    private static final String BUNDLE_NAME = "i18n.i18n";
    private static ResourceBundle BUNDLE;

    public I18N(@NonNls @NotNull String pathToBundle) {
        super(pathToBundle);
    }

    private static void init() {
        MySettings settings = MySettings.getInstance();
        String language = settings.getState().language;
        if (StringUtils.isBlank(language)) {
            if (isSupportLocale()) {
                BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);
                return;
            }
            BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ENGLISH);
            return;
        }
        I18NEnum i18NEnum = I18NEnum.valueOf(language);
        BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, i18NEnum.getLocale());
    }

    public static @NotNull String get(String key, Object... params) {
        if (Objects.isNull(BUNDLE)) {
            init();
        }
        return message(BUNDLE, key, params);
    }

    /**
     * 检查指定的资源包是否存在于给定的 Locale 中。
     *
     * @return 如果资源包存在，则返回 true；否则返回 false
     */
    public static boolean isSupportLocale() {
        try {
            ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault());
            return resourceBundle != null;
        } catch (MissingResourceException e) {
            return false;
        }
    }

    public static void switchLanguage(I18NEnum language) {
        BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, language.getLocale());
    }
}

