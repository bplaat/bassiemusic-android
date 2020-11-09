package nl.plaatsoft.bassiemusic;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

public class RatingAlert {
    private static final int LAUNCHES_UNTIL_PROMPT = 5;
    private static final int TIME_UNTIL_PROMPT = 2 * 24 * 60 * 60 * 1000;

    // Check and show rating dialog after some time
    public static void check(Context context) {
        // Get settings
        SharedPreferences settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE);

        // Check if the rating is hidden for ever
        if (settings.getBoolean("rating_hidden", false)) {
            return;
        }

        SharedPreferences.Editor settingsEditor = settings.edit();

        // Increment launch counter
        int launchCount = settings.getInt("rating_launch_count", 0) + 1;
        settingsEditor.putInt("rating_launch_count", launchCount);

        // Set date of first launch when no set
        long firstLaunchTime = settings.getLong("rating_first_launch_time", 0);
        if (firstLaunchTime == 0) {
            firstLaunchTime = System.currentTimeMillis();
            settingsEditor.putLong("rating_first_launch_time", firstLaunchTime);
        }

        settingsEditor.commit();

        // Wait at least n days before opening
        if (launchCount >= RatingAlert.LAUNCHES_UNTIL_PROMPT) {
            if (System.currentTimeMillis() - firstLaunchTime >= RatingAlert.TIME_UNTIL_PROMPT) {
                showRatingAlert(context);
            }
        }
    }

    // Show rating dialog
    public static void showRatingAlert(Context context) {
        // Get settings
        SharedPreferences settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE);

        // Show rating alert
        new AlertDialog.Builder(context)
            .setTitle(R.string.rating_alert_title)
            .setMessage(R.string.rating_alert_message_label)
            .setPositiveButton(R.string.rating_alert_rate_button, (DialogInterface dialog, int whichButton) -> {
                // Open the store page for the user to add a rating
                String appPackageName = context.getPackageName();
                try {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (Exception exception) {
                    exception.printStackTrace();

                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }

                // Set the rating hidden flag
                SharedPreferences.Editor settingsEditor = settings.edit();
                settingsEditor.putBoolean("rating_hidden", true);
                settingsEditor.commit();
            })
            .setNeutralButton(R.string.rating_alert_later_button, (DialogInterface dialog, int whichButton) -> {
                // Reset the rating counters
                SharedPreferences.Editor settingsEditor = settings.edit();
                settingsEditor.putInt("rating_launch_count", 0);
                settingsEditor.putLong("rating_first_launch_time", System.currentTimeMillis());
                settingsEditor.commit();
            })
            .setNegativeButton(R.string.rating_alert_never_button, (DialogInterface dialog, int whichButton)-> {
                // Set the rating hidden flag
                SharedPreferences.Editor settingsEditor = settings.edit();
                settingsEditor.putBoolean("rating_hidden", true);
                settingsEditor.commit();
            })
            .show();
    }
}
