package indi.bookmarkx.tree;

import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import indi.bookmarkx.model.BookmarkNodeModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.tree.TreePath;

/**
 * @author Nonoas
 * @date 2023/6/8
 */
class BookmarkTreeTest {

    private Project project;

    @BeforeEach
    public void setUp() throws Exception {

    }

    @Test
    public void testAddNode() {
//        BookmarkTree tree = new BookmarkTree(project);
//
//        BookmarkNodeModel model = new BookmarkNodeModel();
//        model.setUuid("uuid");
//
//        BookmarkTreeNode node = new BookmarkTreeNode(model);
//
//        tree.add(node);
//
//        int rowForPath = tree.getRowForPath(new TreePath(node.getPath()));
//        // 添加之后的节点必须存在于当前树上
//        Assertions.assertNotEquals(rowForPath, -1, "添加之后的节点必须存在于当前树上");
    }

}