package com.galaxyjoy.hexviewer.ui.util;

import android.text.SpannableString;

import androidx.emoji.bundled.BundledEmojiCompatConfig;
import androidx.emoji.text.EmojiCompat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for {@link LineUpdateTextWatcher#normalizeForEmoji(CharSequence)}.
 *
 * All tests run in Robolectric — EmojiCompat is NOT initialized by default,
 * simulating the exact timing that caused the original crash.
 *
 * Covers:
 *  1.  null input → returns ""
 *  2.  empty String → returns ""
 *  3.  normal ASCII (not initialized) → same text, no crash
 *  4.  emoji text (not initialized) → same text, no crash  ← KEY BUG FIX SCENARIO
 *  5.  hex-like text (typical app content) → same text
 *  6.  whitespace-only → same text
 *  7.  single character → same text
 *  8.  very long string (10_000 chars) → no crash, same text
 *  9.  SpannableString input (CharSequence subtype) → same text
 * 10.  ASCII text after EmojiCompat initialized → same text
 * 11.  emoji text after EmojiCompat initialized → non-null result, no crash
 * 12.  null input after EmojiCompat initialized → ""
 * 13.  empty input after EmojiCompat initialized → ""
 */
@RunWith(RobolectricTestRunner.class)
public class LineUpdateTextWatcherUnitTest {

    // Helper: reset the EmojiCompat singleton
    private void resetEmojiCompat() {
        try {
            java.lang.reflect.Field f = EmojiCompat.class.getDeclaredField("sInstance");
            f.setAccessible(true);
            f.set(null, null);
        } catch (Throwable ignored) { }
    }

    // Helper: initialize EmojiCompat (bundled)
    private void initEmojiCompat() {
        try {
            BundledEmojiCompatConfig config = new BundledEmojiCompatConfig(
                    RuntimeEnvironment.getApplication());
            EmojiCompat.init(config);
        } catch (Throwable ignored) { }
    }

    // =====================================================================
    // Group A — EmojiCompat NOT initialized (simulates cold start / restore)
    // =====================================================================

    @Test
    public void test01_nullInput_returnsEmpty() {
        resetEmojiCompat();
        assertEquals("", LineUpdateTextWatcher.normalizeForEmoji(null));
    }

    @Test
    public void test02_emptyString_returnsEmpty() {
        resetEmojiCompat();
        assertEquals("", LineUpdateTextWatcher.normalizeForEmoji(""));
    }

    @Test
    public void test03_asciiText_notInitialized_returnsSameText() {
        resetEmojiCompat();
        String input = "Hello World 123";
        String result = LineUpdateTextWatcher.normalizeForEmoji(input);
        assertNotNull(result);
        assertEquals(input, result);
    }

    @Test
    public void test04_emojiText_notInitialized_returnsSameText_nocrash() {
        // KEY scenario: this previously threw IllegalStateException: Not initialized yet
        resetEmojiCompat();
        String input = "Test \uD83D\uDE0A emoji \uD83C\uDF89";
        String result = LineUpdateTextWatcher.normalizeForEmoji(input);
        assertNotNull(result);
        assertEquals(input, result);
    }

    @Test
    public void test05_hexLikeInput_notInitialized_returnsSameText() {
        resetEmojiCompat();
        String input = "FF 0A 3B C4 7E 12 56 D8";
        String result = LineUpdateTextWatcher.normalizeForEmoji(input);
        assertNotNull(result);
        assertEquals(input, result);
    }

    @Test
    public void test06_whitespaceOnly_notInitialized_returnsSame() {
        resetEmojiCompat();
        String input = "   \t  ";
        String result = LineUpdateTextWatcher.normalizeForEmoji(input);
        assertNotNull(result);
        assertEquals(input, result);
    }

    @Test
    public void test07_singleChar_notInitialized_returnsSame() {
        resetEmojiCompat();
        String input = "X";
        String result = LineUpdateTextWatcher.normalizeForEmoji(input);
        assertNotNull(result);
        assertEquals(input, result);
    }

    @Test
    public void test08_veryLongString_notInitialized_nocrash() {
        resetEmojiCompat();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10_000; i++) sb.append((char) ('A' + (i % 26)));
        String input = sb.toString();
        String result = LineUpdateTextWatcher.normalizeForEmoji(input);
        assertNotNull(result);
        assertEquals(input, result);
    }

    @Test
    public void test09_spannableInput_notInitialized_returnsSame() {
        resetEmojiCompat();
        SpannableString spannable = new SpannableString("Spannable content");
        String result = LineUpdateTextWatcher.normalizeForEmoji(spannable);
        assertNotNull(result);
        assertEquals("Spannable content", result);
    }

    // =====================================================================
    // Group B — EmojiCompat IS initialized
    // =====================================================================

    @Test
    public void test10_asciiText_initialized_returnsSameText() {
        resetEmojiCompat();
        initEmojiCompat();
        String input = "Hello World";
        String result = LineUpdateTextWatcher.normalizeForEmoji(input);
        assertNotNull(result);
        // ASCII-only: no emoji spans → unchanged
        assertEquals(input, result);
    }

    @Test
    public void test11_emojiText_initialized_nocrash() {
        resetEmojiCompat();
        initEmojiCompat();
        String input = "Hello \uD83D\uDE0A";
        String result = LineUpdateTextWatcher.normalizeForEmoji(input);
        assertNotNull(result);
        // Result may differ if EmojiCompat processed spans, but must not be null or crash
    }

    @Test
    public void test12_nullInput_initialized_returnsEmpty() {
        resetEmojiCompat();
        initEmojiCompat();
        assertEquals("", LineUpdateTextWatcher.normalizeForEmoji(null));
    }

    @Test
    public void test13_emptyInput_initialized_returnsEmpty() {
        resetEmojiCompat();
        initEmojiCompat();
        assertEquals("", LineUpdateTextWatcher.normalizeForEmoji(""));
    }
}
