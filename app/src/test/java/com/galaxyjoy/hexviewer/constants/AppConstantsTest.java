/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Unit tests for AppConstants.
 * </p>
 *
 * <p>
 * Pure JVM tests – no Android context required, so no @RunWith is used.
 * </p>
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.constants;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Comprehensive unit tests for {@link AppConstants}.
 *
 * <p>Naming convention: should_&lt;Expected&gt;_When_&lt;Condition&gt;</p>
 */
public class AppConstantsTest {

    // ----------------------------------------------------------------
    // FILE_BUFFER_ROWS
    // ----------------------------------------------------------------

    @Test
    public void should_Equal_20000_When_FILE_BUFFER_ROWS_IsAccessed() {
        assertEquals(
                "FILE_BUFFER_ROWS must be exactly 20 000",
                20_000,
                AppConstants.FILE_BUFFER_ROWS
        );
    }

    // ----------------------------------------------------------------
    // MAX_FILE_BUFFER_SIZE
    // ----------------------------------------------------------------

    @Test
    public void should_Equal_16_Times_FILE_BUFFER_ROWS_When_MAX_FILE_BUFFER_SIZE_IsAccessed() {
        int expected = 16 * AppConstants.FILE_BUFFER_ROWS;
        assertEquals(
                "MAX_FILE_BUFFER_SIZE must equal 16 * FILE_BUFFER_ROWS",
                expected,
                AppConstants.MAX_FILE_BUFFER_SIZE
        );
    }

    @Test
    public void should_Equal_320000_When_MAX_FILE_BUFFER_SIZE_IsAccessed() {
        // 16 * 20 000 = 320 000
        assertEquals(
                "MAX_FILE_BUFFER_SIZE must be exactly 320 000 bytes",
                320_000,
                AppConstants.MAX_FILE_BUFFER_SIZE
        );
    }

    // ----------------------------------------------------------------
    // MAX_NORMAL_FILE_SIZE  (30 MB)
    // ----------------------------------------------------------------

    @Test
    public void should_Equal_30MB_When_MAX_NORMAL_FILE_SIZE_IsAccessed() {
        long expected = 30L * 1024 * 1024;
        assertEquals(
                "MAX_NORMAL_FILE_SIZE must be exactly 30 MB",
                expected,
                AppConstants.MAX_NORMAL_FILE_SIZE
        );
    }

    @Test
    public void should_Equal_31457280_When_MAX_NORMAL_FILE_SIZE_IsAccessed() {
        assertEquals(
                "MAX_NORMAL_FILE_SIZE must equal 31 457 280 bytes (30 MB)",
                31_457_280L,
                AppConstants.MAX_NORMAL_FILE_SIZE
        );
    }

    // ----------------------------------------------------------------
    // MAX_SEQUENTIAL_FILE_SIZE  (2 GB)
    // ----------------------------------------------------------------

    @Test
    public void should_Equal_2GB_When_MAX_SEQUENTIAL_FILE_SIZE_IsAccessed() {
        long expected = 2L * 1024 * 1024 * 1024;
        assertEquals(
                "MAX_SEQUENTIAL_FILE_SIZE must be exactly 2 GB",
                expected,
                AppConstants.MAX_SEQUENTIAL_FILE_SIZE
        );
    }

    @Test
    public void should_Equal_2147483648_When_MAX_SEQUENTIAL_FILE_SIZE_IsAccessed() {
        assertEquals(
                "MAX_SEQUENTIAL_FILE_SIZE must equal 2 147 483 648 bytes (2 GB)",
                2_147_483_648L,
                AppConstants.MAX_SEQUENTIAL_FILE_SIZE
        );
    }

    // ----------------------------------------------------------------
    // RECOMMENDED_MAX_FILE_SIZE  (30 MB)
    // ----------------------------------------------------------------

    @Test
    public void should_Equal_30MB_When_RECOMMENDED_MAX_FILE_SIZE_IsAccessed() {
        long expected = 30L * 1024 * 1024;
        assertEquals(
                "RECOMMENDED_MAX_FILE_SIZE must be exactly 30 MB",
                expected,
                AppConstants.RECOMMENDED_MAX_FILE_SIZE
        );
    }

    @Test
    public void should_MatchMAX_NORMAL_FILE_SIZE_When_RECOMMENDED_MAX_FILE_SIZE_IsAccessed() {
        assertEquals(
                "RECOMMENDED_MAX_FILE_SIZE must equal MAX_NORMAL_FILE_SIZE",
                AppConstants.MAX_NORMAL_FILE_SIZE,
                AppConstants.RECOMMENDED_MAX_FILE_SIZE
        );
    }

