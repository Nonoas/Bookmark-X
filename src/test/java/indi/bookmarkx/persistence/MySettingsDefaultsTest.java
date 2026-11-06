package indi.bookmarkx.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class MySettingsDefaultsTest {
    @Test
    void treeDecorationsAreDisabledByDefault() {
        MySettings.State state = new MySettings.State();

        assertFalse(state.showNodeNumbers);
        assertFalse(state.showModuleSuffix);
    }
}
