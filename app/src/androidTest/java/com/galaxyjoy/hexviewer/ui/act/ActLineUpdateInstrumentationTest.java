package com.galaxyjoy.hexviewer.ui.act;

import android.app.Activity;
import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.galaxyjoy.hexviewer.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Espresso integration/instrumented tests for {@link ActLineUpdate}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ActLineUpdateInstrumentationTest {

    @Before
    public void setUp() {
        ActLineUpdate.sBridgeTexts = null;
        ActLineUpdate.sBridgeResultReferenceString = null;
        ActLineUpdate.sBridgeResultNewString = null;

        // Force configurations for consistent test environment
        com.galaxyjoy.hexviewer.MyApplication app =
                (com.galaxyjoy.hexviewer.MyApplication) ApplicationProvider.getApplicationContext();
        app.setSmartInput(true);
        app.setOverwrite(false);
    }

    @After
    public void tearDown() {
        ActLineUpdate.sBridgeTexts = null;
        ActLineUpdate.sBridgeResultReferenceString = null;
        ActLineUpdate.sBridgeResultNewString = null;
    }

    @Test
    public void testLineUpdateIntegrationFlow() {
        byte[] testBytes = new byte[]{0x11, 0x22, 0x33, 0x44};
        ActLineUpdate.sBridgeTexts = testBytes;

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ActLineUpdate.class);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_POSITION, 0);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_NB_LINES, 1);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_FILENAME, "integration_test.bin");
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_CHANGE, false);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_SEQUENTIAL, false);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_SHIFT_OFFSET, 0);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_START_OFFSET, 0L);

        try (ActivityScenario<ActLineUpdate> scenario = ActivityScenario.launchActivityForResult(intent)) {
            // Verify input field exists and is displayed
            onView(withId(R.id.tilInputHex)).check(matches(isDisplayed()));

            // Clear existing contents and type new hex
            onView(withId(R.id.etInputHex)).perform(clearText(), typeText("aabbccdd"));

            // Verify TextWatcher automatically formats input since smartInput is enabled
            // (ASCII characters 'a','a','b','b','c','c','d','d' are formatted to their hex values '61 61 62 62 63 63 64 64')
            onView(withId(R.id.etInputHex)).check(matches(withText("61 61 62 62 63 63 64 64")));

            // Trigger "Done" action
            onView(withId(R.id.menuActionDone)).perform(click());

            // Check that the output is set in the bridge and the activity closes successfully
            assertEquals("6161626263636464", ActLineUpdate.sBridgeResultNewString);
            assertEquals(Activity.RESULT_OK, scenario.getResult().getResultCode());
        }
    }
}
