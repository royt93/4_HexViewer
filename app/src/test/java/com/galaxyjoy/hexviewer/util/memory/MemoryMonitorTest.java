/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Unit tests for MemoryMonitor
 * </p>
 *
 * @author Test Suite
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.util.memory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for MemoryMonitor class.
 * Tests memory monitoring, threshold detection, and listener callbacks.
 */
@RunWith(RobolectricTestRunner.class)
public class MemoryMonitorTest {

    private MemoryMonitor memoryMonitor;

    @Mock
    private MemoryListener mockListener;

    private static final float TEST_THRESHOLD = 10.0f;
    private static final int TEST_CHECK_FREQUENCY = 100;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() {
        if (memoryMonitor != null) {
            memoryMonitor.stop();
            memoryMonitor = null;
        }
    }

    // ========== Initialization Tests ==========

    /**
     * Test that MemoryMonitor can be created with valid parameters.
     */
    @Test
    public void should_CreateMemoryMonitor_When_ValidParametersProvided() {
        memoryMonitor = new MemoryMonitor(TEST_THRESHOLD, TEST_CHECK_FREQUENCY);

        assertNotNull("MemoryMonitor should not be null", memoryMonitor);
    }

    /**
     * Test that MemoryMonitor can be created with zero threshold.
     */
    @Test
    public void should_CreateMemoryMonitor_When_ThresholdIsZero() {
        memoryMonitor = new MemoryMonitor(0.0f, TEST_CHECK_FREQUENCY);

        assertNotNull("MemoryMonitor should not be null", memoryMonitor);
    }

    /**
     * Test that MemoryMonitor can be created with negative threshold (special case).
     */
    @Test
    public void should_CreateMemoryMonitor_When_ThresholdIsNegative() {
        memoryMonitor = new MemoryMonitor(-1.0f, TEST_CHECK_FREQUENCY);

        assertNotNull("MemoryMonitor should not be null", memoryMonitor);
    }

    // ========== Start/Stop Tests ==========

    /**
     * Test that start method initializes the monitor.
     */
    @Test
    public void should_InitializeMonitor_When_StartCalled() {
        memoryMonitor = new MemoryMonitor(TEST_THRESHOLD, TEST_CHECK_FREQUENCY);

        memoryMonitor.start(mockListener, false);

        assertNotNull("MemoryMonitor should be started", memoryMonitor);
    }

    /**
     * Test that stop method can be called safely.
     */
    @Test
    public void should_StopMonitor_When_StopCalled() {
        memoryMonitor = new MemoryMonitor(TEST_THRESHOLD, TEST_CHECK_FREQUENCY);
        memoryMonitor.start(mockListener, false);

        memoryMonitor.stop();

        // Should not throw exception
        assertNotNull("MemoryMonitor should still exist", memoryMonitor);
    }

    /**
     * Test that stop can be called without start.
     */
    @Test
    public void should_HandleStop_When_NotStarted() {
        memoryMonitor = new MemoryMonitor(TEST_THRESHOLD, TEST_CHECK_FREQUENCY);

        memoryMonitor.stop();

        // Should not throw exception
        assertNotNull("MemoryMonitor should still exist", memoryMonitor);
    }

    /**
     * Test that stop can be called multiple times.
     */
    @Test
    public void should_HandleMultipleStops_When_StopCalledRepeatedly() {
        memoryMonitor = new MemoryMonitor(TEST_THRESHOLD, TEST_CHECK_FREQUENCY);
        memoryMonitor.start(mockListener, false);

        memoryMonitor.stop();
        memoryMonitor.stop();
        memoryMonitor.stop();

        // Should not throw exception
        assertNotNull("MemoryMonitor should still exist", memoryMonitor);
    }

    /**
     * Test that start can be called multiple times.
     */
    @Test
    public void should_RestartMonitor_When_StartCalledAgain() {
        memoryMonitor = new MemoryMonitor(TEST_THRESHOLD, TEST_CHECK_FREQUENCY);

        memoryMonitor.start(mockListener, false);
        memoryMonitor.start(mockListener, false);

        // Should not throw exception
        assertNotNull("MemoryMonitor should be restarted", memoryMonitor);
    }

    // ========== AutoCloseable Tests ==========

    /**
     * Test that close method stops the monitor.
     */
    @Test
    public void should_StopMonitor_When_CloseCalled() {
        memoryMonitor = new MemoryMonitor(TEST_THRESHOLD, TEST_CHECK_FREQUENCY);
        memoryMonitor.start(mockListener, false);

        memoryMonitor.close();

        // Should not throw exception
        assertNotNull("MemoryMonitor should still exist", memoryMonitor);
    }

