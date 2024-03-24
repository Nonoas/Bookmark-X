package indi.bookmarkx;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.ComponentUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.WindowMoveListener;
import com.intellij.util.ui.JBUI;
import indi.bookmarkx.common.BaseColors;
import indi.bookmarkx.model.BookmarkNodeModel;
import indi.bookmarkx.ui.tree.BookmarkTreeNode;

import javax.swing.*;
import javax.swing.plaf.LayerUI;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class BookmarkPopup {

    private BookmarkTreeNode treeNode;

    private final JPanel jPanel;

    private final JBPopup popup;

    private final OpenFileDescriptor ofd;

    private final JLabel indexLabel;

    private final JLabel lbClose;

    private final LayerUI<JComponent> layerUI = new PopupLayer(BaseColors.GREEN);

    private final LayerUI<JComponent> layerUI2 = new PopupLayer(BaseColors.PURPLE);

    private final JLayer jLayer = new JLayer<>();

    Rectangle myViewBounds;

    private final BookmarkNodeModel bookmarkModel;

    @SuppressWarnings("unchecked")
    public BookmarkPopup(BookmarkNodeModel bookmarkModel, Editor editor) {

        this.bookmarkModel = bookmarkModel;
        this.ofd = bookmarkModel.getOpenFileDescriptor();

        JLabel fileLabel = new JLabel();
        fileLabel.setText(String.format("%s", bookmarkModel.getName()));
        fileLabel.setIcon(bookmarkModel.getIcon());

        this.indexLabel = new JLabel(bookmarkModel.getIndex() + "  ");
        this.lbClose = new JLabel(AllIcons.Actions.Close);

        this.lbClose.setToolTipText("Close. Alt-Click to Close Others  ; Shift-Click to Close All");
        this.jPanel = new JPanel();
        this.jPanel.setLayout(new FlowLayout());
        this.jPanel.add(this.indexLabel);
        this.jPanel.add(fileLabel);
        this.jPanel.add(this.lbClose);
        this.jLayer.setView(this.jPanel);
        this.jLayer.setUI(this.layerUI);

        WindowMoveListener windowListener = new WindowMoveListener(this.jPanel) {
            Point myLocation;

            @Override
            public void mouseDragged(MouseEvent e) {
                super.mouseDragged(e);
                if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
                    int dx = e.getXOnScreen() - this.myLocation.x;
                    int dy = e.getYOnScreen() - this.myLocation.y;
                    System.out.println(dx + "," + dy);
//                    BookmarksManager.moveOthers(dx, dy, BookmarkPopup.this);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
                    this.myLocation = null;
//                    BookmarksManager.stopMoveOthers(BookmarkPopup.this);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
                    this.myLocation = e.getLocationOnScreen();
//                    BookmarksManager.startMoveOthers(BookmarkPopup.this);
                }
            }
        };

        this.indexLabel.addMouseListener(windowListener);
        this.indexLabel.addMouseMotionListener(windowListener);

        this.lbClose.addMouseListener(windowListener);
        this.lbClose.addMouseMotionListener(windowListener);

        fileLabel.addMouseListener(windowListener);
        fileLabel.addMouseMotionListener(windowListener);
        fileLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
//                BookmarksManager.select(BookmarkPopup.this);
            }
        });

        this.lbClose.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if ((e.getModifiersEx() & 0x40) != 0) {
//                    BookmarksManager.removeAndCLoseAll();
                } else if ((e.getModifiersEx() & 0x200) != 0) {
//                    BookmarksManager.removeAndCLoseOthers(BookmarkPopup.this);
                } else {
                    BookmarkPopup.this.close();
                }
            }

            public void mouseEntered(MouseEvent e) {
                BookmarkPopup.this.lbClose.setIcon(AllIcons.Actions.CloseHovered);
            }

            public void mouseExited(MouseEvent e) {
                BookmarkPopup.this.lbClose.setIcon(AllIcons.Actions.Close);
            }
        });

        MouseAdapter selectUI = new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                BookmarkPopup.this.jLayer.setUI(BookmarkPopup.this.layerUI2);
            }

            public void mouseExited(MouseEvent e) {
                BookmarkPopup.this.jLayer.setUI(BookmarkPopup.this.layerUI);
            }
        };
        this.jPanel.addMouseListener(selectUI);
        this.indexLabel.addMouseListener(selectUI);
        fileLabel.addMouseListener(selectUI);
        this.lbClose.addMouseListener(selectUI);
        this.popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(this.jLayer, this.jLayer)
                .setFocusable(true)
                .setRequestFocus(true)
                .setMovable(true)
                .setCancelKeyEnabled(false)
                .setResizable(false)
                .setCancelOnClickOutside(false)
                .setCancelOnOtherWindowOpen(false)
                .setCancelOnOtherWindowOpen(false)
                .setCancelOnWindowDeactivation(false)
                .setShowBorder(false)
                .createPopup();
        this.popup.showInBestPositionFor(editor);
    }

    public void select(boolean over) {
        this.jPanel.setBorder(!over ? JBUI.Borders.customLine(JBColor.LIGHT_GRAY) : JBUI.Borders.customLine(JBColor.darkGray));
    }

    public void navigate() {
        this.ofd.navigate(true);
        if (!this.popup.isVisible())
            this.popup.setUiVisible(true);
    }

    public void setIndex(int index) {
        this.indexLabel.setText(index + " ");
    }

    public void registerToTreeNode(BookmarkTreeNode node) {
        node.setUserObject(this);
        this.treeNode = node;
    }

    public void close() {
        this.popup.cancel();
    }

    public void move(int dx, int dy) {
        Window myView = ComponentUtil.getWindow(this.jPanel);
        Rectangle bounds = new Rectangle(this.myViewBounds);
        bounds.x += dx;
        bounds.y += dy;
        if (!bounds.equals(myView.getBounds())) {
            myView.setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
            myView.invalidate();
            myView.validate();
            myView.repaint();
        }
    }

    public void stopMove() {
        this.myViewBounds = null;
    }

    public void startMove() {
        Window myView = ComponentUtil.getWindow(this.jPanel);
        this.myViewBounds = myView.getBounds();
    }

    public BookmarkTreeNode getTreeNode() {
        return treeNode;
    }

    public void setTreeNode(BookmarkTreeNode treeNode) {
        this.treeNode = treeNode;
    }


    private static class PopupLayer extends LayerUI<JComponent> {

        private final Color color;

        PopupLayer(Color color) {
            this.color = color;
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            super.paint(g, c);
            g.setColor(color);
            g.fillRect(0, 0, c.getWidth(), c.getHeight());
        }
    }

    ;
}
