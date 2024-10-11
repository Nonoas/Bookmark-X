package indi.bookmarkx;


import com.intellij.openapi.options.Configurable;
import indi.bookmarkx.common.I18NEnum;
import indi.bookmarkx.persistence.MySettings;
import indi.bookmarkx.ui.MySettingsPanel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

import static indi.bookmarkx.common.Constants.PLUGIN_NAME;

/**
 * 配置类
 *
 * @author Nonoas
 * @date 2024/10/11 14:48
 */
public class MySettingsConfigurable implements Configurable {

    private MySettingsPanel settingsComponent;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return PLUGIN_NAME;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return settingsComponent = new MySettingsPanel();
    }

    @Override
    public boolean isModified() {
        MySettings settings = MySettings.getInstance();
        return !settingsComponent.getLanguage().name().equals(settings.getState().language);
    }

    @Override
    public void apply() {
        MySettings settings = MySettings.getInstance();
        I18NEnum language = settingsComponent.getLanguage();
        settings.getState().language = language.name();
    }

    @Override
    public void reset() {
        MySettings settings = MySettings.getInstance();
    }

    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }
}
