package indi.bookmarkx.ui.pannel;

import com.intellij.codeInsight.documentation.DocumentationComponent;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.icons.AllIcons;
import com.intellij.lang.documentation.DocumentationMarkup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import indi.bookmarkx.model.AbstractTreeNodeModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;

/**
 * 使用 IDEA 完全原生的 DocumentationComponent 实现的书签提示面板
 * 这是最接近 IDEA 原生 JavaDoc 显示的方式
 */
public class BookmarkTipPanel extends JBPanel<BookmarkTipPanel> {

    private final Project project;
    private final AbstractTreeNodeModel model;
    private DocumentationComponent documentationComponent;
    private Runnable onOpenInToolWindow;

    /**
     * 简单构造器，用于 Balloon 弹窗显示
     */
    public BookmarkTipPanel(@NotNull Project project, @NotNull AbstractTreeNodeModel model) {
        this.project = project;
        this.model = model;
        setLayout(new BorderLayout());
        initComponents(false);
    }

    /**
     * 完整构造器，带工具栏
     */
    public BookmarkTipPanel(@NotNull Project project,
                                  @NotNull AbstractTreeNodeModel model,
                                  @Nullable Runnable onOpenInToolWindow) {
        this.project = project;
        this.model = model;
        this.onOpenInToolWindow = onOpenInToolWindow;
        setLayout(new BorderLayout());
        initComponents(true);
    }

    private void initComponents(boolean withToolbar) {
        // 创建 IDEA 原生的 DocumentationComponent
        DocumentationManager docManager = DocumentationManager.getInstance(project);
        documentationComponent = new DocumentationComponent(docManager);

        // 设置文档内容
        String html = generateDocumentationHtml(model);
        documentationComponent.setData(
                null,  // targetElement (我们是自定义文档，没有 PSI 元素)
                html,  // 文档 HTML 内容
                null,  // originalElement
                null,  // url
                null   // ref
        );

        add(documentationComponent, BorderLayout.CENTER);

        // 添加工具栏
        if (withToolbar) {
            add(createBottomPanel(), BorderLayout.SOUTH);
            setPreferredSize(JBUI.size(400, 300));
        }

        setOpaque(false);
    }

    /**
     * 使用 IDEA DocumentationMarkup 标准格式生成文档 HTML
     */
    private String generateDocumentationHtml(@NotNull AbstractTreeNodeModel model) {
        StringBuilder html = new StringBuilder();

        html.append("<html><body>");

        // === DEFINITION SECTION (定义区域，显示名称) ===
        html.append(DocumentationMarkup.DEFINITION_START);
        String name = escapeHtml(model.getName());
        html.append("<b>").append(name).append("</b>");
        html.append(DocumentationMarkup.DEFINITION_END);

        // === CONTENT SECTION (内容区域，显示描述) ===
        String desc = model.getDesc();
        if (desc != null && !desc.trim().isEmpty()) {
            html.append(DocumentationMarkup.CONTENT_START);
            html.append(formatDescription(desc));
            html.append(DocumentationMarkup.CONTENT_END);
        } else {
            html.append(DocumentationMarkup.CONTENT_START);
            html.append("<p style='color:gray;'><i>No description available</i></p>");
            html.append(DocumentationMarkup.CONTENT_END);
        }

        // === SECTIONS (可选的附加信息) ===
        // 你可以添加更多的 section，比如标签、创建时间等
        String tags = getTagsInfo(model);
        if (tags != null) {
            html.append(DocumentationMarkup.SECTIONS_START);
            html.append(DocumentationMarkup.SECTION_HEADER_START);
            html.append("Tags:");
            html.append(DocumentationMarkup.SECTION_SEPARATOR);
            html.append(tags);
            html.append(DocumentationMarkup.SECTION_END);
            html.append(DocumentationMarkup.SECTIONS_END);
        }

        html.append("</body></html>");

        return html.toString();
    }

    /**
     * 格式化描述文本
     */
    private String formatDescription(String desc) {
        if (desc == null) return "";

        // HTML 转义
        desc = escapeHtml(desc);

        // 处理换行：双换行变段落，单换行变 br
        desc = desc.replaceAll("(\r?\n){2,}", "</p><p>");
        desc = desc.replace("\n", "<br/>");

        // 包装为段落
        return "<p>" + desc + "</p>";
    }

    /**
     * 获取标签信息（示例，根据你的 model 实际情况调整）
     */
    @Nullable
    private String getTagsInfo(AbstractTreeNodeModel model) {
        // 这里假设你的 model 有 getTags() 方法
        // 如果没有，可以删除或返回 null
        try {
            // String tags = model.getTags();
            // if (tags != null && !tags.isEmpty()) {
            //     return escapeHtml(tags);
            // }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    /**
     * 创建底部工具栏
     */
    private JPanel createBottomPanel() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        // "在工具窗口中打开"操作
        if (onOpenInToolWindow != null) {
            actionGroup.add(new AnAction("Open in Tool Window",
                    "Open this content in a tool window",
                    AllIcons.Toolwindows.ToolWindowChanges) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    onOpenInToolWindow.run();
                }
            });
        }

        // 创建工具栏
        ActionToolbar toolbar = ActionManager.getInstance()
                .createActionToolbar("BookmarkDocToolbar", actionGroup, true);
        toolbar.setTargetComponent(this);
        toolbar.getComponent().setOpaque(false);

        // 底部面板
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0));
        panel.add(toolbar.getComponent(), BorderLayout.EAST);

        return panel;
    }

    /**
     * 更新文档内容
     */
    public void updateContent(@NotNull AbstractTreeNodeModel newModel) {
        String html = generateDocumentationHtml(newModel);
        documentationComponent.setData(null, html, null, null, null);
    }

    /**
     * HTML 转义
     */
    private static String escapeHtml(String s) {
        //if (s == null) return "";
        //return s.replace("&", "&amp;")
        //        .replace("<", "&lt;")
        //        .replace(">", "&gt;")
        //        .replace("\"", "&quot;");
        return s;
    }

    /**
     * 释放资源
     */
    public void dispose() {
        // DocumentationComponent 会自动管理资源
    }

    /**
     * 显示为原生样式的 Balloon 弹窗
     */
    public static void showTooltip(@NotNull Component owner,
                                   @NotNull Project project,
                                   @NotNull AbstractTreeNodeModel model) {
        BookmarkTipPanel panel = new BookmarkTipPanel(project, model);

        BalloonBuilder builder = JBPopupFactory.getInstance()
                .createBalloonBuilder(panel)
                .setFillColor(UIUtil.getToolTipBackground())
                .setBorderColor(JBColor.border())
                .setShadow(true)
                .setHideOnClickOutside(true)
                .setHideOnKeyOutside(true)
                .setAnimationCycle(200)
                .setCloseButtonEnabled(false)
                .setBlockClicksThroughBalloon(true)
                .setRequestFocus(true);

        Balloon balloon = builder.createBalloon();

        if (owner instanceof JComponent) {
            balloon.showInCenterOf((JComponent) owner);
        } else {
            balloon.show(
                    JBPopupFactory.getInstance().guessBestPopupLocation((JComponent) owner),
                    Balloon.Position.above
            );
        }
    }
}