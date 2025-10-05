/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Unit tests for LineEntry model
 * </p>
 *
 * @author Test Suite
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.models;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static com.google.common.truth.Truth.assertThat;

/**
 * Comprehensive unit tests for LineEntry class.
 * Tests all getter/setter methods, constructors, and edge cases.
 */
@RunWith(RobolectricTestRunner.class)
public class LineEntryTest {

    private LineEntry lineEntry;
    private String testPlain;
    private List<Byte> testRaw;

    @Before
    public void setUp() {
        testPlain = "48 65 6C 6C 6F  Hello";
        testRaw = new ArrayList<>();
        testRaw.add((byte) 0x48);
        testRaw.add((byte) 0x65);
        testRaw.add((byte) 0x6C);
        testRaw.add((byte) 0x6C);
        testRaw.add((byte) 0x6F);
        lineEntry = new LineEntry(testPlain, testRaw);
    }

    /**
     * Test that LineEntry can be created with valid plain text and raw bytes.
     */
    @Test
    public void should_CreateLineEntry_When_ProvidedValidData() {
        assertNotNull("LineEntry should not be null", lineEntry);
        assertEquals("Plain text should match", testPlain, lineEntry.getPlain());
        assertEquals("Raw bytes should match", testRaw, lineEntry.getRaw());
    }

    /**
     * Test that LineEntry copy constructor creates a deep copy.
     */
    @Test
    public void should_CreateDeepCopy_When_UsingCopyConstructor() {
        lineEntry.setIndex(5);
        lineEntry.setUpdated(true);
        lineEntry.setShiftOffset(2);

        LineEntry copy = new LineEntry(lineEntry);

        assertEquals("Plain should be copied", lineEntry.getPlain(), copy.getPlain());
        assertEquals("Index should be copied", lineEntry.getIndex(), copy.getIndex());
        assertEquals("Updated flag should be copied", lineEntry.isUpdated(), copy.isUpdated());
        assertEquals("Shift offset should be copied", lineEntry.getShiftOffset(), copy.getShiftOffset());

        // Verify it's a deep copy by modifying the original
        List<Byte> originalRaw = lineEntry.getRaw();
        originalRaw.add((byte) 0x21); // Add exclamation mark

        assertThat(copy.getRaw()).hasSize(5);
        assertThat(lineEntry.getRaw()).hasSize(6);
    }

    /**
     * Test that getRaw returns the correct byte list.
     */
    @Test
    public void should_ReturnCorrectRawBytes_When_GetRawIsCalled() {
        List<Byte> raw = lineEntry.getRaw();

        assertNotNull("Raw bytes should not be null", raw);
        assertEquals("Should have 5 bytes", 5, raw.size());
        assertEquals("First byte should be 'H'", (byte) 0x48, (byte) raw.get(0));
        assertEquals("Last byte should be 'o'", (byte) 0x6F, (byte) raw.get(4));
    }

    /**
     * Test that getPlain returns the correct plain text.
     */
    @Test
    public void should_ReturnCorrectPlainText_When_GetPlainIsCalled() {
        String plain = lineEntry.getPlain();

        assertNotNull("Plain text should not be null", plain);
        assertEquals("Plain text should match", testPlain, plain);
    }

    /**
     * Test that toString returns the plain text representation.
     */
    @Test
    public void should_ReturnPlainText_When_ToStringIsCalled() {
        String result = lineEntry.toString();

        assertEquals("toString should return plain text", testPlain, result);
    }

    /**
     * Test that setValues updates both plain and raw data.
     */
    @Test
    public void should_UpdateBothPlainAndRaw_When_SetValuesIsCalled() {
        String newPlain = "41 42  AB";
        List<Byte> newRaw = Arrays.asList((byte) 0x41, (byte) 0x42);

        lineEntry.setValues(newPlain, newRaw);

        assertEquals("Plain should be updated", newPlain, lineEntry.getPlain());
        assertEquals("Raw should be updated", newRaw.size(), lineEntry.getRaw().size());
        assertEquals("First raw byte should match", (byte) 0x41, (byte) lineEntry.getRaw().get(0));
    }

