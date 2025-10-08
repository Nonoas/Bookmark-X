package indi.bookmarkx.ui.pannel;

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
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

public class BookmarkTipPanel extends JBPanel<BookmarkTipPanel> {

    public BookmarkTipPanel(@NotNull AbstractTreeNodeModel model) {
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

    private static JBLabel createHtmlLabel(@NotNull AbstractTreeNodeModel model) {
        String name = escapeHtml(model.getName());
        String desc = model.getDesc() != null && !model.getDesc().trim().isEmpty() ? escapeHtml(model.getDesc()) : "no description";

        String html = "<html>" +
                "<body style='font-family:" + UIManager.getFont("Label.font").getFamily() + ";" +
                " font-size:" + UIManager.getFont("Label.font").getSize() + "pt;" +
                " color:" + toHtmlColor(JBColor.foreground()) + "; margin:0;'>" +
                "<h3 style='margin:0; padding:0;'>" + name + "</h3>" +
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

