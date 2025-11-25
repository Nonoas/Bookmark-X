package indi.bookmarkx.ui.dialog;

import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.JBUI;
import indi.bookmarkx.common.I18N;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author Nonoas
 * @date 2023/6/1
 */
public class BookmarkCreatorDialog extends DialogWrapper {

    private final EditorTextField tfName = new EditorTextField();
    private final EditorTextField tfDesc = new EditorTextField();
    private final EditorTextField tfLineNumber = new EditorTextField(); // 新增行号输入框
    private boolean isEditMode = false; // 标记是否为编辑模式
    private int maxLineNumber = 0;
    private OnOKAction oKAction;

    private Project project;

    public BookmarkCreatorDialog(Project project, String title) {
        super(true);
        initSelf(project, title);
    }

    /**
     * 构造函数，<B>编辑时 请使用此构造函数</B>
     * @param project
     * @param title
     * @param lineNumber 新行号
     * @param maxLineNumber 文件的最大行数
     */
    public BookmarkCreatorDialog(Project project, String title, int lineNumber, int maxLineNumber) {
        super(true);
        this.isEditMode = true;
        this.tfLineNumber.setText(String.valueOf(lineNumber));
        this.maxLineNumber = maxLineNumber;
        initSelf(project, title);
    }

    private void initSelf(Project project, String title) {
        setTitle(title);
        this.project = project;
        init();
    }

    public BookmarkCreatorDialog defaultName(String name) {
        this.tfName.setText(name);
        return this;
    }

    public BookmarkCreatorDialog defaultDesc(String desc) {
        this.tfDesc.setText(desc);
        return this;
    }

    public BookmarkCreatorDialog namePrompt(String name) {
        this.tfName.setPlaceholder(name);
        return this;
    }

    public BookmarkCreatorDialog descPrompt(String name) {
        this.tfDesc.setPlaceholder(name);
        return this;
    }

    @Override
    protected JComponent createCenterPanel() {

        tfDesc.setOneLineMode(false);
        tfDesc.setBorder(JBUI.Borders.empty());

        new ComponentValidator(project)
                .withValidator(() -> {
                    if (tfName.getText().isBlank()) {
                        return new ValidationInfo(I18N.get("bookmarkNameNonNullMessage"), tfName);
                    }
                    return null;
                })
                .installOn(tfName);

        tfName.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                ComponentValidator.getInstance(tfName).ifPresent(ComponentValidator::revalidate);
                setOKActionEnabled(!tfName.getText().isBlank());
            }
        });

        // 行号输入框验证（仅在编辑模式）
        if (isEditMode) {
            new ComponentValidator(project)
                    .withValidator(() -> {
                        try {
                            int line = Integer.parseInt(tfLineNumber.getText().trim());
                            if (line < 0) {
                                return new ValidationInfo(I18N.get("bookmark.lineNumberNegative"), tfLineNumber);
                            }
                            if (line >= maxLineNumber) {
                                return new ValidationInfo(I18N.get("bookmark.lineNumberTooLarge", String.valueOf(maxLineNumber)), tfLineNumber);
                            }

                        } catch (NumberFormatException e) {
                            return new ValidationInfo(I18N.get("bookmark.lineNumberInvalid"), tfLineNumber);
                        }
                        return null;
                    })
                    .installOn(tfLineNumber);
            tfLineNumber.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void documentChanged(@NotNull DocumentEvent e) {
                    ComponentValidator.getInstance(tfLineNumber).ifPresent(ComponentValidator::revalidate);
                    setOKActionEnabled(!tfLineNumber.getText().isBlank());
                }
            });
        }

        // 创建容器面板
        JPanel panel = new JPanel(new GridBagLayout());

        // 创建 GridBagConstraints 对象
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = JBUI.insets(8);

        // 第一行第一列
        JLabel lbName = new JLabel(I18N.get("bookmark.dialog.name"));
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0;
        constraints.weighty = 0;
        panel.add(lbName, constraints);

        // 第一行第二列
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 0;
        panel.add(tfName, constraints);

        // 行号输入行（仅在编辑模式显示）
        if (isEditMode) {
            JLabel lbLineNumber = new JLabel(I18N.get("bookmark.dialog.lineNumber"));
            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.weightx = 0;
            constraints.weighty = 0;
            panel.add(lbLineNumber, constraints);

            tfLineNumber.setPreferredSize(new Dimension(100, 28));
            constraints.gridx = 1;
            constraints.gridy = 1;
            constraints.weightx = 1;
            constraints.weighty = 0;
            panel.add(tfLineNumber, constraints);
        }

        // 第二行第一列
        JLabel lbDesc = new JLabel(I18N.get("bookmark.dialog.desc"));
        constraints.gridx = 0;
        constraints.gridy = isEditMode ? 2 : 1; // 根据模式调整行号;
        constraints.weightx = 0;
        constraints.weighty = 1;
        panel.add(lbDesc, constraints);

        // 第二行第二列
        JBScrollPane scrollPane = new JBScrollPane(tfDesc);
        scrollPane.setPreferredSize(new JBDimension(400, 200));
        scrollPane.setBorder(JBUI.Borders.empty());


        constraints.gridx = 1;
        constraints.gridy = isEditMode ? 2 : 1; // 根据模式调整行号;
        constraints.weightx = 1;
        constraints.weighty = 1;
        panel.add(scrollPane, constraints);

        pack();

        return panel;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return tfName;
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        String name = tfName.getText();
        String desc = tfDesc.getText();
        Integer lineNumber = null;
        if (isEditMode) {
            lineNumber = Integer.parseInt(tfLineNumber.getText().trim()) - 1; // 行号从0开始
        }

        if (null != oKAction) {
            oKAction.onAction(name, desc, lineNumber);
        }
    }

    public void showAndCallback(OnOKAction onOKAction) {
        this.oKAction = onOKAction;
        show();
    }

    public interface OnOKAction {
        void onAction(String name, String desc, Integer newLineNumber);
    }

}
