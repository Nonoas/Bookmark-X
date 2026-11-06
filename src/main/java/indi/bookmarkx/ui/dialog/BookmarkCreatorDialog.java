package indi.bookmarkx.ui.dialog;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.HtmlPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import indi.bookmarkx.BookmarksManager;
import indi.bookmarkx.common.I18N;
import indi.bookmarkx.model.BookmarkDescriptionFormat;
import indi.bookmarkx.ui.tree.BookmarkTree;
import indi.bookmarkx.ui.tree.BookmarkTreeNode;
import indi.bookmarkx.utils.BookmarkDescriptionFormatter;
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
    private final ComboBox<GroupItem> cbGroup = new ComboBox<>();
    private final ComboBox<BookmarkDescriptionFormat> cbDescriptionFormat = new ComboBox<>(new BookmarkDescriptionFormat[]{
            BookmarkDescriptionFormat.HTML,
            BookmarkDescriptionFormat.PLAIN_TEXT
    });
    private final HtmlPanel previewPanel = new DescriptionPreviewPanel();

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentCard = new JPanel(cardLayout);

    // 标记当前是否为预览模式
    private boolean isPreviewMode = false;

    private boolean isEditMode = false;
    private boolean showGroup = false;
    private boolean showDescriptionFormat = false;
    private BookmarkTreeNode defaultGroup;
    private int maxLineNumber = 0;
    private OnOKAction oKAction;
    private Project project;

    public BookmarkCreatorDialog(Project project, String title) {
        super(true);
        initSelf(project, title);
    }

    private BookmarkCreatorDialog(Project project, String title, boolean showGroup) {
        super(true);
        this.showGroup = showGroup;
        this.showDescriptionFormat = true;
        initSelf(project, title);
    }

    public BookmarkCreatorDialog(Project project, String title, int lineNumber, int maxLineNumber) {
        super(true);
        this.isEditMode = true;
        this.showGroup = true;
        this.showDescriptionFormat = true;
        this.tfLineNumber.setText(String.valueOf(lineNumber));
        this.maxLineNumber = maxLineNumber;
        initSelf(project, title);
    }

    public static BookmarkCreatorDialog createBookmarkDialog(Project project, String title) {
        return new BookmarkCreatorDialog(project, title, true);
    }

    private void initSelf(Project project, String title) {
        setTitle(title);
        this.project = project;
        configureDescriptionFormatComboBox();
        init();
    }

    private void configureDescriptionFormatComboBox() {
        cbDescriptionFormat.setSelectedItem(BookmarkDescriptionFormat.HTML);
        cbDescriptionFormat.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus
                );
                BookmarkDescriptionFormat format = value instanceof BookmarkDescriptionFormat
                        ? (BookmarkDescriptionFormat) value
                        : BookmarkDescriptionFormat.HTML;
                boolean html = format == BookmarkDescriptionFormat.HTML;
                label.setText(I18N.get(html
                        ? "bookmark.dialog.descriptionFormat.html"
                        : "bookmark.dialog.descriptionFormat.plainText"));
                label.setIcon(html ? AllIcons.FileTypes.Html : AllIcons.FileTypes.Text);
                label.setIconTextGap(JBUI.scale(6));
                return label;
            }
        });
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

    public BookmarkCreatorDialog defaultDescriptionFormat(BookmarkDescriptionFormat format) {
        cbDescriptionFormat.setSelectedItem(BookmarkDescriptionFormat.orDefault(format));
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

    public BookmarkCreatorDialog defaultGroup(@Nullable BookmarkTreeNode group) {
        if (group == null || !group.isGroup()) {
            return this;
        }
        this.defaultGroup = group;
        selectGroup(group);
        return this;
    }

    private void initializeGroupChoices() {
        if (!showGroup) {
            return;
        }
        BookmarkTree tree = BookmarksManager.getInstance(project).getToolWindowRootPanel().tree();
        BookmarkTreeNode selectedGroup = defaultGroup == null ? tree.getActivatedGroupNode() : defaultGroup;
        cbGroup.removeAllItems();
        for (BookmarkTreeNode group : tree.getGroupNodes()) {
            cbGroup.addItem(new GroupItem(group, tree.getGroupPath(group)));
        }
        selectGroup(selectedGroup);
    }

    private void selectGroup(BookmarkTreeNode group) {
        for (int i = 0; i < cbGroup.getItemCount(); i++) {
            GroupItem item = cbGroup.getItemAt(i);
            if (item.group == group) {
                cbGroup.setSelectedIndex(i);
                return;
            }
        }
    }

    @Nullable
    private BookmarkTreeNode getSelectedGroup() {
        GroupItem selectedItem = (GroupItem) cbGroup.getSelectedItem();
        return selectedItem == null ? null : selectedItem.group;
    }

    @Override
    protected JComponent createCenterPanel() {
        setupValidators();
        initializeGroupChoices();

        // 1. 编辑器与预览面板
        tfDesc.setOneLineMode(false);
        tfDesc.setBorder(JBUI.Borders.empty(5));
        JBScrollPane editScroll = new JBScrollPane(tfDesc);

        previewPanel.setBorder(JBUI.Borders.empty(10));
        JBScrollPane previewScroll = new JBScrollPane(previewPanel);
        cbDescriptionFormat.addActionListener(event -> {
            if (isPreviewMode) {
                updatePreview();
            }
        });

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

        int row = 0;
        // Name
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.weightx = 0;
        panel.add(new JBLabel(I18N.get("bookmark.dialog.name")), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(tfName, gbc);

        if (showGroup) {
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.weightx = 0;
            panel.add(new JBLabel(I18N.get("bookmark.dialog.group")), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(cbGroup, gbc);
        }

        if (isEditMode) {
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.weightx = 0;
            panel.add(new JBLabel(I18N.get("bookmark.dialog.lineNumber")), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(createLineAndFormatPanel(), gbc);
        } else if (showDescriptionFormat) {
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.weightx = 0;
            panel.add(new JBLabel(I18N.get("bookmark.dialog.descriptionFormat")), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(cbDescriptionFormat, gbc);
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

    private JPanel createLineAndFormatPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.weightx = 0.35;
        panel.add(tfLineNumber, gbc);

        if (showDescriptionFormat) {
            gbc.gridx = 1;
            gbc.weightx = 0;
            gbc.insets = JBUI.insetsLeft(12);
            panel.add(new JBLabel(I18N.get("bookmark.dialog.descriptionFormat")), gbc);

            gbc.gridx = 2;
            gbc.weightx = 0.65;
            gbc.insets = JBUI.insetsLeft(8);
            panel.add(cbDescriptionFormat, gbc);
        }
        return panel;
    }

    private void updatePreview() {
        String text = tfDesc.getText();
        Color fg = UIUtil.getLabelForeground();
        String colorHex = String.format("#%02x%02x%02x", fg.getRed(), fg.getGreen(), fg.getBlue());
        String htmlContent = String.format(
                "<html><body style='font-family: %s; font-size: 11pt; color: %s;'>" +
                        "%s</body></html>",
                JBUI.Fonts.label().getFontName(), colorHex,
                BookmarkDescriptionFormatter.format(text, getDescriptionFormat())
        );
        previewPanel.setText(htmlContent);
    }

    private BookmarkDescriptionFormat getDescriptionFormat() {
        return BookmarkDescriptionFormat.orDefault(
                (BookmarkDescriptionFormat) cbDescriptionFormat.getSelectedItem()
        );
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

        return BookmarkDialogResult.ok(name, desc, line, getSelectedGroup(), getDescriptionFormat());
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

            if (isEditMode) {
                line = Integer.parseInt(tfLineNumber.getText().trim());
            }

            callback.onConfirmed(name, desc, line);
        }
    }

    @FunctionalInterface
    public interface BookmarkCallback {
        void onConfirmed(@NotNull String name, @NotNull String desc, @Nullable Integer line);
    }

    private static class GroupItem {
        private final BookmarkTreeNode group;
        private final String path;

        private GroupItem(BookmarkTreeNode group, String path) {
            this.group = group;
            this.path = path;
        }

        @Override
        public String toString() {
            return path;
        }
    }

    /**
     * Represents the result of a bookmark creation/editing dialog.
     */
    public static class BookmarkDialogResult {

        private final boolean ok;
        private final String name;
        private final String desc;
        private final Integer line;
        private final BookmarkTreeNode group;
        private final BookmarkDescriptionFormat descriptionFormat;

        private BookmarkDialogResult(boolean ok, @Nullable String name,
                                     @Nullable String desc, @Nullable Integer line,
                                     @Nullable BookmarkTreeNode group,
                                     BookmarkDescriptionFormat descriptionFormat) {
            this.ok = ok;
            this.name = name;
            this.desc = desc;
            this.line = line;
            this.group = group;
            this.descriptionFormat = BookmarkDescriptionFormat.orDefault(descriptionFormat);
        }

        /**
         * Creates a result indicating the dialog was cancelled.
         */
        public static BookmarkDialogResult cancel() {
            return new BookmarkDialogResult(
                    false, null, null, null, null, BookmarkDescriptionFormat.HTML
            );
        }

        /**
         * Creates a result with the user input values.
         */
        public static BookmarkDialogResult ok(@NotNull String name,
                                              @NotNull String desc,
                                              @Nullable Integer line) {
            return ok(name, desc, line, null);
        }

        public static BookmarkDialogResult ok(@NotNull String name,
                                              @NotNull String desc,
                                              @Nullable Integer line,
                                              @Nullable BookmarkTreeNode group) {
            return ok(name, desc, line, group, BookmarkDescriptionFormat.HTML);
        }

        public static BookmarkDialogResult ok(@NotNull String name,
                                              @NotNull String desc,
                                              @Nullable Integer line,
                                              @Nullable BookmarkTreeNode group,
                                              BookmarkDescriptionFormat descriptionFormat) {
            return new BookmarkDialogResult(true, name, desc, line, group, descriptionFormat);
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

        @Nullable
        public BookmarkTreeNode getGroup() {
            return group;
        }

        public BookmarkDescriptionFormat getDescriptionFormat() {
            return descriptionFormat;
        }
    }
}