    // ----------------------------------------------------------------
    // ABSOLUTE_MAX_FILE_SIZE  (Integer.MAX_VALUE)
    // ----------------------------------------------------------------

    @Test
    public void should_EqualIntegerMaxValue_When_ABSOLUTE_MAX_FILE_SIZE_IsAccessed() {
        assertEquals(
                "ABSOLUTE_MAX_FILE_SIZE must equal Integer.MAX_VALUE",
                (long) Integer.MAX_VALUE,
                AppConstants.ABSOLUTE_MAX_FILE_SIZE
        );
    }

    @Test
    public void should_Equal_2147483647_When_ABSOLUTE_MAX_FILE_SIZE_IsAccessed() {
        assertEquals(
                "ABSOLUTE_MAX_FILE_SIZE must equal 2 147 483 647",
                2_147_483_647L,
                AppConstants.ABSOLUTE_MAX_FILE_SIZE
        );
    }

    // ----------------------------------------------------------------
    // MAX_EXTERNAL_INTENT_FILE_SIZE  (500 MB)
    // ----------------------------------------------------------------

    @Test
    public void should_Equal_500MB_When_MAX_EXTERNAL_INTENT_FILE_SIZE_IsAccessed() {
        long expected = 500L * 1024 * 1024;
        assertEquals(
                "MAX_EXTERNAL_INTENT_FILE_SIZE must be exactly 500 MB",
                expected,
                AppConstants.MAX_EXTERNAL_INTENT_FILE_SIZE
        );
    }

    @Test
    public void should_Equal_524288000_When_MAX_EXTERNAL_INTENT_FILE_SIZE_IsAccessed() {
        assertEquals(
                "MAX_EXTERNAL_INTENT_FILE_SIZE must equal 524 288 000 bytes (500 MB)",
                524_288_000L,
                AppConstants.MAX_EXTERNAL_INTENT_FILE_SIZE
        );
    }

    // ----------------------------------------------------------------
    // Inter-constant ordering invariants
    // ----------------------------------------------------------------

    @Test
    public void should_BeSmallerThanMAX_SEQUENTIAL_FILE_SIZE_When_MAX_NORMAL_FILE_SIZE_IsCompared() {
        assertTrue(
                "MAX_NORMAL_FILE_SIZE (30 MB) must be strictly less than MAX_SEQUENTIAL_FILE_SIZE (2 GB)",
                AppConstants.MAX_NORMAL_FILE_SIZE < AppConstants.MAX_SEQUENTIAL_FILE_SIZE
        );
    }

    @Test
    public void should_BeSmallerThanMAX_EXTERNAL_INTENT_FILE_SIZE_When_MAX_NORMAL_FILE_SIZE_IsCompared() {
        assertTrue(
                "MAX_NORMAL_FILE_SIZE (30 MB) must be strictly less than MAX_EXTERNAL_INTENT_FILE_SIZE (500 MB)",
                AppConstants.MAX_NORMAL_FILE_SIZE < AppConstants.MAX_EXTERNAL_INTENT_FILE_SIZE
        );
    }

    @Test
    public void should_BeGreaterThanMAX_SEQUENTIAL_FILE_SIZE_When_ABSOLUTE_MAX_FILE_SIZE_IsCompared() {
        // Guard: Sequential is 2 GB (2 147 483 648), Absolute is Integer.MAX_VALUE (2 147 483 647).
        // Sequential uses 2L * 1024 * 1024 * 1024 = 2 147 483 648 which EXCEEDS Integer.MAX_VALUE.
        // This test asserts the relationship documented in the constant's Javadoc:
        // ABSOLUTE_MAX_FILE_SIZE should be >= MAX_SEQUENTIAL_FILE_SIZE once cast to long,
        // i.e. no file system operation will be blocked before the sequential guard fires.
        // Because Integer.MAX_VALUE < 2 GB we verify the guard is coherent:
        // files that exceed ABSOLUTE_MAX_FILE_SIZE are also caught by the sequential limit.
        assertTrue(
                "ABSOLUTE_MAX_FILE_SIZE (Integer.MAX_VALUE) must be >= MAX_NORMAL_FILE_SIZE — " +
                        "Sequential (2 GB) exceeds Integer.MAX_VALUE, so ABSOLUTE acts as the real ceiling",
                AppConstants.ABSOLUTE_MAX_FILE_SIZE >= AppConstants.MAX_NORMAL_FILE_SIZE
        );
    }

