/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Unit tests for LineEntries memory-estimation and list management behaviour.
 * </p>
 *
 * <p>
 * Robolectric is used because {@link LineEntries#addAll} calls
 * {@code android.util.Log} internally, which requires an Android runtime stub.
 * </p>
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.models;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Comprehensive Robolectric unit tests for {@link LineEntries}.
 *
 * <p>Naming convention: should_&lt;Expected&gt;_When_&lt;Condition&gt;</p>
 */
@RunWith(RobolectricTestRunner.class)
public class LineEntriesTest {

    /** Shared {@link LineEntries} instance, freshly constructed before each test. */
    private LineEntries mLineEntries;

    @Before
    public void setUp() {
        mLineEntries = new LineEntries(new ArrayList<>());
    }

    // ----------------------------------------------------------------
    // Helper factory
    // ----------------------------------------------------------------

    /**
     * Creates a simple {@link LineEntry} with a plain-text label and an empty raw list.
     *
     * @param label Human-readable label stored as the plain-text value.
     * @return A new {@link LineEntry} instance.
     */
    private static LineEntry createEntry(String label) {
        return new LineEntry(label, new ArrayList<>());
    }

    /**
     * Builds a {@link List} of {@link LineEntry} objects of the requested size.
     *
     * @param count Number of entries to create.
     * @return Populated list.
     */
    private static List<LineEntry> buildEntryList(int count) {
        List<LineEntry> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(createEntry("entry_" + i));
        }
        return list;
    }

    // ----------------------------------------------------------------
    // addAll – small collection (well below any memory limit)
    // ----------------------------------------------------------------

    @Test
    public void should_NotThrow_When_addAll_IsCalledWithSmallCollection() {
        List<LineEntry> entries = buildEntryList(10);
        // Must not throw any exception
        mLineEntries.addAll(entries);
    }

    @Test
    public void should_NotThrow_When_addAll_IsCalledWithSingleEntry() {
        List<LineEntry> entries = buildEntryList(1);
        mLineEntries.addAll(entries);
    }

    @Test
    public void should_NotThrow_When_addAll_IsCalledWithEmptyCollection() {
        List<LineEntry> emptyList = new ArrayList<>();
        // An empty collection must always succeed without error
        mLineEntries.addAll(emptyList);
    }

    @Test
    public void should_NotThrow_When_addAll_IsCalledTwiceSequentiallyWithSmallCollections() {
        mLineEntries.addAll(buildEntryList(5));
        mLineEntries.clear();
        mLineEntries.addAll(buildEntryList(5));
    }

    // ----------------------------------------------------------------
    // getCount
    // ----------------------------------------------------------------

    @Test
    public void should_ReturnZero_When_getCount_IsCalledOnFreshInstance() {
        assertEquals("A fresh LineEntries must have count 0", 0, mLineEntries.getCount());
    }

    @Test
    public void should_ReturnCorrectCount_When_getCount_IsCalledAfterAddAll() {
        int expected = 7;
        mLineEntries.addAll(buildEntryList(expected));
        assertEquals(
                "getCount() must equal the number of entries added via addAll()",
                expected,
                mLineEntries.getCount()
        );
    }

    @Test
    public void should_ReturnZero_When_getCount_IsCalledAfterAddAllWithEmptyCollection() {
        mLineEntries.addAll(new ArrayList<>());
        assertEquals(
                "getCount() must be 0 after addAll() with an empty collection",
                0,
                mLineEntries.getCount()
        );
    }

    @Test
    public void should_Return100_When_getCount_IsCalledAfterAddingOneHundredEntries() {
        mLineEntries.addAll(buildEntryList(100));
        assertEquals(
                "getCount() must return 100 after adding 100 entries",
                100,
                mLineEntries.getCount()
        );
    }

    // ----------------------------------------------------------------
    // clear
    // ----------------------------------------------------------------

    @Test
    public void should_ReturnZeroCount_When_clear_IsCalledAfterAddAll() {
        mLineEntries.addAll(buildEntryList(5));
        mLineEntries.clear();
        assertEquals(
                "getCount() must be 0 after clear()",
                0,
                mLineEntries.getCount()
        );
    }

    @Test
    public void should_NotThrow_When_clear_IsCalledOnEmptyInstance() {
        // clear() on a freshly created (empty) LineEntries must be safe
        mLineEntries.clear();
    }

    @Test
    public void should_ReturnZeroCount_When_clear_IsCalledMultipleTimes() {
        mLineEntries.addAll(buildEntryList(3));
        mLineEntries.clear();
        mLineEntries.clear(); // second clear must also be safe
        assertEquals("Count must remain 0 after repeated clear() calls", 0, mLineEntries.getCount());
    }

    // ----------------------------------------------------------------
    // getItem
    // ----------------------------------------------------------------

    @Test
    public void should_ReturnCorrectEntry_When_getItem_IsCalledWithValidPosition() {
        List<LineEntry> entries = buildEntryList(5);
        mLineEntries.addAll(entries);

        // addAll() calls setIndex(i) on each entry, so the insertion order is preserved
        LineEntry item = mLineEntries.getItem(0);
        assertNotNull("getItem(0) must not return null after addAll()", item);
    }

    @Test
    public void should_ReturnEntryAtPosition2_When_getItem_IsCalledWithPosition2() {
        mLineEntries.addAll(buildEntryList(5));

        LineEntry item = mLineEntries.getItem(2);
        assertNotNull("getItem(2) must return a non-null entry when 5 items were added", item);
    }

    @Test
    public void should_ReturnNull_When_getItem_IsCalledOnEmptyLineEntries() {
        // No items added – any position should return null
        LineEntry item = mLineEntries.getItem(0);
        assertNull("getItem(0) must return null when LineEntries is empty", item);
    }

    @Test
    public void should_ReturnNull_When_getItem_IsCalledWithOutOfBoundsPosition() {
        mLineEntries.addAll(buildEntryList(3));
        // Position 99 is beyond the 3 items we added
        LineEntry item = mLineEntries.getItem(99);
        assertNull("getItem() must return null for an out-of-bounds position", item);
    }

    @Test
    public void should_ReturnLastEntry_When_getItem_IsCalledWithLastValidPosition() {
        int count = 5;
        mLineEntries.addAll(buildEntryList(count));
        LineEntry lastItem = mLineEntries.getItem(count - 1);
        assertNotNull(
                "getItem(count - 1) must return a non-null entry for the last valid position",
                lastItem
        );
    }

    // ----------------------------------------------------------------
    // getItem – plain text content round-trip
    // ----------------------------------------------------------------

    @Test
    public void should_ReturnEntryWithMatchingPlainText_When_getItem_IsCalledAfterAddAll() {
        String label = "test plain";
        List<LineEntry> entries = new ArrayList<>();
        entries.add(createEntry(label));
        mLineEntries.addAll(entries);

        LineEntry retrieved = mLineEntries.getItem(0);
        assertNotNull("Retrieved entry must not be null", retrieved);
        assertEquals(
                "Plain text of retrieved entry must match the value used during construction",
                label,
                retrieved.getPlain()
        );
    }

    // ----------------------------------------------------------------
    // addAll + clear + addAll lifecycle
    // ----------------------------------------------------------------

    @Test
    public void should_ReturnNewCount_When_addAll_IsCalledAfterClear() {
        mLineEntries.addAll(buildEntryList(5));
        mLineEntries.clear();

        int newCount = 3;
        mLineEntries.addAll(buildEntryList(newCount));

        assertEquals(
                "After clear() and a second addAll(), getCount() must reflect only the new entries",
                newCount,
                mLineEntries.getCount()
        );
    }

    @Test
    public void should_ReturnZeroCount_When_clear_IsCalledImmediatelyAfterAddAllWithEmptyCollection() {
        mLineEntries.addAll(new ArrayList<>());
        mLineEntries.clear();
        assertEquals("Count must be 0 after clear() following addAll(empty)", 0, mLineEntries.getCount());
    }

    // ----------------------------------------------------------------
    // Thread-safety smoke test (single-threaded assertion)
    // ----------------------------------------------------------------

    @Test
    public void should_MaintainConsistentCount_When_OperationsAreChained() {
        // Simulates a typical use-case: open → clear → open again
        mLineEntries.addAll(buildEntryList(10));
        assertEquals(10, mLineEntries.getCount());

        mLineEntries.clear();
        assertEquals(0, mLineEntries.getCount());

        mLineEntries.addAll(buildEntryList(4));
        assertEquals(4, mLineEntries.getCount());
    }
}
