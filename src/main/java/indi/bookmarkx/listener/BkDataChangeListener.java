package indi.bookmarkx.listener;

import indi.bookmarkx.model.AbstractTreeNodeModel;

/**
 * @author Nonoas
 * @date 2024/12/15
 * @since
 */
public interface BkDataChangeListener {

    void onDataAdd(AbstractTreeNodeModel model);

    void onDataDelete(AbstractTreeNodeModel model);

}