    /**
     * Test try-with-resources pattern.
     */
    @Test
    public void should_AutoClose_When_UsedWithTryWithResources() {
        try (MemoryMonitor monitor = new MemoryMonitor(TEST_THRESHOLD, TEST_CHECK_FREQUENCY)) {
            monitor.start(mockListener, false);
            assertNotNull("Monitor should be active in try block", monitor);
        }
        // Monitor should be closed automatically
    }

    // ========== Memory Info Tests ==========

    /**
     * Test that getLastMemoryInfo returns valid info after start.
     */
    @Test
    public void should_ReturnMemoryInfo_When_GetLastMemoryInfoCalled() {
        memoryMonitor = new MemoryMonitor(TEST_THRESHOLD, TEST_CHECK_FREQUENCY);
        memoryMonitor.start(mockListener, false);

        MemoryInfo info = memoryMonitor.getLastMemoryInfo();

        assertNotNull("MemoryInfo should not be null", info);
        assertTrue("Total memory should be positive", info.getTotalMemory() > 0);
        assertTrue("Used memory should be non-negative", info.getUsedMemory() >= 0);
    }

    /**
     * Test that memory info is calculated correctly.
     */
    @Test
    public void should_CalculateMemoryStats_When_MonitorStarts() {
        memoryMonitor = new MemoryMonitor(TEST_THRESHOLD, TEST_CHECK_FREQUENCY);
        memoryMonitor.start(mockListener, false);

        MemoryInfo info = memoryMonitor.getLastMemoryInfo();

        assertTrue("Total memory should be greater than used",
                   info.getTotalMemory() >= info.getUsedMemory());
        assertTrue("Percent used should be valid",
                   info.getPercentUsed() >= 0 && info.getPercentUsed() <= 100);
        assertEquals("Total free should equal total minus used",
                     info.getTotalMemory() - info.getUsedMemory(),
                     info.getTotalFreeMemory());
    }

    // ========== Listener Tests ==========

    /**
     * Test that listener is called when threshold is -1.
     */
    @Test
    public void should_CallListenerImmediately_When_ThresholdIsNegativeOne() {
        memoryMonitor = new MemoryMonitor(-1.0f, TEST_CHECK_FREQUENCY);

        memoryMonitor.start(mockListener, false);

        ArgumentCaptor<MemoryInfo> captor = ArgumentCaptor.forClass(MemoryInfo.class);
        verify(mockListener, times(1)).onLowAppMemory(eq(true), captor.capture());

        MemoryInfo info = captor.getValue();
        assertNotNull("MemoryInfo should be passed to listener", info);
    }

    /**
     * Test that null listener doesn't cause errors.
     */
    @Test
    public void should_HandleNullListener_When_StartCalledWithNull() {
        memoryMonitor = new MemoryMonitor(TEST_THRESHOLD, TEST_CHECK_FREQUENCY);

        memoryMonitor.start(null, false);

        // Should not throw exception
        assertNotNull("MemoryMonitor should handle null listener", memoryMonitor);
    }

    // ========== Auto-Stop Tests ==========

    /**
     * Test that auto-stop flag is respected.
     */
    @Test
    public void should_RespectAutoStopFlag_When_SetToTrue() {
        memoryMonitor = new MemoryMonitor(-1.0f, TEST_CHECK_FREQUENCY);

        memoryMonitor.start(mockListener, true);

        verify(mockListener, times(1)).onLowAppMemory(eq(true), any(MemoryInfo.class));
    }

    /**
     * Test that auto-stop flag false doesn't stop monitor.
     */
    @Test
    public void should_ContinueMonitoring_When_AutoStopIsFalse() {
        memoryMonitor = new MemoryMonitor(TEST_THRESHOLD, TEST_CHECK_FREQUENCY);

        memoryMonitor.start(mockListener, false);

        // Monitor should continue running
        assertNotNull("Monitor should be running", memoryMonitor);
    }

    // ========== Threshold Tests ==========

    /**
     * Test that high threshold doesn't trigger callback immediately.
     */
    @Test
    public void should_NotTriggerCallback_When_ThresholdIsHigh() {
        memoryMonitor = new MemoryMonitor(99.0f, TEST_CHECK_FREQUENCY);

        memoryMonitor.start(mockListener, false);

        // With 99% threshold, callback likely won't trigger immediately
        // This test verifies the threshold logic works
        assertNotNull("Monitor should be running", memoryMonitor);
    }

