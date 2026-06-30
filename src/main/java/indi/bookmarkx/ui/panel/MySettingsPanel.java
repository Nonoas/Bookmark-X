package indi.bookmarkx.ui.panel;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.*;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import indi.bookmarkx.MySettingsConfigurable;
import indi.bookmarkx.common.I18N;
import indi.bookmarkx.common.I18NEnum;
import indi.bookmarkx.mcp.BookmarkMcpConfig;
import indi.bookmarkx.persistence.MySettings;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

/**
 * 插件设置面板
 *
 * @author Nonoas
 * @date 2024/10/11 14:49
 */
public class MySettingsPanel extends JBPanel<MySettingsPanel> {

    private final ComboBox<I18NEnum> languageComboBox = new ComboBox<>(I18NEnum.values());

    private final ComboBox<String> showTypeComboBox = new ComboBox<>(new String[]{
            I18N.get("setting.desc.showType.popup"),
            I18N.get("setting.desc.showType.splitPane")
    });

    private final JPanel tipSettingsWrapper = new JPanel(new BorderLayout());
    private final JPanel mcpSettingsWrapper = new JPanel(new BorderLayout());

    private final JBCheckBox showTipCheckBox = new JBCheckBox(I18N.get("setting.tipToggle"), true);
    private final JBTextField jtfDelay = new JBTextField();
    private final JBCheckBox enableMcpCheckBox = new JBCheckBox(I18N.get("setting.mcp.enable"), true);
    private final JBTextField jtfMcpPort = new JBTextField();
    private final JBPasswordField jpfMcpPassword = new JBPasswordField();
    private final JBLabel mcpUrlValueLabel = new JBLabel();

    public MySettingsPanel() {
        initComponentState();
        setLayout(new BorderLayout());

        showTypeComboBox.addActionListener(e -> updateVisibility());
        enableMcpCheckBox.addItemListener(e -> updateMcpControls());
        jtfMcpPort.getDocument().addDocumentListener(new SimpleDocumentListener(this::refreshMcpUrlLabel));

        JPanel contentPanel = FormBuilder.createFormBuilder()
                .addComponent(new TitledSeparator(I18N.get("setting.group.general")))
                .addLabeledComponent(new JBLabel(I18N.get("setting.language")), languageComboBox)
                .addVerticalGap(10)
                .addComponent(new TitledSeparator(I18N.get("setting.group.desc")))
                .addLabeledComponent(new JBLabel(I18N.get("setting.desc.showType")), showTypeComboBox)
                .addComponent(tipSettingsWrapper)
                .addVerticalGap(10)
                .addComponent(new TitledSeparator(I18N.get("setting.group.mcp")))
                .addComponent(enableMcpCheckBox)
                .addComponent(mcpSettingsWrapper)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        tipSettingsWrapper.add(createTwoColumnRow(), BorderLayout.CENTER);
        mcpSettingsWrapper.add(createMcpSettingsPanel(), BorderLayout.CENTER);
        // 初始状态触发一次
        updateVisibility();
        updateMcpControls();
        refreshMcpUrlLabel();

        contentPanel.setBorder(JBUI.Borders.empty(10, 20));
        add(contentPanel, BorderLayout.CENTER);
    }

    /**
     * 核心方法：控制显示与隐藏
     */
    private void updateVisibility() {
        // 假设 0 是气泡框 (Popup)
        boolean isPopup = showTypeComboBox.getSelectedIndex() == 0;

        // 设置容器可见性
        tipSettingsWrapper.setVisible(isPopup);
        if (isPopup) {

        } else {
            jtfDelay.setText("0");
            showTipCheckBox.setSelected(false);
        }

        // 关键：通知 Swing 重新布局并重绘，否则界面可能会留白或不刷新
        revalidate();
        repaint();
    }

    /**
     * 创建两列并排的行
     */
    private JPanel createTwoColumnRow() {

        JPanel rowPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        rowPanel.add(showTipCheckBox);
        JPanel delayWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        delayWrapper.add(new JBLabel(I18N.get("setting.tipDelay")));
        jtfDelay.setColumns(4);
        delayWrapper.add(jtfDelay);
        delayWrapper.add(new JBLabel("ms"));

        rowPanel.add(delayWrapper);
        return rowPanel;
    }

    private JPanel createMcpSettingsPanel() {
        JPanel panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel(I18N.get("setting.mcp.port")), jtfMcpPort)
                .addLabeledComponent(new JBLabel(I18N.get("setting.mcp.password")), jpfMcpPassword)
                .addLabeledComponent(new JBLabel(I18N.get("setting.mcp.url")), mcpUrlValueLabel)
                .addComponent(new JBLabel(I18N.get("setting.mcp.password.tip")))
                .getPanel();

