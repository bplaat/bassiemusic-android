package nl.plaatsoft.bassiemusic;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SettingsActivity extends BaseActivity {
    public static final int LANGUAGE_DEFAULT = 2;
    public static final int THEME_DEFAULT = 2;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ((ImageView)findViewById(R.id.settings_back_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                finish();
            }
        });

        SharedPreferences settings = getSharedPreferences("settings", Context.MODE_PRIVATE);

        String[] languages = getResources().getStringArray(R.array.languages);
        int language = settings.getInt("language", SettingsActivity.LANGUAGE_DEFAULT);
        ((TextView)findViewById(R.id.settings_language_label)).setText(languages[language]);

        ((LinearLayout)findViewById(R.id.settings_language_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle(getResources().getString(R.string.settings_language))
                    .setSingleChoiceItems(languages, language, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences.Editor settingsEditor = settings.edit();
                            settingsEditor.putInt("language", which);
                            settingsEditor.apply();

                            dialog.dismiss();
                            recreate();
                        }
                    })
                    .setNegativeButton(getResources().getString(R.string.settings_cancel), null)
                    .show();
            }
        });

        String[] themes = getResources().getStringArray(R.array.themes);
        int theme = settings.getInt("theme", SettingsActivity.THEME_DEFAULT);
        ((TextView)findViewById(R.id.settings_theme_label)).setText(themes[theme]);

        ((LinearLayout)findViewById(R.id.settings_theme_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle(getResources().getString(R.string.settings_theme))
                    .setSingleChoiceItems(themes, theme, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences.Editor settingsEditor = settings.edit();
                            settingsEditor.putInt("theme", which);
                            settingsEditor.apply();

                            dialog.dismiss();
                            recreate();
                        }
                    })
                    .setNegativeButton(getResources().getString(R.string.settings_cancel), null)
                    .show();
            }
        });

        ((TextView)findViewById(R.id.settings_about_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://bastiaan.ml/")));
            }
        });
    }
}
