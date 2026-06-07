package com.galaxyjoy.hexviewer.ui.act;

import android.app.Activity;
import android.content.Intent;
import android.view.MenuItem;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.fakes.RoboMenuItem;

import static org.junit.Assert.*;

/**
 * Robolectric widget and unit tests for {@link ActLineUpdate}.
 */
@RunWith(RobolectricTestRunner.class)
public class ActLineUpdateTest {

    @Before
    public void setUp() {
        // Reset static bridge variables before each test
        ActLineUpdate.sBridgeTexts = null;
        ActLineUpdate.sBridgeResultReferenceString = null;
        ActLineUpdate.sBridgeResultNewString = null;

        // Force configurations for consistent test environment
        com.galaxyjoy.hexviewer.MyApplication app =
                (com.galaxyjoy.hexviewer.MyApplication) ApplicationProvider.getApplicationContext();
        app.setSmartInput(false);
        app.setOverwrite(false);
    }

    @After
    public void tearDown() {
        // Clean up static bridge variables after each test
        ActLineUpdate.sBridgeTexts = null;
        ActLineUpdate.sBridgeResultReferenceString = null;
        ActLineUpdate.sBridgeResultNewString = null;
    }

    @Test
    public void testBridgeVariablesInIsolation() {
        byte[] testBytes = new byte[]{0x0A, 0x0B, 0x0C};
        ActLineUpdate.sBridgeTexts = testBytes;
        assertArrayEquals(testBytes, ActLineUpdate.sBridgeTexts);

        ActLineUpdate.sBridgeResultReferenceString = "0a0b0c";
        ActLineUpdate.sBridgeResultNewString = "0d0e0f";
        assertEquals("0a0b0c", ActLineUpdate.sBridgeResultReferenceString);
        assertEquals("0d0e0f", ActLineUpdate.sBridgeResultNewString);
    }

    @Test
    public void testActivityLaunchesWithBridgeTexts() {
        byte[] testBytes = new byte[]{0x10, 0x20, 0x30, 0x40};
        ActLineUpdate.sBridgeTexts = testBytes;

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ActLineUpdate.class);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_POSITION, 0);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_NB_LINES, 1);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_FILENAME, "test.bin");
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_CHANGE, false);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_SEQUENTIAL, false);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_SHIFT_OFFSET, 0);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_START_OFFSET, 0L);

        try (ActivityScenario<ActLineUpdate> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                assertNotNull(activity);
                assertNotNull(activity.findViewById(com.galaxyjoy.hexviewer.R.id.etInputHex));
                assertNotNull(activity.findViewById(com.galaxyjoy.hexviewer.R.id.tilInputHex));
            });
        }
    }

    @Test
    public void testActivityDoneFlow_setsBridgeResultsAndFinishes() {
        byte[] testBytes = new byte[]{0x11, 0x22, 0x33, 0x44};
        ActLineUpdate.sBridgeTexts = testBytes;

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ActLineUpdate.class);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_POSITION, 0);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_NB_LINES, 1);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_FILENAME, "test_done.bin");
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_CHANGE, false);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_SEQUENTIAL, false);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_SHIFT_OFFSET, 0);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_START_OFFSET, 0L);

        try (ActivityScenario<ActLineUpdate> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                com.google.android.material.textfield.TextInputEditText etInputHex =
                        activity.findViewById(com.galaxyjoy.hexviewer.R.id.etInputHex);
                etInputHex.setText("11223344");

                MenuItem doneMenuItem = new RoboMenuItem(com.galaxyjoy.hexviewer.R.id.menuActionDone);
                boolean handled = activity.onOptionsItemSelected(doneMenuItem);
                assertTrue(handled);

                // Verify static bridge variables are populated
                assertEquals("11223344", ActLineUpdate.sBridgeResultReferenceString);
                assertEquals("11223344", ActLineUpdate.sBridgeResultNewString);

                // Verify the activity is finishing or finished
                assertTrue(activity.isFinishing());
            });
        }
    }
}
