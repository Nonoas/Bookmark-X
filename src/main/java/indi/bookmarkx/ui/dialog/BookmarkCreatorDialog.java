package indi.bookmarkx.ui.dialog;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.HtmlPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import indi.bookmarkx.common.I18N;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author Nonoas
 * @date 2023/6/1
 */
public class BookmarkCreatorDialog extends DialogWrapper {

    private final EditorTextField tfName = new EditorTextField();
    private final EditorTextField tfDesc = new EditorTextField();
    private final EditorTextField tfLineNumber = new EditorTextField();
    private final HtmlPanel previewPanel = new DescriptionPreviewPanel();

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentCard = new JPanel(cardLayout);

    // 标记当前是否为预览模式
    private boolean isPreviewMode = false;

    private boolean isEditMode = false;
    private int maxLineNumber = 0;
    private OnOKAction oKAction;
    private Project project;

    public BookmarkCreatorDialog(Project project, String title) {
        super(true);
        initSelf(project, title);
    }

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

    // --- Fluent API ---
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
        setupValidators();

        // 1. 编辑器与预览面板
        tfDesc.setOneLineMode(false);
        tfDesc.setBorder(JBUI.Borders.empty(5));
        JBScrollPane editScroll = new JBScrollPane(tfDesc);

        previewPanel.setBorder(JBUI.Borders.empty(10));
        JBScrollPane previewScroll = new JBScrollPane(previewPanel);

        contentCard.add(editScroll, "EDIT");
        contentCard.add(previewScroll, "PREVIEW");