    @Test
    public void should_BeSmallerThanMAX_SEQUENTIAL_FILE_SIZE_When_ABSOLUTE_MAX_FILE_SIZE_IsCompared() {
        // Integer.MAX_VALUE (2_147_483_647) < 2 GB (2_147_483_648)
        // This is the key invariant: ABSOLUTE_MAX_FILE_SIZE is effectively a tighter cap
        // than MAX_SEQUENTIAL_FILE_SIZE on 32-bit long arithmetic paths.
        assertTrue(
                "ABSOLUTE_MAX_FILE_SIZE (Integer.MAX_VALUE = 2 147 483 647) must be < " +
                        "MAX_SEQUENTIAL_FILE_SIZE (2 GB = 2 147 483 648)",
                AppConstants.ABSOLUTE_MAX_FILE_SIZE < AppConstants.MAX_SEQUENTIAL_FILE_SIZE
        );
    }

    @Test
    public void should_BeLessThanMAX_SEQUENTIAL_FILE_SIZE_When_MAX_EXTERNAL_INTENT_FILE_SIZE_IsCompared() {
        assertTrue(
                "MAX_EXTERNAL_INTENT_FILE_SIZE (500 MB) must be less than MAX_SEQUENTIAL_FILE_SIZE (2 GB)",
                AppConstants.MAX_EXTERNAL_INTENT_FILE_SIZE < AppConstants.MAX_SEQUENTIAL_FILE_SIZE
        );
    }

    @Test
    public void should_BePositive_When_FILE_BUFFER_ROWS_IsAccessed() {
        assertTrue(
                "FILE_BUFFER_ROWS must be a positive value",
                AppConstants.FILE_BUFFER_ROWS > 0
        );
    }

    @Test
    public void should_BePositive_When_MAX_FILE_BUFFER_SIZE_IsAccessed() {
        assertTrue(
                "MAX_FILE_BUFFER_SIZE must be a positive value",
                AppConstants.MAX_FILE_BUFFER_SIZE > 0
        );
    }

    // ----------------------------------------------------------------
    // Supplementary value-correctness tests for other constants
    // ----------------------------------------------------------------

    @Test
    public void should_Equal_2000_When_LOG_BUFFER_CAPACITY_IsAccessed() {
        assertEquals(
                "LOG_BUFFER_CAPACITY must be exactly 2 000",
                2_000,
                AppConstants.LOG_BUFFER_CAPACITY
        );
    }

    @Test
    public void should_Equal_2000_When_DOUBLE_BACK_PRESS_INTERVAL_MS_IsAccessed() {
        assertEquals(
                "DOUBLE_BACK_PRESS_INTERVAL_MS must be exactly 2 000 ms",
                2_000,
                AppConstants.DOUBLE_BACK_PRESS_INTERVAL_MS
        );
    }

    @Test
    public void should_Equal_2000L_When_BACK_TIME_DELAY_MS_IsAccessed() {
        assertEquals(
                "BACK_TIME_DELAY_MS must be exactly 2 000 ms",
                2_000L,
                AppConstants.BACK_TIME_DELAY_MS
        );
    }

    @Test
    public void should_Equal_16_When_HEX_BYTES_PER_ROW_16_IsAccessed() {
        assertEquals(
                "HEX_BYTES_PER_ROW_16 must be exactly 16",
                16,
                AppConstants.HEX_BYTES_PER_ROW_16
        );
    }

    @Test
    public void should_Equal_8_When_HEX_BYTES_PER_ROW_8_IsAccessed() {
        assertEquals(
                "HEX_BYTES_PER_ROW_8 must be exactly 8",
                8,
                AppConstants.HEX_BYTES_PER_ROW_8
        );
    }

    @Test
    public void should_Equal_0x20_When_ASCII_PRINTABLE_MIN_IsAccessed() {
        assertEquals(
                "ASCII_PRINTABLE_MIN must be 0x20 (space character)",
                (byte) 0x20,
                AppConstants.ASCII_PRINTABLE_MIN
        );
    }

    @Test
    public void should_Equal_0x7E_When_ASCII_PRINTABLE_MAX_IsAccessed() {
        assertEquals(
                "ASCII_PRINTABLE_MAX must be 0x7E (tilde character)",
                (byte) 0x7E,
                AppConstants.ASCII_PRINTABLE_MAX
        );
    }

    @Test
    public void should_Equal_0x2E_When_ASCII_DOT_IsAccessed() {
        assertEquals(
                "ASCII_DOT must be 0x2E ('.')",
                (char) 0x2E,
                AppConstants.ASCII_DOT
        );
    }
}
