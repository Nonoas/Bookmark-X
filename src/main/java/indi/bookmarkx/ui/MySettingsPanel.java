package indi.bookmarkx.ui;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import indi.bookmarkx.common.I18N;
import indi.bookmarkx.common.I18NEnum;
import indi.bookmarkx.persistence.MySettings;
import org.apache.commons.lang.StringUtils;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;

/**
 * 插件设置面板
 *
 * @author Nonoas
 * @date 2024/10/11 14:49
 */
public class MySettingsPanel extends JBPanel<MySettingsPanel> {

    private final ComboBox<I18NEnum> languageComboBox;
    private final JBCheckBox showTipCheckBox = new JBCheckBox(I18N.get("setting.tipToggle"), true);
    private final JBTextField jtfDelay = new JBTextField();

    public MySettingsPanel() {
        MySettings settings = MySettings.getInstance();
        // 设置面板布局
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.VERTICAL; // 组件水平填充
        gbc.anchor = GridBagConstraints.NORTHWEST; // 组件靠左对齐
        gbc.weightx = 1;

        // 添加国际化语言选择
        languageComboBox = new ComboBox<>(I18NEnum.values());

        JLabel selectLanguageLabel = new JLabel(I18N.get("setting.language"));
        JPanel languagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        languagePanel.add(selectLanguageLabel);
        languagePanel.add(languageComboBox);
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(languagePanel, gbc);

        // 添加是否显示提示的选项
        JPanel showTipPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));


        showTipPanel.add(showTipCheckBox);

        showTipCheckBox.addItemListener(e -> {
            // 当复选框的状态发生变化时，这个方法会被调用
            jtfDelay.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
        });
        if (settings.getTipDelay() >= 0) {
            jtfDelay.setText(String.valueOf(settings.getTipDelay()));
        } else {
            showTipCheckBox.setSelected(false);
        }

        gbc.gridy++;
        add(showTipPanel, gbc);

        gbc.gridx = 1;
        JPanel delayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        delayPanel.add(new JBLabel(I18N.get("setting.tipDelay")));
        delayPanel.add(jtfDelay);
        delayPanel.add(new JBLabel("ms"));
        add(delayPanel, gbc);

        gbc.weighty = 1;
        add(Box.createVerticalStrut(10), gbc);
    }

    public I18NEnum getLanguage() {
        I18NEnum selectedItem = (I18NEnum) languageComboBox.getSelectedItem();
        if (null == selectedItem) {
            selectedItem = I18NEnum.CHINESE;
        }
        return selectedItem;
    }

    public void setLanguage(I18NEnum i18NEnum) {
        languageComboBox.setItem(i18NEnum);
    }

    public int getTipDelay() {
        if (!showTipCheckBox.isSelected()) {
            return -1;
        }
        String delay = jtfDelay.getText();
        if (StringUtils.isBlank(delay)) {
            return 0;
        }
        return Integer.parseInt(delay);
    }

}
