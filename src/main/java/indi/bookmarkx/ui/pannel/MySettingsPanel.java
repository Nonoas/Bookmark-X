package indi.bookmarkx.ui.pannel;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import indi.bookmarkx.MySettingsConfigurable;
import indi.bookmarkx.common.I18N;
import indi.bookmarkx.common.I18NEnum;
import indi.bookmarkx.persistence.MySettings;
import org.apache.commons.lang.StringUtils;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
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

    private final JBCheckBox showTipCheckBox = new JBCheckBox(I18N.get("setting.tipToggle"), true);
    private final JBTextField jtfDelay = new JBTextField();

    public MySettingsPanel() {
        initComponentState();
        setLayout(new BorderLayout());

        showTypeComboBox.addActionListener(e -> updateVisibility());

        JPanel contentPanel = FormBuilder.createFormBuilder()
                .addComponent(new TitledSeparator(I18N.get("setting.group.general")))
                .addLabeledComponent(new JBLabel(I18N.get("setting.language")), languageComboBox)
                .addVerticalGap(10)
                .addComponent(new TitledSeparator(I18N.get("setting.group.desc")))
                .addLabeledComponent(new JBLabel(I18N.get("setting.desc.showType")), showTypeComboBox)
                .addComponent(tipSettingsWrapper)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        tipSettingsWrapper.add(createTwoColumnRow(), BorderLayout.CENTER);
        // 初始状态触发一次
        updateVisibility();

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

        showTypeComboBox.setSelectedIndex(settings.getDescShowType());
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

    public int getDescShowType() {
        int selectedIndex = showTypeComboBox.getSelectedIndex();
        MySettingsConfigurable.DescShowType descShowType = MySettingsConfigurable.DescShowType.fromCode(selectedIndex);
        return descShowType.getValue();
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
        showTypeComboBox.setSelectedIndex(settings.getDescShowType());
    }
}
