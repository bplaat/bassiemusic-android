package nl.plaatsoft.bassiemusic;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PowerManager;
import java.util.Locale;

public abstract class BaseActivity extends Activity {
    protected SharedPreferences settings;

    public void attachBaseContext(Context context) {
        settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE);

        int language = settings.getInt("language", SettingsActivity.LANGUAGE_DEFAULT);
        int theme = settings.getInt("theme", SettingsActivity.THEME_DEFAULT);

        if (language != SettingsActivity.LANGUAGE_DEFAULT || theme != SettingsActivity.THEME_DEFAULT) {
            Configuration configuration = new Configuration(context.getResources().getConfiguration());

            if (language == 0) {
                configuration.setLocale(new Locale("en"));
            }

            if (language == 1) {
                configuration.setLocale(new Locale("nl"));
            }

            if (theme == 0) {
                configuration.uiMode |= Configuration.UI_MODE_NIGHT_NO;
                configuration.uiMode &= ~Configuration.UI_MODE_NIGHT_YES;
            }

            if (theme == 1) {
                configuration.uiMode |= Configuration.UI_MODE_NIGHT_YES;
                configuration.uiMode &= ~Configuration.UI_MODE_NIGHT_NO;
            }

            if (theme == 2 && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (((PowerManager)context.getSystemService(Context.POWER_SERVICE)).isPowerSaveMode()) {
                    configuration.uiMode |= Configuration.UI_MODE_NIGHT_YES;
                    configuration.uiMode &= ~Configuration.UI_MODE_NIGHT_NO;
                } else {
                    configuration.uiMode |= Configuration.UI_MODE_NIGHT_NO;
                    configuration.uiMode &= ~Configuration.UI_MODE_NIGHT_YES;
                }
            }

            super.attachBaseContext(context.createConfigurationContext(configuration));
            return;
        }

        super.attachBaseContext(context);
    }
}
