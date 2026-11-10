package indi.bookmarkx.service.relocation;

import indi.bookmarkx.model.BookmarkNodeModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnchorRelocationStrategyTest {

    @Test
    void recoversUniqueAnchorWithoutSnapshot() {
        BookmarkNodeModel bookmark = bookmark("    public void handleCallback() {");
        RelocationResult result = relocate(bookmark, "header\ninserted\n    public void handleCallback() {").orElseThrow();
        assertEquals(2, result.getLine());
    }

    @Test
    void recoversWrappedSignatureThatBecameSingleLine() {
        BookmarkNodeModel bookmark = bookmark("    public Map<String, Object> execute(Map<String, Object> req,");
        String current = "header\ncomment\n    public Map<String, Object> execute(Map<String, Object> req, Payload payload) {";
        assertEquals(2, relocate(bookmark, current).orElseThrow().getLine());
    }

    @Test
    void leavesAmbiguousOverloadsUnchanged() {
        BookmarkNodeModel bookmark = bookmark("    public Map<String, Object> execute(Map<String, Object> req,");
        String current = "public Map<String, Object> execute(Map<String, Object> req, Payload payload) {\n"
                + "public Map<String, Object> execute(Map<String, Object> req, String payload) {";
        assertTrue(relocate(bookmark, current).isEmpty());
    }

    @Test
    void leavesDuplicateAndMissingAnchorsUnchanged() {
        BookmarkNodeModel bookmark = bookmark("public void handleCallback() {");
        assertTrue(relocate(bookmark, "public void handleCallback() {\npublic void handleCallback() {").isEmpty());
        assertTrue(relocate(bookmark, "public void handleAnotherCallback() {").isEmpty());
    }

    @Test
    void doesNotTreatCommonLinesOrArbitraryPrefixesAsAnchors() {
        assertTrue(relocate(bookmark("return null;"), "header\nreturn null;").isEmpty());
        assertTrue(relocate(bookmark("auditService.recordSomething(payload,"), "auditService.recordSomething(payload, context);").isEmpty());
    }

    private BookmarkNodeModel bookmark(String anchor) {
        BookmarkNodeModel bookmark = new BookmarkNodeModel();
        bookmark.setLine(151);
        bookmark.setAnchorLineText(anchor);
        bookmark.setAnchorLineHash(TextLineUtil.hashLine(anchor));
        return bookmark;
    }

    private java.util.Optional<RelocationResult> relocate(BookmarkNodeModel bookmark, String current) {
        return new AnchorRelocationStrategy().relocate(new RelocationContext(
                null, null, bookmark, null, TextLineUtil.splitLines(current)));
    }
}