    /**
     * Test that setValues creates a copy of the raw list (not a reference).
     */
    @Test
    public void should_CreateCopyOfRawList_When_SetValuesIsCalled() {
        List<Byte> newRaw = new ArrayList<>(Arrays.asList((byte) 0x41, (byte) 0x42));

        lineEntry.setValues("Test", newRaw);

        // Modify the original list
        newRaw.add((byte) 0x43);

        // LineEntry's raw should not be affected
        assertEquals("Raw list should be a copy", 2, lineEntry.getRaw().size());
    }

    /**
     * Test index getter and setter.
     */
    @Test
    public void should_StoreAndRetrieveIndex_When_SetIndexIsCalled() {
        assertEquals("Default index should be 0", 0, lineEntry.getIndex());

        lineEntry.setIndex(42);

        assertEquals("Index should be updated", 42, lineEntry.getIndex());
    }

    /**
     * Test updated flag getter and setter.
     */
    @Test
    public void should_StoreAndRetrieveUpdatedFlag_When_SetUpdatedIsCalled() {
        assertFalse("Default updated flag should be false", lineEntry.isUpdated());

        lineEntry.setUpdated(true);

        assertTrue("Updated flag should be true", lineEntry.isUpdated());

        lineEntry.setUpdated(false);

        assertFalse("Updated flag should be false", lineEntry.isUpdated());
    }

    /**
     * Test shift offset getter and setter.
     */
    @Test
    public void should_StoreAndRetrieveShiftOffset_When_SetShiftOffsetIsCalled() {
        assertEquals("Default shift offset should be 0", 0, lineEntry.getShiftOffset());

        lineEntry.setShiftOffset(8);

        assertEquals("Shift offset should be updated", 8, lineEntry.getShiftOffset());
    }

    /**
     * Test LineEntry with empty plain text.
     */
    @Test
    public void should_HandleEmptyPlainText_When_CreatedWithEmptyString() {
        LineEntry empty = new LineEntry("", new ArrayList<>());

        assertEquals("Plain should be empty", "", empty.getPlain());
        assertEquals("Raw should be empty", 0, empty.getRaw().size());
        assertEquals("toString should be empty", "", empty.toString());
    }

    /**
     * Test LineEntry with null plain text.
     */
    @Test
    public void should_HandleNullPlainText_When_CreatedWithNull() {
        LineEntry nullEntry = new LineEntry(null, new ArrayList<>());

        assertNull("Plain should be null", nullEntry.getPlain());
        assertEquals("Raw should be empty", 0, nullEntry.getRaw().size());
    }

    /**
     * Test LineEntry with null raw bytes.
     */
    @Test
    public void should_HandleNullRawBytes_When_CreatedWithNull() {
        LineEntry nullRaw = new LineEntry("Test", null);

        assertEquals("Plain should be set", "Test", nullRaw.getPlain());
        assertNull("Raw should be null", nullRaw.getRaw());
    }

    /**
     * Test that multiple LineEntry instances are independent.
     */
    @Test
    public void should_MaintainIndependence_When_MultipleInstancesExist() {
        LineEntry entry1 = new LineEntry("Entry 1", Arrays.asList((byte) 0x01));
        LineEntry entry2 = new LineEntry("Entry 2", Arrays.asList((byte) 0x02));

        entry1.setIndex(10);
        entry1.setUpdated(true);

        assertEquals("Entry1 index should be 10", 10, entry1.getIndex());
        assertEquals("Entry2 index should be 0", 0, entry2.getIndex());
        assertTrue("Entry1 should be updated", entry1.isUpdated());
        assertFalse("Entry2 should not be updated", entry2.isUpdated());
    }

    /**
     * Test negative index values.
     */
    @Test
    public void should_AcceptNegativeIndex_When_SetIndexCalled() {
        lineEntry.setIndex(-5);

        assertEquals("Index should accept negative values", -5, lineEntry.getIndex());
    }

    /**
     * Test large index values.
     */
    @Test
    public void should_AcceptLargeIndex_When_SetIndexCalled() {
        lineEntry.setIndex(Integer.MAX_VALUE);

        assertEquals("Index should accept large values", Integer.MAX_VALUE, lineEntry.getIndex());
    }

    /**
     * Test that shift offset can handle various values.
     */
    @Test
    public void should_HandleVariousShiftOffsets_When_SetShiftOffsetCalled() {
        int[] offsets = {0, 1, 8, 16, 100, -5};

        for (int offset : offsets) {
            lineEntry.setShiftOffset(offset);
            assertEquals("Shift offset should be " + offset, offset, lineEntry.getShiftOffset());
        }
    }
}
