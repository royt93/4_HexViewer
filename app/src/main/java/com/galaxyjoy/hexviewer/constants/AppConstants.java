/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Application-wide constants
 * </p>
 *
 * @author Keidan
 * <p>
 * License: GPLv3
 * </p>
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.constants;

/**
 * Central location for all app-wide constants
 * Replaces magic numbers throughout the codebase for better maintainability
 */
public final class AppConstants {

    private AppConstants() {
        // Utility class - prevent instantiation
    }

    // ============================
    // File I/O Constants
    // ============================

    /**
     * Number of rows to buffer when reading files
     * Used in TaskOpen to limit memory usage for large files
     */
    public static final int FILE_BUFFER_ROWS = 20_000;

    /**
     * Maximum file buffer size in bytes
     * Calculated as: MAX_BY_ROW_16 (16 bytes) * FILE_BUFFER_ROWS
     */
    public static final int MAX_FILE_BUFFER_SIZE = 16 * FILE_BUFFER_ROWS;

    /**
     * Maximum file size for files opened via external intents (500 MB)
     * Prevents malicious apps from opening huge files that could cause OOM
     */
    public static final long MAX_EXTERNAL_INTENT_FILE_SIZE = 500L * 1024 * 1024;

    /**
     * Maximum file size for normal (full) open mode (30 MB)
     * Files larger than this must be opened in sequential (partial) mode to prevent OOM
     */
    public static final long MAX_NORMAL_FILE_SIZE = 30L * 1024 * 1024;

    /**
     * Maximum file size for sequential access mode (partial open) (2 GB)
     */
    public static final long MAX_SEQUENTIAL_FILE_SIZE = 2L * 1024 * 1024 * 1024;

    /**
     * Recommended maximum file size for normal operation (30 MB)
     * Files larger than this should be opened in sequential (partial) mode
     */
    public static final long RECOMMENDED_MAX_FILE_SIZE = 30L * 1024 * 1024;

    /**
     * Absolute maximum file size (4 GB - 1 byte)
     * Android file system limit for most operations
     */
    public static final long ABSOLUTE_MAX_FILE_SIZE = Integer.MAX_VALUE;

    // ============================
    // Logging Constants
    // ============================

    /**
     * Maximum number of log entries to keep in circular buffer
     * Prevents unbounded memory growth while maintaining recent logs
     */
    public static final int LOG_BUFFER_CAPACITY = 2_000;

    // ============================
    // UI Constants
    // ============================

    /**
     * Double back press interval in milliseconds for exit confirmation
     * User must press back twice within this interval to exit app
     */
    public static final int DOUBLE_BACK_PRESS_INTERVAL_MS = 2_000;

    /**
     * Delay in milliseconds before showing "Press back again to exit" message
     */
    public static final long BACK_TIME_DELAY_MS = 2_000L;

    // ============================
    // Memory Management Constants
    // ============================

    /**
     * Cooldown period after low memory event (15 minutes)
     * Prevents frequent memory checks that could impact performance
     */
    public static final long LOW_MEMORY_COOLDOWN_MS = 15 * 60 * 1000L;

    // ============================
    // Hex Display Constants
    // ============================

    /**
     * Maximum bytes to display per row in 16-byte mode
     */
    public static final int HEX_BYTES_PER_ROW_16 = 16;

    /**
     * Maximum bytes to display per row in 8-byte mode
     */
    public static final int HEX_BYTES_PER_ROW_8 = 8;

    // ============================
    // Permission Request Codes
    // ============================

    /**
     * Request code for storage permissions
     */
    public static final int REQUEST_CODE_STORAGE_PERMISSION = 1;

    /**
     * Request code for file picker
     */
    public static final int REQUEST_CODE_PICK_FILE = 100;

    // ============================
    // ASCII Character Constants
    // ============================

    /**
     * ASCII printable range minimum (space character)
     */
    public static final byte ASCII_PRINTABLE_MIN = 0x20;

    /**
     * ASCII printable range maximum (tilde character ~)
     */
    public static final byte ASCII_PRINTABLE_MAX = 0x7E;

    /**
     * ASCII dot character (.) - used as placeholder for non-printable chars
     */
    public static final char ASCII_DOT = 0x2E;
}
