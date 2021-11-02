package nl.plaatsoft.bassiemusic.activities;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PowerManager;
import java.util.Locale;
import nl.plaatsoft.bassiemusic.Config;

public abstract class BaseActivity extends Activity {
    protected SharedPreferences settings;

    @Override
    public void attachBaseContext(Context context) {
        settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE);

        int language = settings.getInt("language", Config.SETTINGS_LANGUAGE_DEFAULT);
        int theme = settings.getInt("theme", Config.SETTINGS_THEME_DEFAULT);

        if (
            language != Config.SETTINGS_LANGUAGE_SYSTEM ||
            theme != Config.SETTINGS_THEME_SYSTEM ||
            (theme == Config.SETTINGS_THEME_SYSTEM && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
        ) {
            Configuration configuration = new Configuration(context.getResources().getConfiguration());

            if (language == Config.SETTINGS_LANGUAGE_ENGLISH) {
                configuration.setLocale(new Locale("en"));
            }

            if (language == Config.SETTINGS_LANGUAGE_DUTCH) {
                configuration.setLocale(new Locale("nl"));
            }

            if (theme == Config.SETTINGS_THEME_LIGHT) {
                configuration.uiMode |= Configuration.UI_MODE_NIGHT_NO;
                configuration.uiMode &= ~Configuration.UI_MODE_NIGHT_YES;
            }

            if (theme == Config.SETTINGS_THEME_DARK) {
                configuration.uiMode |= Configuration.UI_MODE_NIGHT_YES;
                configuration.uiMode &= ~Configuration.UI_MODE_NIGHT_NO;
            }

            if (theme == Config.SETTINGS_THEME_SYSTEM && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
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
