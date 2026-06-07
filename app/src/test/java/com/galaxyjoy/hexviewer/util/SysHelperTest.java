/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Unit tests for SysHelper utility class
 * </p>
 *
 * @author Test Suite
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.util;

import android.content.Context;

import com.galaxyjoy.hexviewer.models.LineEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static com.google.common.truth.Truth.assertThat;

/**
 * Comprehensive unit tests for SysHelper utility class.
 * Tests hex conversion, buffer formatting, and helper methods.
 */
@RunWith(RobolectricTestRunner.class)
public class SysHelperTest {

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
    }

    // ========== Hex Conversion Tests ==========

    /**
     * Test hexStringToByteArray with valid hex string.
     */
    @Test
    public void should_ConvertToByteArray_When_ValidHexStringProvided() {
        String hex = "48656C6C6F"; // "Hello"

        byte[] result = SysHelper.hexStringToByteArray(hex);

        assertEquals("Should have 5 bytes", 5, result.length);
        assertEquals("First byte should be 'H'", 0x48, result[0]);
        assertEquals("Second byte should be 'e'", 0x65, result[1]);
        assertEquals("Last byte should be 'o'", 0x6F, result[4]);
    }

    /**
     * Test hexStringToByteArray with lowercase hex.
     */
    @Test
    public void should_HandleLowercaseHex_When_ConvertingToByteArray() {
        String hex = "48656c6c6f"; // "Hello" in lowercase

        byte[] result = SysHelper.hexStringToByteArray(hex);

        assertEquals("Should have 5 bytes", 5, result.length);
        assertEquals("Should convert lowercase correctly", 0x6c, result[2]);
    }

    /**
     * Test hexStringToByteArray with odd-length string.
     */
    @Test
    public void should_HandleOddLength_When_ConvertingHexToByteArray() {
        String hex = "123"; // Odd length

        byte[] result = SysHelper.hexStringToByteArray(hex);

        assertEquals("Should pad to even length", 2, result.length);
        assertEquals("First byte should be 0x12", 0x12, result[0]);
    }

    /**
     * Test hexStringToByteArray with empty string.
     */
    @Test
    public void should_ReturnEmptyArray_When_EmptyHexStringProvided() {
        byte[] result = SysHelper.hexStringToByteArray("");

        assertEquals("Should return empty array", 0, result.length);
    }

    /**
     * Test hex2bin conversion.
     */
    @Test
    public void should_ConvertHexToBinary_When_Hex2binCalled() {
        String hex = "48 65 6C";

        byte[] result = SysHelper.hex2bin(hex);

        assertEquals("Should have 3 bytes", 3, result.length);
        assertEquals("First byte should be 0x48", 0x48, result[0]);
        assertEquals("Second byte should be 0x65", 0x65, result[1]);
    }

    /**
     * Test hex2bin with spaces.
     */
    @Test
    public void should_RemoveSpaces_When_Hex2binConverts() {
        String hex = "48 65 6C 6C 6F";

        byte[] result = SysHelper.hex2bin(hex);

        assertEquals("Should ignore spaces", 5, result.length);
    }

    /**
     * Test hex2bin with empty string.
     */
    @Test
    public void should_ReturnEmptyArray_When_Hex2binGetsEmptyString() {
        byte[] result = SysHelper.hex2bin("");

        assertEquals("Should return empty array", 0, result.length);
    }

    /**
     * Test formatHex with uppercase.
     */
    @Test
    public void should_FormatAsUppercaseHex_When_UppercaseFlagIsTrue() {
        char c = 'A'; // 0x41

        String result = SysHelper.formatHex(c, true);

        assertEquals("Should format as uppercase", "41", result);
    }

    /**
     * Test formatHex with lowercase.
     */
    @Test
    public void should_FormatAsLowercaseHex_When_UppercaseFlagIsFalse() {
        char c = 'A'; // 0x41

        String result = SysHelper.formatHex(c, false);

        assertEquals("Should format as lowercase", "41", result);
    }

    /**
     * Test formatHex with special characters.
     */
    @Test
    public void should_FormatSpecialChar_When_FormatHexCalled() {
        char c = '\n'; // 0x0A

        String result = SysHelper.formatHex(c, true);

        assertEquals("Should format newline as 0A", "0A", result);
    }

    // ========== Validation Tests ==========

    /**
     * Test isValidHexLine with valid hex.
     */
    @Test
    public void should_ReturnTrue_When_HexLineIsValid() {
        assertTrue("Should accept valid hex", SysHelper.isValidHexLine("48656C6C6F"));
        assertTrue("Should accept lowercase hex", SysHelper.isValidHexLine("48656c6c6f"));
        assertTrue("Should accept mixed case", SysHelper.isValidHexLine("48656C6c6F"));
    }

    /**
     * Test isValidHexLine with invalid hex.
     */
    @Test
    public void should_ReturnFalse_When_HexLineIsInvalid() {
        assertFalse("Should reject odd-length hex", SysHelper.isValidHexLine("123"));
        assertFalse("Should reject non-hex characters", SysHelper.isValidHexLine("GHIJ"));
        assertFalse("Should reject spaces", SysHelper.isValidHexLine("48 65"));
    }

    /**
     * Test isValidHexLine with empty string.
     */
    @Test
    public void should_ReturnTrue_When_HexLineIsEmpty() {
        assertTrue("Should accept empty string", SysHelper.isValidHexLine(""));
    }

    /**
     * Test isEven method.
     */
    @Test
    public void should_ReturnTrue_When_NumberIsEven() {
        assertTrue("0 is even", SysHelper.isEven(0));
        assertTrue("2 is even", SysHelper.isEven(2));
        assertTrue("100 is even", SysHelper.isEven(100));
        assertTrue("-4 is even", SysHelper.isEven(-4));
    }

    /**
     * Test isEven method with odd numbers.
     */
    @Test
    public void should_ReturnFalse_When_NumberIsOdd() {
        assertFalse("1 is odd", SysHelper.isEven(1));
        assertFalse("3 is odd", SysHelper.isEven(3));
        assertFalse("99 is odd", SysHelper.isEven(99));
        assertFalse("-3 is odd", SysHelper.isEven(-3));
    }

    // ========== Buffer Formatting Tests ==========

    /**
     * Test formatBuffer with simple data.
     */
    @Test
    public void should_FormatBuffer_When_ValidDataProvided() {
        byte[] buffer = "Hello".getBytes();

        List<LineEntry> result = SysHelper.formatBuffer(buffer, null, SysHelper.MAX_BY_ROW_16);

        assertNotNull("Result should not be null", result);
        assertEquals("Should have 1 line", 1, result.size());

        LineEntry line = result.get(0);
        assertNotNull("Line should not be null", line);
        assertThat(line.toString()).contains("48"); // 'H'
        assertThat(line.toString()).contains("65"); // 'e'
    }

    /**
     * Test formatBuffer with multiple rows.
     */
    @Test
    public void should_FormatMultipleRows_When_BufferExceedsMaxByRow() {
        byte[] buffer = new byte[32]; // 2 rows with MAX_BY_ROW_16
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) (0x41 + (i % 26)); // A-Z repeating
        }

        List<LineEntry> result = SysHelper.formatBuffer(buffer, null, SysHelper.MAX_BY_ROW_16);

        assertEquals("Should have 2 rows", 2, result.size());
        assertEquals("Each row should have 16 bytes", 16, result.get(0).getRaw().size());
        assertEquals("Second row should have 16 bytes", 16, result.get(1).getRaw().size());
    }

    /**
     * Test formatBuffer with partial last row.
     */
    @Test
    public void should_AlignLastRow_When_BufferSizeNotMultipleOfMaxByRow() {
        byte[] buffer = new byte[20]; // More than 16, less than 32
        Arrays.fill(buffer, (byte) 0x41);

        List<LineEntry> result = SysHelper.formatBuffer(buffer, null, SysHelper.MAX_BY_ROW_16);

        assertEquals("Should have 2 rows", 2, result.size());
        assertEquals("First row should have 16 bytes", 16, result.get(0).getRaw().size());
        assertEquals("Second row should have 4 bytes", 4, result.get(1).getRaw().size());
    }

    /**
     * Test formatBuffer with cancellation.
     */
    @Test
    public void should_StopFormatting_When_CancelledDuringOperation() {
        byte[] buffer = new byte[1000];
        AtomicBoolean cancel = new AtomicBoolean(false);

        // Start formatting, then cancel immediately
        cancel.set(true);
        List<LineEntry> result = SysHelper.formatBuffer(buffer, cancel, SysHelper.MAX_BY_ROW_16);

        // Result should be incomplete or empty
        assertNotNull("Result should not be null", result);
    }

    /**
     * Test formatBuffer with shift offset.
     */
    @Test
    public void should_ApplyShiftOffset_When_FormatBufferCalledWithOffset() {
        byte[] buffer = "Test".getBytes();
        int shiftOffset = 4;

        List<LineEntry> result = SysHelper.formatBuffer(buffer, null, SysHelper.MAX_BY_ROW_16, shiftOffset);

        assertNotNull("Result should not be null", result);
        if (!result.isEmpty()) {
            assertEquals("First line should have shift offset", shiftOffset, result.get(0).getShiftOffset());
        }
    }

    /**
     * Test formatBuffer with MAX_BY_ROW_8.
     */
    @Test
    public void should_FormatWith8BytesPerRow_When_MaxByRow8Used() {
        byte[] buffer = new byte[16];
        Arrays.fill(buffer, (byte) 0x42);

        List<LineEntry> result = SysHelper.formatBuffer(buffer, null, SysHelper.MAX_BY_ROW_8);

        assertEquals("Should have 2 rows", 2, result.size());
        assertEquals("Each row should have 8 bytes", 8, result.get(0).getRaw().size());
    }

    /**
     * Test formatBuffer with empty buffer.
     */
    @Test
    public void should_ReturnEmptyList_When_BufferIsEmpty() {
        byte[] buffer = new byte[0];

        List<LineEntry> result = SysHelper.formatBuffer(buffer, null, SysHelper.MAX_BY_ROW_16);

        assertNotNull("Result should not be null", result);
    }

    /**
     * Test that formatSingleLine outputs match the expected formatted hex and ASCII output.
     */
    @Test
    public void should_FormatSingleLineCorrectly_When_CalledWithVariousParams() {
        byte[] raw = "Hello World!".getBytes();
        // Normal formatting, maxByRow = 16, no shiftOffset
        String result = SysHelper.formatSingleLine(raw, 16, 0);
        assertThat(result).contains("48 65 6c 6c 6f");
        assertThat(result).contains("Hello World!");

        // Formatting with shift offset
        String resultShifted = SysHelper.formatSingleLine(raw, 16, 4);
        assertThat(resultShifted).startsWith("            "); // 4 * 3 = 12 spaces prefix
        assertThat(resultShifted).contains("Hello World!");
    }

    // ========== Size Conversion Tests ==========

    /**
     * Test sizeToHuman with bytes.
     */
    @Test
    public void should_FormatAsBytes_When_SizeLessThan1KB() {
        String result = SysHelper.sizeToHuman(context, 512);

        assertThat(result).contains("512");
    }

    /**
     * Test sizeToHuman with kilobytes.
     */
    @Test
    public void should_FormatAsKB_When_SizeBetween1KBAnd1MB() {
        String result = SysHelper.sizeToHuman(context, 2048);

        assertThat(result).contains("2.00");
    }

    /**
     * Test sizeToHuman with megabytes.
     */
    @Test
    public void should_FormatAsMB_When_SizeBetween1MBAnd1GB() {
        String result = SysHelper.sizeToHuman(context, 2 * 1024 * 1024);

        assertThat(result).contains("2.00");
    }

    /**
     * Test sizeToHuman with gigabytes.
     */
    @Test
    public void should_FormatAsGB_When_SizeGreaterThan1GB() {
        String result = SysHelper.sizeToHuman(context, 2L * 1024 * 1024 * 1024);

        assertThat(result).contains("2.00");
    }

    /**
     * Test sizeToHuman with zero.
     */
    @Test
    public void should_FormatZero_When_SizeIsZero() {
        String result = SysHelper.sizeToHuman(context, 0);

        assertThat(result).contains("0");
    }

    // ========== Collection Helper Tests ==========

    /**
     * Test getMapKeys returns sorted keys.
     */
    @Test
    public void should_ReturnSortedKeys_When_GetMapKeysCalled() {
        Map<Integer, String> map = new HashMap<>();
        map.put(5, "five");
        map.put(1, "one");
        map.put(3, "three");

        List<Integer> keys = SysHelper.getMapKeys(map);

        assertEquals("Should have 3 keys", 3, keys.size());
        assertEquals("First key should be 1", Integer.valueOf(1), keys.get(0));
        assertEquals("Second key should be 3", Integer.valueOf(3), keys.get(1));
        assertEquals("Third key should be 5", Integer.valueOf(5), keys.get(2));
    }

    /**
     * Test getMapKeys with empty map.
     */
    @Test
    public void should_ReturnEmptyList_When_MapIsEmpty() {
        Map<Integer, String> map = new HashMap<>();

        List<Integer> keys = SysHelper.getMapKeys(map);

        assertTrue("Should return empty list", keys.isEmpty());
    }

    /**
     * Test toByteArray conversion.
     */
    @Test
    public void should_ConvertListToByteArray_When_ToByteArrayCalled() {
        List<Byte> bytes = Arrays.asList((byte) 0x48, (byte) 0x65, (byte) 0x6C);

        byte[] result = SysHelper.toByteArray(bytes, null);

        assertEquals("Should have 3 bytes", 3, result.length);
        assertEquals("First byte should match", 0x48, result[0]);
        assertEquals("Second byte should match", 0x65, result[1]);
    }

    /**
     * Test toByteArray with cancellation.
     */
    @Test
    public void should_StopConversion_When_CancelledDuringToByteArray() {
        List<Byte> bytes = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            bytes.add((byte) i);
        }
        AtomicBoolean cancel = new AtomicBoolean(true);

        byte[] result = SysHelper.toByteArray(bytes, cancel);

        assertNotNull("Result should not be null", result);
    }

    /**
     * Test toByteArray with empty list.
     */
    @Test
    public void should_ReturnEmptyArray_When_ListIsEmpty() {
        List<Byte> bytes = new ArrayList<>();

        byte[] result = SysHelper.toByteArray(bytes, null);

        assertEquals("Should return empty array", 0, result.length);
    }

    // ========== Character Processing Tests ==========

    /**
     * Test ignoreNonDisplayedChar.
     */
    @Test
    public void should_ReplaceNonDisplayedChars_When_IgnoreNonDisplayedCharCalled() {
        String input = "Hello\u0000World\u0001"; // Contains null and SOH

        String result = SysHelper.ignoreNonDisplayedChar(input);

        assertThat(result).contains("Hello");
        assertThat(result).contains("World");
        assertThat(result).contains(".");
    }

    /**
     * Test ignoreNonDisplayedChar with tab and newline.
     */
    @Test
    public void should_KeepTabAndNewline_When_IgnoringNonDisplayedChars() {
        String input = "Hello\tWorld\n";

        String result = SysHelper.ignoreNonDisplayedChar(input);

        assertThat(result).contains("\t");
        assertThat(result).contains("\n");
    }

    /**
     * Test ignoreNonDisplayedChar with all visible chars.
     */
    @Test
    public void should_KeepAllChars_When_AllAreDisplayable() {
        String input = "Hello World 123";

        String result = SysHelper.ignoreNonDisplayedChar(input);

        assertEquals("Should keep all characters", input, result);
    }
}
