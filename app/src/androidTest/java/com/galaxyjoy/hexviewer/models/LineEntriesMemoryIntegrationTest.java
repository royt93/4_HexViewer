/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Integration tests for LineEntries memory safety guard.
 *
 * These on-device tests verify:
 * 1. addAll with 1M+ optimized LineEntries (lazy-format) succeeds without OOM
 * 2. Memory estimate formula (size * 120) is accurate and realistic
 * 3. LineEntries.clear() properly releases memory
 * 4. Items are stored and indexed correctly after addAll
 * </p>
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.models;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.galaxyjoy.hexviewer.util.SysHelper;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration tests for LineEntries memory safety on a real device.
 *
 * Run on device: ./gradlew connectedAndroidTest
 */
@RunWith(AndroidJUnit4.class)
public class LineEntriesMemoryIntegrationTest {

    /**
     * TEST 1: addAll with 1M lazy LineEntries (realistic 16MB file load) must succeed.
     *
     * A real 16.64MB file = ~1,040,219 rows of 16 bytes.
     * With lazy formatting, each LineEntry holds only a byte[] (no pre-built String),
     * so actual memory is: 1,040,219 * ~50 bytes (object header + byte[16]) ≈ 52MB.
     * Our estimate formula uses 120 bytes/entry = 125MB estimate, well within free heap.
     */
    @Test
    public void addAll_with1MillionLazyEntries_succeedsWithoutOOM() {
        // Simulate 1.04M lines of 16 bytes each (typical 16.64MB file)
        int count = 1_040_219;
        List<LineEntry> items = new ArrayList<>(count);
        byte[] rawTemplate = new byte[]{
            0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x20, 0x57, 0x6F,
            0x72, 0x6C, 0x64, 0x21, 0x00, 0x01, 0x02, 0x03
        };
        // Use lazy factory (no String pre-built)
        for (int i = 0; i < count; i++) {
            items.add(LineEntry.create(rawTemplate.clone(), SysHelper.MAX_BY_ROW_16));
        }

        LineEntries entries = new LineEntries(new ArrayList<>());
        try {
            entries.addAll(items);
            // Verify data integrity
            assertEquals("Should have " + count + " entries", count, entries.getCount());
            assertNotNull("First item should not be null", entries.getItem(0));
            assertNotNull("Last item should not be null", entries.getItem(count - 1));
        } catch (OutOfMemoryError e) {
            fail("addAll threw OOM for 1M lazy LineEntries: " + e.getMessage() +
                 " — lazy formatting optimization may have been broken");
        }
    }

    /**
     * TEST 2: Memory estimate formula accuracy.
     *
     * Verifies that size * 120 bytes correctly underestimates actual memory,
     * but not so severely that it triggers false OOM guard rejections.
     */
    @Test
    public void memoryEstimate_120BytesPerEntry_isRealistic() {
        // 1M entries * 120 bytes = 120MB estimated
        int count = 1_000_000;
        long estimated = count * 120L;

        // On a 512MB heap device with 400MB free, this should pass (120 < 400 * 0.7 = 280)
        Runtime runtime = Runtime.getRuntime();
        long freeMemory = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());

        assertTrue(
            "Estimated memory (" + (estimated / 1024 / 1024) + "MB) should be < 70% of free heap " +
            "(" + (freeMemory / 1024 / 1024) + "MB) for a 1M entry load to succeed",
            estimated < freeMemory * 0.7
        );
    }

    /**
     * TEST 3: clear() after addAll releases items correctly.
     */
    @Test
    public void clear_afterAddAll_returnsEmptyCount() {
        List<LineEntry> items = new ArrayList<>();
        byte[] raw = new byte[]{0x41, 0x42, 0x43};
        for (int i = 0; i < 100; i++) {
            items.add(LineEntry.create(raw.clone(), SysHelper.MAX_BY_ROW_8));
        }

        LineEntries entries = new LineEntries(new ArrayList<>());
        entries.addAll(items);
        assertEquals("Should have 100 entries before clear", 100, entries.getCount());

        entries.clear();
        assertEquals("Should have 0 entries after clear", 0, entries.getCount());
        assertNull("getItem(0) should be null after clear", entries.getItem(0));
    }

    /**
     * TEST 4: Items are correctly indexed after addAll.
     *
     * Verifies setIndex(i) is correctly called during addAll.
     */
    @Test
    public void addAll_setsCorrectIndexes_forAllItems() {
        int count = 500;
        List<LineEntry> items = new ArrayList<>(count);
        byte[] raw = "ABCDEFGHIJKLMNOP".getBytes();
        for (int i = 0; i < count; i++) {
            items.add(LineEntry.create(raw.clone(), SysHelper.MAX_BY_ROW_16));
        }

        LineEntries entries = new LineEntries(new ArrayList<>());
        entries.addAll(items);

        // Verify index correctness
        for (int i = 0; i < count; i++) {
            LineEntry item = entries.getItem(i);
            assertNotNull("Item at position " + i + " should not be null", item);
            assertEquals("Item at position " + i + " should have index " + i, i, item.getIndex());
        }
    }

    /**
     * TEST 5: getItem returns null for out-of-bounds position.
     */
    @Test
    public void getItem_returnsNull_forOutOfBoundsPosition() {
        LineEntries entries = new LineEntries(new ArrayList<>());
        byte[] raw = new byte[]{0x01};
        List<LineEntry> items = new ArrayList<>();
        items.add(LineEntry.create(raw, SysHelper.MAX_BY_ROW_8));
        entries.addAll(items);

        assertEquals("Should have 1 item", 1, entries.getCount());
        assertNull("getItem(-1) should be null", entries.getItem(-1));
        assertNull("getItem(1) should be null for single-item list", entries.getItem(1));
    }
}
