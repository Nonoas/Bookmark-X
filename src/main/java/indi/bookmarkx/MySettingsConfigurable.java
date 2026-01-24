package indi.bookmarkx;


import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.Messages;
import indi.bookmarkx.common.I18N;
import indi.bookmarkx.persistence.MySettings;
import indi.bookmarkx.ui.pannel.MySettingsPanel;
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
        return isLanguageChanged()
                || settingsComponent.getTipDelay() != settings.getTipDelay()
                || settingsComponent.getDescShowType() != settings.getDescShowType();
    }

    private boolean isLanguageChanged() {
        MySettings settings = MySettings.getInstance();
        return !settingsComponent.getLanguage().equals(settings.getLanguage());
    }

    @Override
    public void apply() {
        boolean languageChanged = isLanguageChanged();
        MySettings settings = MySettings.getInstance();
        settings.setLanguage(settingsComponent.getLanguage());
        settings.setTipDelay(settingsComponent.getTipDelay());
        settings.setDescShowType(settingsComponent.getDescShowType());

        if (languageChanged) {
            showRestartDialog();
        }
    }

    @Override
    public void reset() {
        settingsComponent.reset();
    }

    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }


    public void showRestartDialog() {
        int response = Messages.showYesNoDialog(
                I18N.get("setting.restartMessage"),
                I18N.get("setting.restartTile"),
                Messages.getQuestionIcon()
        );

        if (response == Messages.YES) {
            ApplicationEx app = (ApplicationEx) ApplicationManager.getApplication();
            app.restart(true);
        }
    }

    public enum DescShowType {
        POPUP(0),
        SPLIT_PANE(1);

        private final int value;

        DescShowType(int value) {
            this.value = value;
        }

        public static DescShowType fromCode(int code) {
            for (DescShowType descShowType : values()) {
                if (code == descShowType.value) {
                    return descShowType;
                }
            }
            return POPUP;
        }

        public int getValue() {
            return value;
        }
    }
}
