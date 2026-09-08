package indi.bookmarkx.service.relocation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LineMappingTest {

    @Test
    void mapsLineAfterInsertedBlock() {
        LineMapping mapping = LineMapping.fromLines(
                TextLineUtil.splitLines("a\nbookmark\nc"),
                TextLineUtil.splitLines("a\nx\ny\nbookmark\nc")
        );

        assertEquals(3, mapping.mapLine(1, 3, 5));
    }

    @Test
    void mapsLineAfterDeletedBlock() {
        LineMapping mapping = LineMapping.fromLines(
                TextLineUtil.splitLines("a\nx\ny\nbookmark\nc"),
                TextLineUtil.splitLines("a\nbookmark\nc")
        );

        assertEquals(1, mapping.mapLine(3, 5, 3));
    }

    @Test
    void mapsLineFromUnifiedDiff() {
        LineMapping mapping = LineMapping.fromUnifiedDiff(
                "@@ -1,0 +2,2 @@\n" +
                        "+x\n" +
                        "+y\n" +
                        "@@ -3,0 +5,1 @@\n" +
                        "+z\n"
        );

        assertEquals(3, mapping.mapLine(1, 3, 6));
        assertEquals(4, mapping.mapLine(2, 3, 6));
    }
}
