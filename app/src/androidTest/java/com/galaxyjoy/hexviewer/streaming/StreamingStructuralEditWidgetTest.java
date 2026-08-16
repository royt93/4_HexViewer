package com.galaxyjoy.hexviewer.streaming;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.galaxyjoy.hexviewer.MyApplication;
import com.galaxyjoy.hexviewer.R;
import com.galaxyjoy.hexviewer.ui.act.ActLineUpdate;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/** Streaming edits are fixed-length: insertion and deletion must be rejected in the editor UI. */
@RunWith(AndroidJUnit4.class)
public class StreamingStructuralEditWidgetTest {
    @Before
    public void setUp() {
        ActLineUpdate.sBridgeTexts = null;
        ActLineUpdate.sBridgeResultReferenceString = null;
        ActLineUpdate.sBridgeResultNewString = null;
        MyApplication app = (MyApplication) ApplicationProvider.getApplicationContext();
        app.setSmartInput(false);
        app.setOverwrite(true);
    }

    @After
    public void tearDown() {
        ActLineUpdate.sBridgeTexts = null;
        ActLineUpdate.sBridgeResultReferenceString = null;
        ActLineUpdate.sBridgeResultNewString = null;
    }

    @Test
    public void streamingEditor_rejectsInsertion() {
        assertLengthChangeRejected("11 22 33 44 55");
    }

    @Test
    public void streamingEditor_rejectsDeletion() {
        assertLengthChangeRejected("11 22 33");
    }

    private static void assertLengthChangeRejected(String replacement) {
        ActLineUpdate.sBridgeTexts = new byte[]{0x11, 0x22, 0x33, 0x44};
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ActLineUpdate.class);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_POSITION, 0);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_NB_LINES, 1);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_FILENAME, "streaming-edit.bin");
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_CHANGE, false);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_SEQUENTIAL, true);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_SHIFT_OFFSET, 0);
        intent.putExtra(ActLineUpdate.ACTIVITY_EXTRA_START_OFFSET, 0L);

        try (ActivityScenario<ActLineUpdate> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.etInputHex)).perform(
                    clearText(), replaceText(replacement), closeSoftKeyboard());
            onView(withId(R.id.menuActionDone)).perform(click());
            onView(withText(R.string.error_open_sequential_add_or_delete_data))
                    .check(matches(isDisplayed()));
        }
    }
}
