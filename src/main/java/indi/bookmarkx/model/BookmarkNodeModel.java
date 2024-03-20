package indi.bookmarkx.model;

import com.intellij.openapi.fileEditor.OpenFileDescriptor;

import javax.swing.*;
import java.util.Objects;

/**
 * 书签数据模型
 *
 * @author Nonoas
 * @date 2023/6/4
 */
public class BookmarkNodeModel extends AbstractTreeNodeModel {

    private String uuid;

    private int index;
    private int line;
    private int column;


    private String name;
    private String desc;

    private Icon icon;

    /**
     * 文件跳转器
     */
    private OpenFileDescriptor openFileDescriptor;

    public BookmarkNodeModel() {
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookmarkNodeModel yourClass = (BookmarkNodeModel) o;
        return uuid.equals(yourClass.uuid);
    }


    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public OpenFileDescriptor getOpenFileDescriptor() {
        return openFileDescriptor;
    }

    public void setOpenFileDescriptor(OpenFileDescriptor openFileDescriptor) {
        this.openFileDescriptor = openFileDescriptor;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    public String getUuid() {
        return uuid;
    }

    @Override
    public boolean isBookmark() {
        return true;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
