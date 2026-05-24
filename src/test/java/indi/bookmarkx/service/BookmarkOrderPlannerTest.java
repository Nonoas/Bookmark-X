package indi.bookmarkx.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookmarkOrderPlannerTest {

    @Test
    void reorderShouldAcceptExactUuidPermutation() {
        List<String> reordered = BookmarkOrderPlanner.reorder(
                List.of("a", "b", "c"),
                List.of("c", "a", "b")
        );

        assertEquals(List.of("c", "a", "b"), reordered);
    }

    @Test
    void reorderShouldRejectMissingUuid() {
        assertThrows(IllegalArgumentException.class, () -> BookmarkOrderPlanner.reorder(
                List.of("a", "b", "c"),
                List.of("a", "b")
        ));
    }

    @Test
    void moveShouldInsertBookmarkAtTargetIndex() {
        List<String> reordered = BookmarkOrderPlanner.move(
                List.of("a", "b", "c"),
                "c",
                1
        );

        assertEquals(List.of("a", "c", "b"), reordered);
    }

    @Test
    void moveShouldRejectUnknownUuid() {
        assertThrows(IllegalArgumentException.class, () -> BookmarkOrderPlanner.move(
                List.of("a", "b", "c"),
                "x",
                1
        ));
    }
}
