package indi.bookmarkx;

/**
 * TODO 类描述
 *
 * @author huangshengsheng
 * @date 2024/10/11 14:48
 */
import com.intellij.openapi.options.Configurable;
import indi.bookmarkx.persistence.MySettingsPersistence;
import indi.bookmarkx.ui.MySettingsPanel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static indi.bookmarkx.common.Constants.PLUGIN_NAME;

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
        MySettingsPersistence settings = MySettingsPersistence.getInstance();
        return !settingsComponent.getLanguage().equals(settings.someValue);
    }

    @Override
    public void apply() {
        MySettingsPersistence settings = MySettingsPersistence.getInstance();
        settings.someValue = settingsComponent.getLanguage();
    }

    @Override
    public void reset() {
        MySettingsPersistence settings = MySettingsPersistence.getInstance();
        //settingsComponent.setTextFieldText(settings.someValue);
    }

    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }
}
