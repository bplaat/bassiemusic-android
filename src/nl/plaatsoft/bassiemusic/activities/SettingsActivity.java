package nl.plaatsoft.bassiemusic.activities;

import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import nl.plaatsoft.bassiemusic.Config;
import nl.plaatsoft.bassiemusic.Utils;
import nl.plaatsoft.bassiemusic.R;

public class SettingsActivity extends BaseActivity {
    @SuppressWarnings("deprecation")
    static private String getVersionName(PackageManager pm) throws PackageManager.NameNotFoundException {
        String packageName = "nl.plaatsoft.bassiemusic";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).versionName;
        }
        return pm.getPackageInfo(packageName, 0).versionName;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ((ImageButton)findViewById(R.id.settings_back_button)).setOnClickListener((View view) -> {
            finish();
        });

        Resources resources = getResources();

        // Init remember music button
        Switch rememberMusicSwitch = (Switch)findViewById(R.id.settings_remember_music_switch);
        rememberMusicSwitch.setChecked(settings.getBoolean("remember_music", Config.SETTINGS_REMEMBER_MUSIC_DEFAULT));
        rememberMusicSwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            SharedPreferences.Editor settingsEditor = settings.edit();
            settingsEditor.putBoolean("remember_music", isChecked);
            if (!isChecked) {
                settingsEditor.remove("playing_music_id");
                settingsEditor.remove("playing_music_position");
            }
            settingsEditor.apply();
        });

        ((LinearLayout)findViewById(R.id.settings_remember_music_button)).setOnClickListener((View view) -> {
            rememberMusicSwitch.toggle();
        });

        // Init language switcher button
        String[] languages = resources.getStringArray(R.array.settings_languages);
        int language = settings.getInt("language", Config.SETTINGS_LANGUAGE_DEFAULT);
        ((TextView)findViewById(R.id.settings_language_label)).setText(languages[language]);

        ((LinearLayout)findViewById(R.id.settings_language_button)).setOnClickListener((View view) -> {
            new AlertDialog.Builder(this)
                .setTitle(R.string.settings_language_alert_title_label)
                .setSingleChoiceItems(languages, language, (DialogInterface dialog, int which) -> {
                    dialog.dismiss();
                    if (language != which) {
                        SharedPreferences.Editor settingsEditor = settings.edit();
                        settingsEditor.putInt("language", which);
                        settingsEditor.apply();
                        recreate();
                    }
                })
                .setNegativeButton(R.string.settings_language_alert_cancel_button, null)
                .show();
        });

        // Init themes switcher button
        String[] themes = resources.getStringArray(R.array.settings_themes);
        int theme = settings.getInt("theme", Config.SETTINGS_THEME_DEFAULT);
        ((TextView)findViewById(R.id.settings_theme_label)).setText(themes[theme]);

        ((LinearLayout)findViewById(R.id.settings_theme_button)).setOnClickListener((View view) -> {
            new AlertDialog.Builder(this)
                .setTitle(R.string.settings_theme_alert_title_label)
                .setSingleChoiceItems(themes, theme, (DialogInterface dialog, int which) ->  {
                    dialog.dismiss();
                    if (theme != which) {
                        SharedPreferences.Editor settingsEditor = settings.edit();
                        settingsEditor.putInt("theme", which);
                        settingsEditor.apply();
                        recreate();
                    }
                })
                .setNegativeButton(R.string.settings_theme_alert_cancel_button, null)
                .show();
        });

        // Init fast scroll button
        Switch fastScrollSwitch = (Switch)findViewById(R.id.settings_fast_scroll_switch);
        fastScrollSwitch.setChecked(settings.getBoolean("fast_scroll", Config.SETTINGS_FAST_SCROLL_DEFAULT));
        fastScrollSwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            SharedPreferences.Editor settingsEditor = settings.edit();
            settingsEditor.putBoolean("fast_scroll", isChecked);
            settingsEditor.apply();
        });

        ((LinearLayout)findViewById(R.id.settings_fast_scroll_button)).setOnClickListener((View view) -> {
            fastScrollSwitch.toggle();
        });

        // Init version button easter egg
        try {
            ((TextView)findViewById(R.id.settings_version_label)).setText("v" + getVersionName(getPackageManager()));
        } catch (Exception exception) {
            Log.e(Config.LOG_TAG, "An exception catched!", exception);
        }

        int versionButtonClickCounterHolder[] = { 0 };
        ((LinearLayout)findViewById(R.id.settings_version_button)).setOnClickListener((View view) -> {
            versionButtonClickCounterHolder[0]++;
            if (versionButtonClickCounterHolder[0] == 8) {
                versionButtonClickCounterHolder[0] = 0;
                Toast.makeText(this, R.string.settings_version_message, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://youtu.be/dQw4w9WgXcQ?t=43")));
            }
        });

        // Init rate button
        ((LinearLayout)findViewById(R.id.settings_rate_button)).setOnClickListener((View view) -> {
            Utils.openStorePage(this);
        });

        // Init share button
        ((LinearLayout)findViewById(R.id.settings_share_button)).setOnClickListener((View view) -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, resources.getString(R.string.settings_share_message) + " " + Utils.getStorePageUrl(this));
            startActivity(Intent.createChooser(intent, null));
        });

        // Init about button
        ((LinearLayout)findViewById(R.id.settings_about_button)).setOnClickListener((View view) -> {
            new AlertDialog.Builder(this)
                .setTitle(R.string.settings_about_alert_title_label)
                .setMessage(R.string.settings_about_alert_message_label)
                .setNegativeButton(R.string.settings_about_alert_website_button, (DialogInterface dialog, int which) ->  {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Config.SETTINGS_ABOUT_WEBSITE_URL)));
                })
                .setPositiveButton(R.string.settings_about_alert_ok_button, null)
                .show();
        });

        // Init footer button
        ((TextView)findViewById(R.id.settings_footer_button)).setOnClickListener((View view) -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Config.SETTINGS_ABOUT_WEBSITE_URL)));
        });
    }
}
