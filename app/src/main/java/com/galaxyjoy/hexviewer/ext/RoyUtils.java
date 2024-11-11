package com.galaxyjoy.hexviewer.ext;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

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
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + packageName));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            // Nếu Google Play không có, mở trên trình duyệt web
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            // Thông báo lỗi nếu có vấn đề phát sinh
            Toast.makeText(context, "Unable to open the rating page.", Toast.LENGTH_SHORT).show();
        }
    }

    public static void getMoreApps(Context context) {
        try {
            // Mở trang của nhà phát triển McKimQuyen trên Google Play
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://search?q=pub:McKimQuyen"));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            // Nếu Google Play không có, mở trên trình duyệt web
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/search?q=pub:McKimQuyen"));
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
            String shareMessage = "✨ Check out this amazing app! ✨\n"
                    + "I think you’ll love it as much as I do. "
                    + "Download it now on Google Play: https://play.google.com/store/apps/details?id=" + packageName;

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
}