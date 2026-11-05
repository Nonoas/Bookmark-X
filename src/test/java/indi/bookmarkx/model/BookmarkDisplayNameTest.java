package indi.bookmarkx.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookmarkDisplayNameTest {
    @Test
    void existingAndCustomNamesReceiveModuleSuffix() {
        assertEquals("Controller.java [web]", BookmarkNodeModel.withModuleName("Controller.java", "web"));
        assertEquals("入口方法 [web]", BookmarkNodeModel.withModuleName("入口方法", "web"));
    }

    @Test
    void newBookmarksDoNotRepeatModuleSuffix() {
        assertEquals("Controller.java [web]", BookmarkNodeModel.withModuleName("Controller.java [web]", "web"));
    }

    @Test
    void missingFileDoesNotInventModuleName() {
        assertEquals("Controller.java", BookmarkNodeModel.withModuleName("Controller.java", null));
    }
}
