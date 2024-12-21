package com.galaxyjoy.hexviewer.ext;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.google.android.play.core.review.ReviewException;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.review.model.ReviewErrorCode;

import java.util.Calendar;

public class RoyUtils {
    /**
     * Opens the Google Play Store to rate the app based on the package name.
     *
     * @param context     The context from which the method is called.
     * @param packageName The package name of the app to be rated.
     */
    public static void rateApp(Context context, String packageName) {
        try {
            // Mở ứng dụng trên Google Play
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            // Nếu Google Play không có, mở trên trình duyệt web
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            // Thông báo lỗi nếu có vấn đề phát sinh
            Toast.makeText(context, "Unable to open the rating page.", Toast.LENGTH_SHORT).show();
        }
    }

    public static void rateAppInApp(Activity activity, boolean forceRateInApp) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences("app_preferences", Context.MODE_PRIVATE);
        long lastReviewTime = sharedPreferences.getLong("last_review_time", 0L);
        Log.d("roy93~", "requestReview lastReviewTime " + lastReviewTime);

        long currentTime = Calendar.getInstance().getTimeInMillis();
        long daysSinceLastReview = (currentTime - lastReviewTime) / (1000 * 60 * 60 * 24);

        Log.d("roy93~", "requestReview forceRateInApp " + forceRateInApp);
        Log.d("roy93~", "requestReview daysSinceLastReview " + daysSinceLastReview);

        if (daysSinceLastReview >= 7 || forceRateInApp) {
            ReviewManager reviewManager = ReviewManagerFactory.create(activity);
            reviewManager.requestReviewFlow().addOnCompleteListener(task -> {
                try {
                    if (task.isSuccessful()) {
                        ReviewInfo reviewInfo = task.getResult();
                        reviewManager.launchReviewFlow(activity, reviewInfo);
                        sharedPreferences.edit().putLong("last_review_time", currentTime).apply();

                        Log.d("roy93~", "requestReview result " + task.getResult());
                        Log.d("roy93~", "requestReview isSuccessful " + task.isSuccessful());
                        Log.d("roy93~", "requestReview isCanceled " + task.isCanceled());
                        Log.d("roy93~", "requestReview isComplete " + task.isComplete());
                        Log.d("roy93~", "requestReview exception " + task.getException());
                    } else {
                        Log.d("roy93~", "requestReview exception " + task.getException());
                    }
                } catch (Exception e) {
                    Log.e("roy93~", "requestReview e " + e);
                }
            });
        }
    }

    public static void getMoreApps(Context context) {
        try {
            // Mở trang của nhà phát triển McKimQuyen trên Google Play
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pub:SAIGON PHANTOM LABS"));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            // Nếu Google Play không có, mở trên trình duyệt web
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=pub:SAIGON PHANTOM LABS"));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            // Thông báo lỗi nếu có vấn đề phát sinh
            Toast.makeText(context, "Unable to open the developer's page.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shares the app link with a catchy message using the system's share dialog.
     *
     * @param context     The context from which the method is called.
     * @param packageName The package name of the app to be shared.
     */
    public static void shareApp(Context context, String packageName) {
        try {
            String shareMessage = "✨ Check out this amazing app! ✨\n" + "I think you’ll love it as much as I do. " + "Download it now on Google Play: https://play.google.com/store/apps/details?id=" + packageName;

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Awesome app alert!");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);

            context.startActivity(Intent.createChooser(shareIntent, "Share this app via"));
        } catch (Exception e) {
            // Show an error message if something goes wrong
            Toast.makeText(context, "Unable to share the app.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Opens a browser with the provided URL.
     *
     * @param context The context from which the method is called.
     * @param url     The URL to be opened in the browser.
     */
    public static void openBrowser(Context context, String url) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(browserIntent);
        } catch (Exception e) {
            // Show an error message if something goes wrong
            Toast.makeText(context, "Unable to open the link.", Toast.LENGTH_SHORT).show();
        }
    }
}