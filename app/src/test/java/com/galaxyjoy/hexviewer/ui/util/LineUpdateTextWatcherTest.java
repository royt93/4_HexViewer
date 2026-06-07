package com.galaxyjoy.hexviewer.ui.util;

import androidx.emoji.text.EmojiCompat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class LineUpdateTextWatcherTest {

    @Test
    public void testNormalizeForEmoji_NullInput() {
        String result = LineUpdateTextWatcher.normalizeForEmoji(null);
        assertNotNull(result);
        assertEquals("", result);
    }

    @Test
    public void testNormalizeForEmoji_EmptyInput() {
        String result = LineUpdateTextWatcher.normalizeForEmoji("");
        assertNotNull(result);
        assertEquals("", result);
    }

    @Test
    public void testNormalizeForEmoji_NormalInput() {
        String input = "Hello World 123";
        String result = LineUpdateTextWatcher.normalizeForEmoji(input);
        assertNotNull(result);
        assertEquals(input, result);
    }

    @Test
    public void testNormalizeForEmoji_SafeFallbackWhenNotInitialized() {
        // By default in unit tests, EmojiCompat is not initialized or fails to load.
        // We verify that calling normalizeForEmoji does not crash with IllegalStateException
        // and safely falls back to returning the input string.
        String inputWithEmoji = "Test 😊 emoji";
        String result = LineUpdateTextWatcher.normalizeForEmoji(inputWithEmoji);
        assertNotNull(result);
        assertEquals(inputWithEmoji, result);
    }
}
