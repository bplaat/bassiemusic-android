package nl.plaatsoft.bassiemusic;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import java.security.MessageDigest;
import nl.plaatsoft.bassiemusic.Config;

public class Utils {
    private Utils() {}

    public static String md5(String data) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(data.getBytes());
            byte[] bytes = messageDigest.digest();
            StringBuilder hashBuilder = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                hashBuilder.append(String.format("%02x", bytes[i]));
            }
            return hashBuilder.toString();
        }
        catch (Exception exception) {
            Log.e(Config.LOG_TAG, "An exception catched!", exception);
            return null;
        }
    }

    public static String getStorePageUrl(Context context) {
        if (Config.APP_OVERRIDE_STORE_PAGE_URL != null) {
            return Config.APP_OVERRIDE_STORE_PAGE_URL;
        } else {
            return "https://play.google.com/store/apps/details?id=" + context.getPackageName();
        }
    }

    public static void openStorePage(Context context) {
        if (Config.APP_OVERRIDE_STORE_PAGE_URL != null) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Config.APP_OVERRIDE_STORE_PAGE_URL)));
        } else {
            String appPackageName = context.getPackageName();
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
            } catch (Exception exception) {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
            }
        }
    }
}
