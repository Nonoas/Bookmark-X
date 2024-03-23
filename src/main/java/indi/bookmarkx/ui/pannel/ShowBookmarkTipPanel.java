package indi.bookmarkx.ui.pannel;

import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.JBUI;
import indi.bookmarkx.model.AbstractTreeNodeModel;
import indi.bookmarkx.model.BookmarkNodeModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @author: codeleep
 * @createTime: 2024/03/21 11:49
 * @description:
 */
public class ShowBookmarkTipPanel extends JBPanel {

    private final EditorTextField tfName = new EditorTextField();
    private final EditorTextField tfDesc = new EditorTextField();

    public ShowBookmarkTipPanel(@NotNull AbstractTreeNodeModel abstractTreeNodeModel) {
        tfName.setText(abstractTreeNodeModel.getName());
        if (abstractTreeNodeModel.isBookmark()) {
            tfDesc.setText(((BookmarkNodeModel)abstractTreeNodeModel).getDesc());
        }
        init(abstractTreeNodeModel);
    }


    private void init(@NotNull AbstractTreeNodeModel abstractTreeNodeModel) {
        setLayout(new GridBagLayout());
        tfName.setPlaceholder("输入展示在标签上的文本");
        tfName.setEnabled(false);
        tfDesc.setPlaceholder("输入对标签的详细描述");
        tfDesc.setEnabled(false);
        tfDesc.setOneLineMode(false);
        tfDesc.setBorder(JBUI.Borders.empty());
        // 创建 GridBagConstraints 对象
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = JBUI.insets(5);

        // 第一行第一列
        JLabel lbName = new JLabel("标签");
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0;
        constraints.weighty = 0;
        add(lbName, constraints);

        // 第一行第二列
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 0;
        add(tfName, constraints);

        if (abstractTreeNodeModel.isBookmark()) {
            // 第二行第一列
            JLabel lbDesc = new JLabel("描述");
            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.weightx = 0;
            constraints.weighty = 1;
            add(lbDesc, constraints);

            // 第二行第二列
            JBScrollPane scrollPane = new JBScrollPane(tfDesc);
            scrollPane.setPreferredSize(new JBDimension(400, 150));
            scrollPane.setBorder(JBUI.Borders.empty());

            constraints.gridx = 1;
            constraints.gridy = 1;
            constraints.weightx = 1;
            constraints.weighty = 1;
            add(scrollPane, constraints);
        }

    }

}