        jtfMcpPort.setColumns(6);
        jpfMcpPassword.setColumns(20);
        return panel;
    }

    private void initComponentState() {
        MySettings settings = MySettings.getInstance();

        // 逻辑联动
        showTipCheckBox.addItemListener(e ->
                jtfDelay.setEnabled(e.getStateChange() == ItemEvent.SELECTED)
        );

        if (settings.getTipDelay() >= 0) {
            jtfDelay.setText(String.valueOf(settings.getTipDelay()));
            showTipCheckBox.setSelected(true);
        } else {
            showTipCheckBox.setSelected(false);
            jtfDelay.setEnabled(false);
        }

        showTypeComboBox.setSelectedIndex(settings.getDescShowType().getValue());
        enableMcpCheckBox.setSelected(settings.isMcpEnabled());
        jtfMcpPort.setText(String.valueOf(settings.getMcpPort()));
        jpfMcpPassword.setText(settings.getMcpPassword());
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

    public MySettingsConfigurable.DescShowType getDescShowType() {
        int selectedIndex = showTypeComboBox.getSelectedIndex();
        MySettingsConfigurable.DescShowType descShowType = MySettingsConfigurable.DescShowType.fromCode(selectedIndex);
        return descShowType;
    }

    public boolean isMcpEnabled() {
        return enableMcpCheckBox.isSelected();
    }

    public String getMcpPortText() {
        return StringUtils.defaultString(jtfMcpPort.getText()).trim();
    }

    public int getMcpPort() {
        String text = getMcpPortText();
        if (StringUtils.isBlank(text)) {
            return BookmarkMcpConfig.DEFAULT_PORT;
        }
        try {
            int port = Integer.parseInt(text);
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException(I18N.get("setting.mcp.port.invalid"));
            }
            return port;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(I18N.get("setting.mcp.port.invalid"));
        }
    }

    public String getMcpPassword() {
        return new String(jpfMcpPassword.getPassword()).trim();
    }

    public void reset() {
        MySettings settings = MySettings.getInstance();
        setLanguage(settings.getLanguage());

        int tipDelay = settings.getTipDelay();
        if (tipDelay >= 0) {
            jtfDelay.setText(String.valueOf(tipDelay));
            showTipCheckBox.setSelected(true);
        } else {
            jtfDelay.setText("0");
            showTipCheckBox.setSelected(false);
        }
        showTypeComboBox.setSelectedIndex(settings.getDescShowType().getValue());
        enableMcpCheckBox.setSelected(settings.isMcpEnabled());
        jtfMcpPort.setText(String.valueOf(settings.getMcpPort()));
        jpfMcpPassword.setText(settings.getMcpPassword());
        updateMcpControls();
        refreshMcpUrlLabel();
    }

    private void updateMcpControls() {
        boolean enabled = enableMcpCheckBox.isSelected();
        jtfMcpPort.setEnabled(enabled);
        jpfMcpPassword.setEnabled(enabled);
        refreshMcpUrlLabel();
        revalidate();
        repaint();
    }

    private void refreshMcpUrlLabel() {
        if (!enableMcpCheckBox.isSelected()) {
            mcpUrlValueLabel.setText(I18N.get("setting.mcp.disabled"));
            return;
        }
        String portText = getMcpPortText();
        if (StringUtils.isBlank(portText)) {
            portText = String.valueOf(BookmarkMcpConfig.DEFAULT_PORT);
        }
        try {
            int port = Integer.parseInt(portText);
            if (port < 1 || port > 65535) {
                mcpUrlValueLabel.setText(I18N.get("setting.mcp.port.invalid"));
                return;
            }
            mcpUrlValueLabel.setText(BookmarkMcpConfig.endpointUrl(port));
        } catch (NumberFormatException ex) {
            mcpUrlValueLabel.setText(I18N.get("setting.mcp.port.invalid"));
        }
    }

    private interface DocumentChangeAction {
        void run();
    }

    private static final class SimpleDocumentListener implements javax.swing.event.DocumentListener {
        private final DocumentChangeAction action;

        private SimpleDocumentListener(@NotNull DocumentChangeAction action) {
            this.action = action;
        }

        @Override
        public void insertUpdate(javax.swing.event.DocumentEvent e) {
            action.run();
        }

        @Override
        public void removeUpdate(javax.swing.event.DocumentEvent e) {
            action.run();
        }

        @Override
        public void changedUpdate(javax.swing.event.DocumentEvent e) {
            action.run();
        }
    }
}
