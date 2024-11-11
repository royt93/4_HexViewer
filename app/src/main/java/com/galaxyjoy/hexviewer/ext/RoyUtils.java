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
}