package indi.bookmarkx.ui.pannel;

import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.HtmlPanel;
import com.intellij.util.ui.JBUI;
import indi.bookmarkx.model.AbstractTreeNodeModel;
import indi.bookmarkx.utils.HtmlUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.JScrollPane;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * 用于展示标签树节点的描述信息
 *
 * @author Nonoas
 * @date 2024/3/24 10:54
 */
public class BookmarkTipPanel extends JBPanel<BookmarkTipPanel> {

    public BookmarkTipPanel(AbstractTreeNodeModel model) {

        setLayout(new BorderLayout());

        TipHtmlPanel tipHtmlPanel = new TipHtmlPanel(model);
        tipHtmlPanel.setSize(tipHtmlPanel.getPreferredSize());

        JBScrollPane scrollPane = new JBScrollPane(tipHtmlPanel);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6, Integer.MAX_VALUE));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(JBUI.Borders.empty());

        add(scrollPane);
    }

    protected static class TipHtmlPanel extends HtmlPanel {
        private final String TITLE_TAG = "h3";
        private final String DESC_TAG = "";

        public TipHtmlPanel(@NotNull AbstractTreeNodeModel nodeModel) {
            StringBuilder sb = new StringBuilder();
            String name = HtmlUtil.wrapText(TITLE_TAG, nodeModel.getName());
            sb.append(name);
            String desc = StringUtils.isNotBlank(nodeModel.getDesc()) ? nodeModel.getDesc() : "no description";
            sb.append(desc);
            setBody(sb.toString());
            Border borderWithPadding = JBUI.Borders.empty(0, 10, 10, 10);
            setBorder(borderWithPadding);

        }

        @Override
        public Dimension getPreferredSize() {
            Dimension preferredSize = super.getPreferredSize();
            if (preferredSize.width > 300) {
                preferredSize.width = 300;
            }
            return preferredSize;
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            Dimension preferredSize = getPreferredSize();
            return new Dimension(Math.min(preferredSize.width, 300), Math.min(preferredSize.height, 150));
        }

        @Override
        protected @NotNull @Nls String getBody() {
            return getText();
        }
    }

}