        // 2. 单个切换按钮 (类似 IDE 原生风格)
        // 初始状态显示为“预览”图标
        JBLabel toggleBtn = new JBLabel(AllIcons.Actions.Preview);
        toggleBtn.setToolTipText(I18N.get("bookmark.dialog.preview"));
        toggleBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        toggleBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                isPreviewMode = !isPreviewMode;
                if (isPreviewMode) {
                    updatePreview();
                    cardLayout.show(contentCard, "PREVIEW");
                    toggleBtn.setIcon(AllIcons.Actions.Edit); // 切换为编辑图标
                    toggleBtn.setToolTipText(I18N.get("bookmark.dialog.edit"));
                } else {
                    cardLayout.show(contentCard, "EDIT");
                    toggleBtn.setIcon(AllIcons.Actions.Preview); // 切换回预览图标
                    toggleBtn.setToolTipText(I18N.get("bookmark.dialog.preview"));
                }
            }
        });

        // 3. 悬浮容器
        JLayeredPane layeredPane = new JLayeredPane() {
            @Override
            public void doLayout() {
                contentCard.setBounds(0, 0, getWidth(), getHeight());
                Dimension prefSize = toggleBtn.getPreferredSize();
                // 放在右上角，避开边框
                toggleBtn.setBounds(getWidth() - prefSize.width - 10, 8, prefSize.width, prefSize.height);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(500, 250);
            }
        };
        layeredPane.add(contentCard, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(toggleBtn, JLayeredPane.POPUP_LAYER);
        layeredPane.setBorder(JBUI.Borders.customLine(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()));

        // 4. 主布局
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = JBUI.insets(8);

        // Name
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(new JBLabel(I18N.get("bookmark.dialog.name")), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(tfName, gbc);

        int row = 1;
        if (isEditMode) {
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.weightx = 0;
            panel.add(new JBLabel(I18N.get("bookmark.dialog.lineNumber")), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(tfLineNumber, gbc);
        }

        // Desc
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JBLabel(I18N.get("bookmark.dialog.desc")), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.weighty = 1;
        panel.add(layeredPane, gbc);

        return panel;
    }

    private void updatePreview() {
        String text = tfDesc.getText();
        Color fg = UIUtil.getLabelForeground();
        String colorHex = String.format("#%02x%02x%02x", fg.getRed(), fg.getGreen(), fg.getBlue());
        String htmlContent = String.format(
                "<html><body style='font-family: %s; font-size: 11pt; color: %s;'>" +
                        "%s</body></html>",
                JBUI.Fonts.label().getFontName(), colorHex, text.replace("\n", "<br>")
        );
        previewPanel.setText(htmlContent);
    }

    private void setupValidators() {
        // Name validator
        new ComponentValidator(project).withValidator(() -> {
            if (tfName.getText().isBlank()) return new ValidationInfo(I18N.get("bookmarkNameNonNullMessage"), tfName);
            return null;
        }).installOn(tfName);

        tfName.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                ComponentValidator.getInstance(tfName).ifPresent(ComponentValidator::revalidate);
                validateAndEnableOK();
            }
        });

        // Line number validator (only in edit mode)
        if (isEditMode) {
            new ComponentValidator(project).withValidator(() -> {
                String text = tfLineNumber.getText().trim();
                if (text.isEmpty()) {
                    return new ValidationInfo(I18N.get("bookmark.lineNumberInvalid"), tfLineNumber);
                }
                try {
                    int line = Integer.parseInt(text);
                    if (line < 1) {
                        return new ValidationInfo(I18N.get("bookmark.lineNumberNegative"), tfLineNumber);
                    }
                    if (line > maxLineNumber) {
                        return new ValidationInfo(
                                I18N.get("bookmark.lineNumberTooLarge", String.valueOf(maxLineNumber)),
                                tfLineNumber
                        );
                    }
                } catch (NumberFormatException e) {
                    return new ValidationInfo(I18N.get("bookmark.lineNumberInvalid"), tfLineNumber);
                }
                return null;
            }).installOn(tfLineNumber);

            tfLineNumber.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void documentChanged(@NotNull DocumentEvent event) {
                    ComponentValidator.getInstance(tfLineNumber).ifPresent(ComponentValidator::revalidate);
                    validateAndEnableOK();
                }
            });
        }
    }

    private void validateAndEnableOK() {
        boolean nameValid = !tfName.getText().isBlank();
        boolean lineValid = true;
        if (isEditMode) {
            lineValid = isValidLineNumber(tfLineNumber.getText().trim());
        }
        setOKActionEnabled(nameValid && lineValid);
    }

    private boolean isValidLineNumber(String text) {
        if (text.isEmpty()) return false;
        try {
            int line = Integer.parseInt(text);
            return line >= 1 && line <= maxLineNumber;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return tfName;
    }

    @Override
    protected void doOKAction() {
        if (oKAction != null) {
            Integer line = null;
            if (isEditMode) {
                try {
                    line = Integer.parseInt(tfLineNumber.getText().trim()) - 1;
                } catch (Exception ignored) {
                }
            }
            oKAction.onAction(tfName.getText(), tfDesc.getText(), line);
        }
        super.doOKAction();
    }

    public void showAndCallback(OnOKAction onOKAction) {
        this.oKAction = onOKAction;
        show();
    }

    public interface OnOKAction {
        void onAction(String name, String desc, @Nullable Integer newLineNumber);
    }

    private static class DescriptionPreviewPanel extends HtmlPanel {
        @Override
        protected @NotNull String getBody() {
            return "";
        }

        @Override
        public void setText(String text) {
            super.setText(text);
        }
    }

    /**
     * Shows the dialog and returns the result.
     * This is the preferred way to use the dialog in modern code.
     *
     * @return BookmarkDialogResult containing user input or cancellation status
     */
    @NotNull
    public BookmarkDialogResult showAndGetResult() {
        boolean ok = showAndGet();
        if (!ok) {
            return BookmarkDialogResult.cancel();
        }

        String name = tfName.getText().trim();
        String desc = tfDesc.getText().trim();
        int line = 0;

        String lineStr = tfLineNumber.getText().trim();
        if (StringUtils.isNotBlank(lineStr)) {
            try {
                line = Integer.parseInt(lineStr);
            } catch (NumberFormatException e) {
                return BookmarkDialogResult.cancel();
            }
        }

        return BookmarkDialogResult.ok(name, desc, line);
    }

    /**
     * Legacy callback-style method for backward compatibility.
     * Consider migrating to {@link #showAndGetResult()}.
     */
    public void showAndGet(@NotNull BookmarkCallback callback) {
        boolean ok = showAndGet();
        if (ok) {
            String name = tfName.getText().trim();
            String desc = tfDesc.getText().trim();
            Integer line = null;

            line = Integer.parseInt(tfLineNumber.getText().trim());

            callback.onConfirmed(name, desc, line);
        }
    }

    @FunctionalInterface
    public interface BookmarkCallback {
        void onConfirmed(@NotNull String name, @NotNull String desc, @Nullable Integer line);
    }

    /**
     * Represents the result of a bookmark creation/editing dialog.
     */
    public static class BookmarkDialogResult {

        private final boolean ok;
        private final String name;
        private final String desc;
        private final Integer line;

        private BookmarkDialogResult(boolean ok, @Nullable String name,
                                     @Nullable String desc, @Nullable Integer line) {
            this.ok = ok;
            this.name = name;
            this.desc = desc;
            this.line = line;
        }

        /**
         * Creates a result indicating the dialog was cancelled.
         */
        public static BookmarkDialogResult cancel() {
            return new BookmarkDialogResult(false, null, null, null);
        }

        /**
         * Creates a result with the user input values.
         */
        public static BookmarkDialogResult ok(@NotNull String name,
                                              @NotNull String desc,
                                              @Nullable Integer line) {
            return new BookmarkDialogResult(true, name, desc, line);
        }

        public boolean isOk() {
            return ok;
        }

        public boolean isCancelled() {
            return !ok;
        }

        @Nullable
        public String getName() {
            return name;
        }

        @Nullable
        public String getDesc() {
            return desc;
        }

        public Integer getLine() {
            return line;
        }
    }
}