package com.galaxyjoy.hexviewer.ui.act;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.galaxyjoy.hexviewer.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ActHashCalculatorTest {

    private File tempTestFile;
    private Uri testFileUri;

    @Before
    public void setUp() throws Exception {
        Intents.init();

        // Create a real temp file so that ContentResolver can read it
        Context context = ApplicationProvider.getApplicationContext();
        tempTestFile = new File(context.getCacheDir(), "espresso_test.bin");
        try (FileOutputStream fos = new FileOutputStream(tempTestFile)) {
            fos.write("Hello, Espresso!".getBytes("UTF-8"));
        }
        testFileUri = Uri.fromFile(tempTestFile);
    }

    @After
    public void tearDown() {
        Intents.release();
        if (tempTestFile != null && tempTestFile.exists()) {
            tempTestFile.delete();
        }
    }

    @Test
    public void should_DisplayInitialViews_When_Launched() {
        try (ActivityScenario<ActHashCalculator> scenario = ActivityScenario.launch(ActHashCalculator.class)) {
            // Check toolbar and file selection card are visible
            onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
            onView(withId(R.id.cardFileSelection)).check(matches(isDisplayed()));

            // Progress indicator should be invisible/gone
            onView(withId(R.id.progressIndicator)).check(matches(withEffectiveVisibility(Visibility.INVISIBLE)));

            // tilCompare layout should be gone
            onView(withId(R.id.tilCompare)).check(matches(withEffectiveVisibility(Visibility.GONE)));
        }
    }

    @Test
    public void should_CalculateHashesAndShowCompareInput_When_FileSelected() {
        // Mock the file picker intent to return our test file URI
        Intent resultData = new Intent();
        resultData.setData(testFileUri);
        Instrumentation.ActivityResult result = new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData);
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(result);

        try (ActivityScenario<ActHashCalculator> scenario = ActivityScenario.launch(ActHashCalculator.class)) {
            // Click to trigger file picker
            onView(withId(R.id.cardFileSelection)).perform(click());

            // Check file name is updated (Uri.fromFile has name "espresso_test.bin")
            onView(withId(R.id.tvFileName)).check(matches(withText("espresso_test.bin")));

            // Wait/check progress bar is hidden now that calculation is done
            onView(withId(R.id.progressIndicator)).check(matches(withEffectiveVisibility(Visibility.INVISIBLE)));

            // Compare text input should now be visible
            onView(withId(R.id.tilCompare)).check(matches(isDisplayed()));

            // Verify comparison matches when typing correct MD5 hash of "Hello, Espresso!"
            // MD5 of "Hello, Espresso!" is: 332879b339454fe8fe1e973970776bc4 (in hex)
            String correctMd5 = "332879b339454fe8fe1e973970776bc4";
            onView(withId(R.id.etCompare)).perform(typeText(correctMd5));
            onView(withId(R.id.tvMatchResult)).check(matches(isDisplayed()));
            onView(withId(R.id.tvMatchResult)).check(matches(withText(R.string.hash_result_match)));

            // Test case-insensitivity (uppercase MD5 should also match)
            onView(withId(R.id.etCompare)).perform(clearText(), typeText(correctMd5.toUpperCase()));
            onView(withId(R.id.tvMatchResult)).check(matches(withText(R.string.hash_result_match)));

            // Test mismatch on incorrect hash
            onView(withId(R.id.etCompare)).perform(clearText(), typeText("1234567890abcdef1234567890abcdef"));
            onView(withId(R.id.tvMatchResult)).check(matches(withText(R.string.hash_result_mismatch)));

            // Test clearing the input makes comparison result gone
            onView(withId(R.id.etCompare)).perform(clearText());
            onView(withId(R.id.tvMatchResult)).check(matches(withEffectiveVisibility(Visibility.GONE)));
        }
    }
}
