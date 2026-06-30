package indi.bookmarkx;


import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.Messages;
import indi.bookmarkx.common.I18N;
import indi.bookmarkx.listener.SettingsListener;
import indi.bookmarkx.persistence.MySettings;
import indi.bookmarkx.ui.panel.MySettingsPanel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

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
                || settingsComponent.getDescShowType() != settings.getDescShowType()
                || settingsComponent.isMcpEnabled() != settings.isMcpEnabled()
                || !settingsComponent.getMcpPortText().equals(String.valueOf(settings.getMcpPort()))
                || !settingsComponent.getMcpPassword().equals(settings.getMcpPassword());
    }

    @Override
    public void apply() throws ConfigurationException {
        boolean languageChanged = isLanguageChanged();

        MySettings settings = MySettings.getInstance();
        int mcpPort;
        try {
            mcpPort = settingsComponent.getMcpPort();
        } catch (IllegalArgumentException ex) {
            throw new ConfigurationException(ex.getMessage());
        }

        settings.setLanguage(settingsComponent.getLanguage());
        settings.setTipDelay(settingsComponent.getTipDelay());
        settings.setDescShowType(settingsComponent.getDescShowType());
        settings.setMcpEnabled(settingsComponent.isMcpEnabled());
        settings.setMcpPort(mcpPort);
        settings.setMcpPassword(settingsComponent.getMcpPassword());

        ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(SettingsListener.TOPIC)
                .settingsUpdated();

        if (languageChanged) {
            showRestartDialog();
        }
    }

    private boolean isLanguageChanged() {
        MySettings settings = MySettings.getInstance();
        return !settingsComponent.getLanguage().equals(settings.getLanguage());
    }

    private boolean idDescShowTypeChanged() {
        MySettings settings = MySettings.getInstance();
        return !(settingsComponent.getDescShowType() == settings.getDescShowType());
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
