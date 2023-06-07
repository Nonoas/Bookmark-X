package indi.worktool.common;

import com.intellij.AbstractBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ResourceBundle;

/**
 * @author Nonoas
 * @date 2023/6/3
 */
public class I18N extends AbstractBundle {
    private static final String BUNDLE_NAME = "i18n.i18n";
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

    public I18N(@NonNls @NotNull String pathToBundle) {
        super(pathToBundle);
    }

    public static String get(String key) {
        return BUNDLE.getString(key);
    }

}

