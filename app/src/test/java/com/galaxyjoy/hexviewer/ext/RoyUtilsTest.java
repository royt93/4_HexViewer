package com.galaxyjoy.hexviewer.ext;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class RoyUtilsTest {

    @Mock
    private Context mockContext;

    @Mock
    private Activity mockActivity;

    @Mock
    private SharedPreferences mockPrefs;

    @Mock
    private SharedPreferences.Editor mockEditor;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockActivity.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)).thenReturn(mockPrefs);
        when(mockPrefs.edit()).thenReturn(mockEditor);
        when(mockEditor.putLong(anyString(), anyLong())).thenReturn(mockEditor);
    }

    // ========== Uri.encode behavior tests ==========

    @Test
    public void should_EncodeSpacesCorrectly_When_UriEncodeCalled() {
        String original = "SAIGON PHANTOM LABS";
        String encoded = Uri.encode(original);
        assertEquals("SAIGON%20PHANTOM%20LABS", encoded);
    }

    @Test
    public void should_LeaveUnchanged_When_NoSpecialCharsToEncode() {
        String original = "SimpleName123";
        String encoded = Uri.encode(original);
        assertEquals(original, encoded);
    }

    @Test
    public void should_EncodeSpecialCharacters_When_AmpersandAndOthersPresent() {
        String original = "a&b=c";
        String encoded = Uri.encode(original);
        assertEquals("a%26b%3Dc", encoded);
    }

    // ========== rateApp() tests ==========

    @Test
    public void should_LaunchMarketIntent_When_RateAppIsCalled() {
        RoyUtils.rateApp(mockContext, "com.galaxyjoy.hexviewer");

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mockContext).startActivity(intentCaptor.capture());

        Intent intent = intentCaptor.getValue();
        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertEquals("market://details?id=com.galaxyjoy.hexviewer", intent.getDataString());
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Test
    public void should_FallbackToWebIntent_When_MarketThrowsActivityNotFound() {
        doThrow(new ActivityNotFoundException())
                .when(mockContext).startActivity(argThat(intent -> 
                        intent.getDataString() != null && intent.getDataString().startsWith("market://")));

        RoyUtils.rateApp(mockContext, "com.galaxyjoy.hexviewer");

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mockContext, times(2)).startActivity(intentCaptor.capture());

        // The second captured intent must be the fallback browser intent
        Intent fallbackIntent = intentCaptor.getAllValues().get(1);
        assertEquals(Intent.ACTION_VIEW, fallbackIntent.getAction());
        assertEquals("https://play.google.com/store/apps/details?id=com.galaxyjoy.hexviewer", fallbackIntent.getDataString());
    }

    // ========== getMoreApps() tests ==========

    @Test
    public void should_LaunchMarketSearchIntent_When_GetMoreAppsIsCalled() {
        RoyUtils.getMoreApps(mockContext);

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mockContext).startActivity(intentCaptor.capture());

        Intent intent = intentCaptor.getValue();
        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertEquals("market://search?q=pub:SAIGON%20PHANTOM%20LABS", intent.getDataString());
    }

    @Test
    public void should_FallbackToWebSearch_When_MarketSearchThrowsActivityNotFound() {
        doThrow(new ActivityNotFoundException())
                .when(mockContext).startActivity(argThat(intent -> 
                        intent.getDataString() != null && intent.getDataString().startsWith("market://")));

        RoyUtils.getMoreApps(mockContext);

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mockContext, times(2)).startActivity(intentCaptor.capture());

        Intent fallbackIntent = intentCaptor.getAllValues().get(1);
        assertEquals(Intent.ACTION_VIEW, fallbackIntent.getAction());
        assertEquals("https://play.google.com/store/search?q=pub:SAIGON%20PHANTOM%20LABS", fallbackIntent.getDataString());
    }

    // ========== shareApp() tests ==========

    @Test
    public void should_LaunchChooserIntent_When_ShareAppIsCalled() {
        RoyUtils.shareApp(mockContext, "com.galaxyjoy.hexviewer");

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mockContext).startActivity(intentCaptor.capture());

        Intent chooserIntent = intentCaptor.getValue();
        assertEquals(Intent.ACTION_CHOOSER, chooserIntent.getAction());

        Intent shareIntent = chooserIntent.getParcelableExtra(Intent.EXTRA_INTENT);
        assertNotNull(shareIntent);
        assertEquals(Intent.ACTION_SEND, shareIntent.getAction());
        assertEquals("text/plain", shareIntent.getType());
        assertTrue(shareIntent.getStringExtra(Intent.EXTRA_TEXT).contains("com.galaxyjoy.hexviewer"));
    }

    // ========== openBrowser() tests ==========

    @Test
    public void should_LaunchBrowserIntent_When_OpenBrowserIsCalled() {
        RoyUtils.openBrowser(mockContext, "https://github.com");

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mockContext).startActivity(intentCaptor.capture());

        Intent intent = intentCaptor.getValue();
        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertEquals("https://github.com", intent.getDataString());
    }

    // ========== rateAppInApp() logic tests ==========

    @Test
    public void should_NotAttemptReview_When_DaysSinceLastReviewLessThan7AndNotForced() {
        Activity activity = org.robolectric.Robolectric.buildActivity(Activity.class).create().get();
        SharedPreferences prefs = activity.getSharedPreferences("app_preferences", Context.MODE_PRIVATE);

        long oldTime = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 3; // 3 days ago
        prefs.edit().putLong("last_review_time", oldTime).apply();

        RoyUtils.rateAppInApp(activity, false);

        // Value should NOT be updated (still oldTime)
        assertEquals(oldTime, prefs.getLong("last_review_time", 0L));
    }

    @Test
    public void should_AttemptReview_When_ForcedRateInAppIsTrue() {
        Activity activity = org.robolectric.Robolectric.buildActivity(Activity.class).create().get();
        SharedPreferences prefs = activity.getSharedPreferences("app_preferences", Context.MODE_PRIVATE);

        long oldTime = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 3; // 3 days ago
        prefs.edit().putLong("last_review_time", oldTime).apply();

        try (org.mockito.MockedStatic<com.google.android.play.core.review.ReviewManagerFactory> factory =
                     org.mockito.Mockito.mockStatic(com.google.android.play.core.review.ReviewManagerFactory.class)) {
            com.google.android.play.core.review.testing.FakeReviewManager fakeReviewManager =
                    new com.google.android.play.core.review.testing.FakeReviewManager(activity);
            factory.when(() -> com.google.android.play.core.review.ReviewManagerFactory.create(activity))
                    .thenReturn(fakeReviewManager);

            RoyUtils.rateAppInApp(activity, true);

            // Since the ReviewManager task callbacks execute asynchronously on the main thread,
            // we must idle the main looper to execute those pending callbacks.
            org.robolectric.shadows.ShadowLooper.idleMainLooper();

            // Since it's forced, the review flow will trigger. In Robolectric, the FakeReviewManager is used,
            // which succeeds and updates SharedPreferences last_review_time.
            assertTrue(prefs.getLong("last_review_time", 0L) > oldTime);
        }
    }
}
