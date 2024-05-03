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
    private OnOKAction oKAction;

    private Project project;

    public BookmarkCreatorDialog(Project project, String title) {
        super(true);
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
                        return new ValidationInfo("书签名称不能为空", tfName);
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

        // 创建容器面板
        JPanel panel = new JPanel(new GridBagLayout());

        // 创建 GridBagConstraints 对象
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = JBUI.insets(5);

        // 第一行第一列
        JLabel lbName = new JLabel("名称");
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

        // 第二行第一列
        JLabel lbDesc = new JLabel("描述");
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 0;
        constraints.weighty = 1;
        panel.add(lbDesc, constraints);

        // 第二行第二列
        JBScrollPane scrollPane = new JBScrollPane(tfDesc);
        scrollPane.setPreferredSize(new JBDimension(400, 150));
        scrollPane.setBorder(JBUI.Borders.empty());


        constraints.gridx = 1;
        constraints.gridy = 1;
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

        if (null != oKAction) {
            oKAction.onAction(name, desc);
        }
    }

    public void showAndCallback(OnOKAction onOKAction) {
        this.oKAction = onOKAction;
        show();
    }

    public interface OnOKAction {
        void onAction(String name, String desc);
    }

}
