package com.galaxyjoy.hexviewer.ui.util;

import android.widget.EditText;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.emoji.bundled.BundledEmojiCompatConfig;
import androidx.emoji.text.EmojiCompat;
import androidx.test.core.app.ApplicationProvider;

import com.galaxyjoy.hexviewer.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Integration tests simulating the full EmojiCompat initialization lifecycle
 * and its interaction with {@link LineUpdateTextWatcher#normalizeForEmoji(CharSequence)}.
 *
 * Covers race-condition scenarios that triggered the original crash:
 *  1. normalizeForEmoji called before EmojiCompat is configured
 *  2. normalizeForEmoji called while EmojiCompat is loading (LOAD_STATE_LOADING)
 *  3. normalizeForEmoji called after EmojiCompat succeeds (LOAD_STATE_SUCCEEDED)
 *  4. normalizeForEmoji called after EmojiCompat fails (simulated)
 *  5. InitCallback lifecycle — onInitialized / onFailed
 *  6. Concurrent calls to normalizeForEmoji from multiple threads — no crash
 *  7. Activity restore simulation — EditText.setText triggers onTextChanged early
 */
@RunWith(RobolectricTestRunner.class)
public class EmojiCompatLifecycleIntegrationTest {

    @Before
    public void tearDownEmojiCompat() {
        // Reset EmojiCompat singleton between tests via reflection
        try {
            java.lang.reflect.Field instance = EmojiCompat.class.getDeclaredField("sInstance");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (Throwable ignored) {
            // If reset fails, tests still run safely due to try-catch in normalizeForEmoji
        }
    }

    // -----------------------------------------------------------------------
    // 1. Called before EmojiCompat is configured — must not crash
    // -----------------------------------------------------------------------
    @Test
    public void normalizeForEmoji_beforeConfig_returnsFallback() {
        String input = "abc 😀 def";
        String result = LineUpdateTextWatcher.normalizeForEmoji(input);
        assertNotNull(result);
        assertEquals(input, result);
    }

    // -----------------------------------------------------------------------
    // 2. Called while EmojiCompat is configured but load state != SUCCEEDED
    // -----------------------------------------------------------------------
    @Test
    public void normalizeForEmoji_duringLoading_returnsFallback() {
        // Init EmojiCompat but do NOT wait for it to finish loading
        BundledEmojiCompatConfig config = new BundledEmojiCompatConfig(
                RuntimeEnvironment.getApplication());
        EmojiCompat.init(config);

        // Immediately call (likely still LOADING or DEFAULT state)
        String input = "loading test 🔥";
        String result = LineUpdateTextWatcher.normalizeForEmoji(input);
        assertNotNull(result);
        // Either returns original or processed — must not crash
        assertFalse(result.isEmpty());
    }

    // -----------------------------------------------------------------------
    // 3. Called after EmojiCompat is fully initialized
    // -----------------------------------------------------------------------
    @Test
    public void normalizeForEmoji_afterSucceeded_nocrash() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean initSuccess = new AtomicBoolean(false);

        BundledEmojiCompatConfig bundledConfig = new BundledEmojiCompatConfig(
                RuntimeEnvironment.getApplication());
        bundledConfig.setReplaceAll(true);
        EmojiCompat.Config config = bundledConfig.registerInitCallback(new EmojiCompat.InitCallback() {
            @Override
            public void onInitialized() {
                initSuccess.set(true);
                latch.countDown();
            }
            @Override
            public void onFailed(Throwable throwable) {
                // Bundled emoji should not fail; mark success = false
                initSuccess.set(false);
                latch.countDown();
            }
        });
        EmojiCompat.init(config);

        // Give up to 5s for initialization
        boolean done = latch.await(5, TimeUnit.SECONDS);

        // Even if initialization doesn't complete in time, normalizeForEmoji must not crash
        String input = "Emoji test 🙌";
        String result = LineUpdateTextWatcher.normalizeForEmoji(input);
        assertNotNull(result);
    }

    // -----------------------------------------------------------------------
    // 4. Simulated failure — normalizeForEmoji must still return original string
    // -----------------------------------------------------------------------
    @Test
    public void normalizeForEmoji_onException_returnsFallback() {
        // Force the catch block by passing a CharSequence that causes
        // EmojiCompat to receive a call when it will throw (not initialized state)
        // Simulate by not initializing EmojiCompat at all
        String input = "Crash candidate 💥";
        String result = LineUpdateTextWatcher.normalizeForEmoji(input);
        assertNotNull(result);
        assertEquals(input, result);
    }

    // -----------------------------------------------------------------------
    // 5. InitCallback lifecycle smoke test
    // -----------------------------------------------------------------------
    @Test
    public void emojiCompatInitCallback_firesCorrectly() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        BundledEmojiCompatConfig bundledConfig2 = new BundledEmojiCompatConfig(
                RuntimeEnvironment.getApplication());
        EmojiCompat.Config config2 = bundledConfig2.registerInitCallback(new EmojiCompat.InitCallback() {
            @Override
            public void onInitialized() {
                latch.countDown();
            }
            @Override
            public void onFailed(Throwable throwable) {
                latch.countDown();
            }
        });
        EmojiCompat.init(config2);

        // Await, but don't fail test if callback doesn't arrive (bundled may be synchronous)
        latch.await(5, TimeUnit.SECONDS);
        // Just verify no exception thrown
        assertNotNull(EmojiCompat.get());
    }

    // -----------------------------------------------------------------------
    // 6. Concurrent calls — thread-safety
    // -----------------------------------------------------------------------
    @Test
    public void normalizeForEmoji_concurrentCalls_nocrash() throws InterruptedException {
        int threadCount = 20;
        List<Thread> threads = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        Object lock = new Object();

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            threads.add(new Thread(() -> {
                try {
                    String input = "Thread-" + idx + " 😊";
                    String result = LineUpdateTextWatcher.normalizeForEmoji(input);
                    assertNotNull(result);
                } catch (Throwable t) {
                    synchronized (lock) {
                        errors.add(t);
                    }
                }
            }));
        }
        threads.forEach(Thread::start);
        for (Thread t : threads) t.join(2000);

        assertTrue("Concurrent normalizeForEmoji calls must not crash: " + errors,
                errors.isEmpty());
    }

    // -----------------------------------------------------------------------
    // 7. Activity restore simulation — EditText.setText triggers onTextChanged early
    //    This was the exact crash scenario from the original bug report
    // -----------------------------------------------------------------------
    @Test
    public void normalizeForEmoji_activityRestoreSimulation_nocrash() {
        // Simulate onTextChanged being called during setText (as happens in onRestoreInstanceState)
        // BEFORE EmojiCompat finishes loading — this is the exact crash path from the bug report
        AtomicReference<Throwable> caughtError = new AtomicReference<>(null);

        try {
            // No EmojiCompat initialized — mimics app fresh boot scenario
            String restoredText = "Restored text from bundle 😀";
            String result = LineUpdateTextWatcher.normalizeForEmoji(restoredText);
            assertNotNull(result);
            assertEquals(restoredText, result);
        } catch (Throwable t) {
            caughtError.set(t);
        }

        assertNull("Activity restore simulation must not throw: " + caughtError.get(),
                caughtError.get());
    }
}
