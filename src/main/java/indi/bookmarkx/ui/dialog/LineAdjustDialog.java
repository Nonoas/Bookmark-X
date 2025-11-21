package indi.bookmarkx.ui.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import indi.bookmarkx.common.I18N;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;

public class LineAdjustDialog extends DialogWrapper {
    private JTextField lineAdjustField;

    public LineAdjustDialog(@Nullable Project project) {
        super(project);
        init();
        setTitle(I18N.get("bookmark.adjustLineDialogTitle"));
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 15));

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        JButton minusBtn = new JButton("-");
        minusBtn.addActionListener((e) -> adjustValue(-1));

        lineAdjustField = new JTextField(10);
        lineAdjustField.setToolTipText(I18N.get("bookmark.adjustLineTip"));
        lineAdjustField.setText("0");
        lineAdjustField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String text = ((JTextField) input).getText().trim();
                return text.matches("^-?\\d+$"); // 仅允许整数
            }
        });

        JButton plusBtn = new JButton("+");
        plusBtn.addActionListener((e) -> adjustValue(1));

        inputPanel.add(new JLabel(I18N.get("bookmark.adjustLineLabel")));
        inputPanel.add(minusBtn);
        inputPanel.add(lineAdjustField);
        inputPanel.add(plusBtn);

        JPanel tipPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel tipLabel = new JLabel(I18N.get("bookmark.adjustLineTip"));
        tipLabel.setForeground(Color.GRAY);
        tipPanel.add(tipLabel);

        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(tipPanel, BorderLayout.CENTER);

        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        return mainPanel;
    }

    /**
     * 调整输入框数值（加/减1）
     */
    private void adjustValue(int delta) {
        try {
            int currentValue = Integer.parseInt(lineAdjustField.getText().trim());
            lineAdjustField.setText(String.valueOf(currentValue + delta));
        } catch (NumberFormatException e) {
            lineAdjustField.setText(String.valueOf(0 + delta));
        }
    }

    public int getAdjustValue() {
        try {
            return Integer.parseInt(lineAdjustField.getText().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 输入验证
     */
    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        String text = lineAdjustField.getText().trim();
        if (!text.matches("^-?\\d+$")) {
            return new ValidationInfo(I18N.get("bookmark.adjustLineTip"), lineAdjustField);
        }
        return super.doValidate();
    }
}