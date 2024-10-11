package indi.bookmarkx.ui;

/**
 * TODO 类描述
 *
 * @author huangshengsheng
 * @date 2024/10/11 14:49
 */

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBPanel;
import com.intellij.openapi.ui.ComboBox;

import javax.swing.*;
import java.awt.*;

public class MySettingsPanel extends JBPanel<MySettingsPanel> {
    private final ComboBox<String> languageComboBox;

    public MySettingsPanel() {
        // 设置面板布局
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.VERTICAL; // 组件水平填充
        gbc.anchor = GridBagConstraints.NORTHWEST; // 组件靠左对齐
        gbc.weightx = 1;

        // 添加国际化语言选择
        languageComboBox = new ComboBox<>(new String[]{"中文", "English"});
        JLabel selectLanguageLabel = new JLabel("插件语言:");
        JPanel languagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        languagePanel.add(selectLanguageLabel);
        languagePanel.add(languageComboBox);
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(languagePanel, gbc);

        // 添加是否显示提示的选项
        JPanel showTipPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JBCheckBox showTipCheckBox = new JBCheckBox("标签悬浮框", true);
        showTipPanel.add(showTipCheckBox);
        gbc.gridy++;
        add(showTipPanel, gbc);

        gbc.weighty = 1;
        add(Box.createVerticalStrut(10), gbc);
    }

    public String getLanguage() {
        return languageComboBox.getSelectedItem().toString();
    }
}