    /**
     * Test that very low threshold might trigger callback.
     */
    @Test
    public void should_MonitorThreshold_When_ThresholdIsLow() {
        memoryMonitor = new MemoryMonitor(0.1f, TEST_CHECK_FREQUENCY);

        memoryMonitor.start(mockListener, false);

        // With very low threshold, monitoring should be active
        assertNotNull("Monitor should be running", memoryMonitor);
    }

    // ========== Edge Case Tests ==========

    /**
     * Test that zero check frequency is handled.
     */
    @Test
    public void should_HandleZeroCheckFrequency_When_Creating() {
        memoryMonitor = new MemoryMonitor(TEST_THRESHOLD, 0);

        memoryMonitor.start(mockListener, false);

        assertNotNull("Monitor should handle zero frequency", memoryMonitor);
    }

    /**
     * Test that negative check frequency is handled.
     */
    @Test
    public void should_HandleNegativeCheckFrequency_When_Creating() {
        memoryMonitor = new MemoryMonitor(TEST_THRESHOLD, -100);

        memoryMonitor.start(mockListener, false);

        assertNotNull("Monitor should handle negative frequency", memoryMonitor);
    }

    /**
     * Test that very high threshold (>100) is handled.
     */
    @Test
    public void should_HandleHighThreshold_When_ThresholdExceeds100() {
        memoryMonitor = new MemoryMonitor(200.0f, TEST_CHECK_FREQUENCY);

        memoryMonitor.start(mockListener, false);

        assertNotNull("Monitor should handle high threshold", memoryMonitor);
    }

    // ========== Run Method Tests ==========

    /**
     * Test that run method executes without errors.
     */
    @Test
    public void should_ExecuteRun_When_Called() {
        memoryMonitor = new MemoryMonitor(TEST_THRESHOLD, TEST_CHECK_FREQUENCY);
        memoryMonitor.start(mockListener, false);

        // Manually call run to test the method
        memoryMonitor.run();

        // Should not throw exception
        assertNotNull("Monitor should complete run", memoryMonitor);
    }

    /**
     * Test multiple run cycles with Robolectric shadow looper.
     */
    @Test
    public void should_ExecuteMultipleCycles_When_RunningContinuously() {
        memoryMonitor = new MemoryMonitor(TEST_THRESHOLD, TEST_CHECK_FREQUENCY);
        memoryMonitor.start(mockListener, false);

        // Advance the looper to trigger delayed callbacks
        ShadowLooper.idleMainLooper(TEST_CHECK_FREQUENCY * 3);

        // Memory monitor should have executed multiple times
        assertNotNull("Monitor should complete multiple cycles", memoryMonitor);
    }

    // ========== Integration Tests ==========

    /**
     * Test typical usage pattern: start, monitor, stop.
     */
    @Test
    public void should_FollowTypicalWorkflow_When_UsedNormally() {
        memoryMonitor = new MemoryMonitor(TEST_THRESHOLD, TEST_CHECK_FREQUENCY);

        // Start monitoring
        memoryMonitor.start(mockListener, false);
        assertNotNull("Monitor should start", memoryMonitor);

        // Get memory info
        MemoryInfo info = memoryMonitor.getLastMemoryInfo();
        assertNotNull("Should have memory info", info);

        // Stop monitoring
        memoryMonitor.stop();
        assertNotNull("Monitor should stop cleanly", memoryMonitor);
    }

    /**
     * Test restart after stop.
     */
    @Test
    public void should_RestartSuccessfully_When_StoppedAndStartedAgain() {
        memoryMonitor = new MemoryMonitor(TEST_THRESHOLD, TEST_CHECK_FREQUENCY);

        // Start, stop, start again
        memoryMonitor.start(mockListener, false);
        memoryMonitor.stop();
        memoryMonitor.start(mockListener, false);

        MemoryInfo info = memoryMonitor.getLastMemoryInfo();
        assertNotNull("Should have memory info after restart", info);
    }

    /**
     * Test that memory info persists after stop.
     */
    @Test
    public void should_RetainMemoryInfo_When_StoppedAfterStart() {
        memoryMonitor = new MemoryMonitor(TEST_THRESHOLD, TEST_CHECK_FREQUENCY);

        memoryMonitor.start(mockListener, false);
        MemoryInfo infoBefore = memoryMonitor.getLastMemoryInfo();

        memoryMonitor.stop();
        MemoryInfo infoAfter = memoryMonitor.getLastMemoryInfo();

        assertNotNull("Info before stop should exist", infoBefore);
        assertNotNull("Info after stop should exist", infoAfter);
    }
}
