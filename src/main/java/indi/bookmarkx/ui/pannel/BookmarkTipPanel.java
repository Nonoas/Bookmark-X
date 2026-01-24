package indi.bookmarkx.ui.pannel;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import indi.bookmarkx.model.AbstractTreeNodeModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

public class BookmarkTipPanel extends JBPanel<BookmarkTipPanel> {

    private Runnable onOpenInToolWindow;
    private final AbstractTreeNodeModel model;

    public BookmarkTipPanel(@NotNull AbstractTreeNodeModel model) {
        this.model = model;
        setLayout(new BorderLayout());

        // 使用 JBLabel + HTML 渲染
        JBLabel htmlLabel = createHtmlLabel(model);

        // 放入滚动面板
        JBScrollPane scrollPane = new JBScrollPane(htmlLabel);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6, Integer.MAX_VALUE));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        add(scrollPane, BorderLayout.CENTER);

        // 边距和圆角
        setBorder(JBUI.Borders.empty(8, 10));
        setBackground(JBColor.PanelBackground);
        setOpaque(true);
    }

    public BookmarkTipPanel(@NotNull AbstractTreeNodeModel model, Runnable onOpenInToolWindow) {
        this.model = model;
        this.onOpenInToolWindow = onOpenInToolWindow;
        setLayout(new BorderLayout());

        // 1. 内容区 (你的 HTML 内容)
        JBLabel htmlLabel = createHtmlLabel(model);
        JBScrollPane scrollPane = new JBScrollPane(htmlLabel);
        scrollPane.setBorder(JBUI.Borders.empty());
        add(scrollPane, BorderLayout.CENTER);

        // 2. 底部工具栏 (仿 Document 弹窗)
        add(createBottomPanel(), BorderLayout.SOUTH);

        setPreferredSize(new Dimension(400, 300));
        setBackground(JBColor.PanelBackground);
    }

    private static JBLabel createHtmlLabel(@NotNull AbstractTreeNodeModel model) {
        String name = escapeHtml(model.getName());
        String desc = model.getDesc() != null && !model.getDesc().trim().isEmpty() ? model.getDesc() : "no description";

        String html = "<html>" +
                "<body style='font-family:" + UIManager.getFont("Label.font").getFamily() + ";" +
                " font-size:" + UIManager.getFont("Label.font").getSize() + "pt;" +
                " color:" + toHtmlColor(JBColor.foreground()) + "; margin:0;'>" +
                "<h2 style='margin:0; padding:0;'>" + name + "</h2>" +
                "<div style='margin-top:4px; color:" + toHtmlColor(JBColor.gray) + ";'>" + desc + "</div>" +
                "</body></html>";

        JBLabel label = new JBLabel(html);
        label.setOpaque(false);
        label.setVerticalAlignment(SwingConstants.TOP);
        return label;
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String toHtmlColor(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private JPanel createBottomPanel() {
        // 创建 ActionGroup (三个点菜单里的内容)
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new AnAction("Open in Tool Window", "Move this content to tool window", AllIcons.Actions.More) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (onOpenInToolWindow != null) {
                    onOpenInToolWindow.run();
                }
            }
        });

        // 将 Group 变成“三个点”按钮
        ActionToolbar toolbar = ActionManager.getInstance()
                .createActionToolbar("BookmarkTipToolbar", actionGroup, true);
        toolbar.setTargetComponent(this);
        toolbar.getComponent().setOpaque(false);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0)); // 顶部加一条细线
        panel.add(toolbar.getComponent(), BorderLayout.EAST); // 靠右对齐
        return panel;
    }

    /**
     * 可选：生成类似 IDEA 注释弹窗的 Balloon 显示
     */
    public static void showTooltip(@NotNull Component owner, @NotNull AbstractTreeNodeModel model) {
        BookmarkTipPanel panel = new BookmarkTipPanel(model);
        BalloonBuilder builder = JBPopupFactory.getInstance()
                .createBalloonBuilder(panel)
                .setFillColor(JBColor.PanelBackground)
                .setBorderColor(JBColor.border())
                .setShadow(true)
                .setHideOnClickOutside(true)
                .setHideOnKeyOutside(true)
                .setAnimationCycle(200)
                .setCloseButtonEnabled(false);

        Balloon balloon = builder.createBalloon();
        balloon.showInCenterOf((JComponent) owner);
    }
}

