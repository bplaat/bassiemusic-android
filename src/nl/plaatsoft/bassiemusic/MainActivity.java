package nl.plaatsoft.bassiemusic;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextSwitcher;
import android.widget.SeekBar;
import android.Manifest;
import java.util.List;

public class MainActivity extends BaseActivity {
    public static final int SEARCH_ACTIVITY_REQUEST_CODE = 1;
    public static final int SETTINGS_ACTIVITY_REQUEST_CODE = 2;
    public static final int STORAGE_PERMISSION_REQUEST_CODE = 1;

    private int oldLanguage = -1;
    private int oldTheme = -1;

    private LinearLayout musicPage;
    private ImageButton musicPlayButton;
    private ListView musicList;
    private MusicAdapter musicAdapter;
    private LinearLayout emptyPage;
    private LinearLayout accessPage;

    private PowerManager.WakeLock wakeLock;
    private MediaPlayer mediaPlayer;
    private Handler handler;
    private Runnable syncPlayer;

    private List<Music> music;
    private int playingPosition;
    private boolean requestAutoplay;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Pages
        musicPage = (LinearLayout)findViewById(R.id.main_music_page);

        emptyPage = (LinearLayout)findViewById(R.id.main_empty_page);

        accessPage = (LinearLayout)findViewById(R.id.main_access_page);

        // Wakelock
        wakeLock = ((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BassieMusic::WakeLock");

        // Music page
        musicPlayButton = (ImageButton)findViewById(R.id.main_music_play_button);

        musicList = (ListView)findViewById(R.id.main_music_list);

        musicAdapter = new MusicAdapter(this);
        musicList.setAdapter(musicAdapter);

        musicList.setOnItemClickListener((AdapterView<?> adapterView, View view, int position, long id) -> {
            startMusic(position, true);
        });

        ((ImageButton)findViewById(R.id.main_music_shuffle_button)).setOnClickListener((View view) -> {
            startMusic((int)(Math.random() * musicAdapter.getCount()), true);
        });

        ((ImageButton)findViewById(R.id.main_music_search_button)).setOnClickListener((View view) -> {
            startActivityForResult(new Intent(this, SearchActivity.class), MainActivity.SEARCH_ACTIVITY_REQUEST_CODE);
        });

        ((ImageButton)findViewById(R.id.main_music_settings_button)).setOnClickListener((View view) -> {
            oldLanguage = settings.getInt("language", Config.SETTINGS_LANGUAGE_DEFAULT);
            oldTheme = settings.getInt("theme", Config.SETTINGS_THEME_DEFAULT);
            startActivityForResult(new Intent(this, SettingsActivity.class), MainActivity.SETTINGS_ACTIVITY_REQUEST_CODE);
        });

        // Music player
        TextSwitcher musicTimeCurrentLabel = (TextSwitcher)findViewById(R.id.main_music_time_current_label);
        TextSwitcher musicTimeUntilLabel = (TextSwitcher)findViewById(R.id.main_music_time_until_label);
        SeekBar musicSeekBar = (SeekBar)findViewById(R.id.main_music_seekbar);

        handler = new Handler(Looper.getMainLooper());

        syncPlayer = () -> {
            musicTimeCurrentLabel.setCurrentText(Music.formatDuration(mediaPlayer.getCurrentPosition()));

            musicTimeUntilLabel.setCurrentText("-" + Music.formatDuration(mediaPlayer.getDuration() - mediaPlayer.getCurrentPosition()));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                musicSeekBar.setProgress(mediaPlayer.getCurrentPosition(), true);
            } else {
                musicSeekBar.setProgress(mediaPlayer.getCurrentPosition());
            }

            handler.postDelayed(syncPlayer, Config.MUSIC_SEEKBAR_UPDATE_TIMEOUT);
        };

        ((LinearLayout)findViewById(R.id.main_music_info_button)).setOnClickListener((View view) -> {
            scrollMusic(playingPosition);
        });

        ((ImageButton)findViewById(R.id.main_music_previous_button)).setOnClickListener((View view) -> {
            if (mediaPlayer.getCurrentPosition() > Config.MUSIC_PREVIOUS_RESET_TIMEOUT) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mediaPlayer.seekTo(0, MediaPlayer.SEEK_CLOSEST_SYNC);
                } else {
                    mediaPlayer.seekTo(0);
                }

                if (!mediaPlayer.isPlaying()) {
                    playMusic(false);
                } else {
                    handler.removeCallbacks(syncPlayer);
                    handler.post(syncPlayer);
                }
            } else {
                startMusic(playingPosition == 0 ? musicAdapter.getCount() - 1 : playingPosition - 1, true);
            }
        });

        ((ImageButton)findViewById(R.id.main_music_seek_back_button)).setOnClickListener((View view) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mediaPlayer.seekTo(Math.max(mediaPlayer.getCurrentPosition() - Config.MUSIC_SEEK_SKIP_TIME, 0), MediaPlayer.SEEK_CLOSEST_SYNC);
            } else {
                mediaPlayer.seekTo(Math.max(mediaPlayer.getCurrentPosition() - Config.MUSIC_SEEK_SKIP_TIME, 0));
            }

            if (!mediaPlayer.isPlaying()) {
                playMusic(false);
            } else {
                handler.removeCallbacks(syncPlayer);
                handler.post(syncPlayer);
            }
        });

        musicPlayButton.setOnClickListener((View view) -> {
            if (mediaPlayer.isPlaying()) {
                pauseMusic();
            } else {
                playMusic(false);
            }
        });

        ((ImageButton)findViewById(R.id.main_music_seek_forward_button)).setOnClickListener((View view) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mediaPlayer.seekTo(Math.min(mediaPlayer.getCurrentPosition() + Config.MUSIC_SEEK_SKIP_TIME, mediaPlayer.getDuration()), MediaPlayer.SEEK_CLOSEST_SYNC);
            } else {
                mediaPlayer.seekTo(Math.min(mediaPlayer.getCurrentPosition() + Config.MUSIC_SEEK_SKIP_TIME, mediaPlayer.getDuration()));
            }

            if (!mediaPlayer.isPlaying()) {
                playMusic(false);
            } else {
                handler.removeCallbacks(syncPlayer);
                handler.post(syncPlayer);
            }
        });

        ((ImageButton)findViewById(R.id.main_music_next_button)).setOnClickListener((View view) -> {
            startMusic(playingPosition == musicAdapter.getCount() - 1 ? 0 : playingPosition + 1, true);
        });

        musicSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(syncPlayer);
            }

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    musicTimeCurrentLabel.setCurrentText(Music.formatDuration(progress));
                    musicTimeUntilLabel.setCurrentText("-" + Music.formatDuration(mediaPlayer.getDuration() - progress));
                }
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mediaPlayer.seekTo(seekBar.getProgress(), MediaPlayer.SEEK_CLOSEST_SYNC);
                } else {
                    mediaPlayer.seekTo(seekBar.getProgress());
                }

                if (!mediaPlayer.isPlaying()) {
                    playMusic(true);
                } else {
                    handler.postDelayed(syncPlayer, Config.MUSIC_SEEKBAR_UPDATE_TIMEOUT);
                }
            }
        });

        // Media player
        mediaPlayer = new MediaPlayer();

        TextSwitcher musicPlayerTitle = (TextSwitcher)findViewById(R.id.main_music_title_label);
        TextSwitcher musicPlayerDuration = (TextSwitcher)findViewById(R.id.main_music_duration_label);

        mediaPlayer.setOnPreparedListener((MediaPlayer mediaPlayer) -> {
            Music music = musicAdapter.getItem(playingPosition);

            musicPlayerTitle.setText(music.getTitle());
            musicPlayerTitle.setSelected(true);

            musicPlayerDuration.setText(Music.formatDuration(music.getDuration()));

            musicTimeCurrentLabel.setText(Music.formatDuration(mediaPlayer.getCurrentPosition()));

            musicTimeUntilLabel.setText("-" + Music.formatDuration(mediaPlayer.getDuration() - mediaPlayer.getCurrentPosition()));

            musicSeekBar.setMax(mediaPlayer.getDuration());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                musicSeekBar.setProgress(0, true);
            } else {
                musicSeekBar.setProgress(0);
            }

            // Start media player when autoplay
            if (requestAutoplay) {
                playMusic(true);
            } else {
                pauseMusic();
            }
        });

        mediaPlayer.setOnCompletionListener((MediaPlayer mediaPlayer) -> {
            startMusic(playingPosition == musicAdapter.getCount() - 1 ? 0 : playingPosition + 1, true);
        });

        // Empty page
        View.OnClickListener refreshOnClick = (View view) -> {
            musicPage.setVisibility(View.VISIBLE);
            musicAdapter.setSelectedPosition(-1);
            emptyPage.setVisibility(View.GONE);

            if (mediaPlayer.isPlaying()) {
                pauseMusic();
            }

            loadMusic(false);
        };
        ((ImageButton)findViewById(R.id.main_music_refresh_button)).setOnClickListener(refreshOnClick);
        ((ImageButton)findViewById(R.id.main_empty_refresh_button)).setOnClickListener(refreshOnClick);
        ((Button)findViewById(R.id.main_empty_hero_button)).setOnClickListener(refreshOnClick);

        // Storage permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Access page
            View.OnClickListener accessOnClick = (View view) -> {
                requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, MainActivity.STORAGE_PERMISSION_REQUEST_CODE);
            };
            ((ImageButton)findViewById(R.id.main_access_refresh_button)).setOnClickListener(accessOnClick);
            ((Button)findViewById(R.id.main_access_hero_button)).setOnClickListener(accessOnClick);

            // Request permission
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                accessPage.setVisibility(View.VISIBLE);
                musicPage.setVisibility(View.GONE);
                requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, MainActivity.STORAGE_PERMISSION_REQUEST_CODE);
            } else {
                loadMusic(true);
            }
        } else {
            loadMusic(true);
        }
    }

    public void onBackPressed() {
        moveTaskToBack(false);
    }

    public void onDestroy() {
        handler.removeCallbacks(syncPlayer);

        mediaPlayer.release();

        if (wakeLock.isHeld()) {
            wakeLock.release();
        }

        super.onDestroy();
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MainActivity.STORAGE_PERMISSION_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            musicPage.setVisibility(View.VISIBLE);
            accessPage.setVisibility(View.GONE);
            loadMusic(true);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MainActivity.SEARCH_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            long id = data.getLongExtra("id", -1);
            if (id != -1) {
                for (Music musicItem : music) {
                    if (id == musicItem.getId()) {
                        handler.post(() -> {
                            startMusic(musicAdapter.getPosition(musicItem), true);
                        });
                        return;
                    }
                }
            }
        }

        if (requestCode == MainActivity.SETTINGS_ACTIVITY_REQUEST_CODE) {
            if (oldLanguage != -1 && oldTheme != -1) {
                if (
                    oldLanguage != settings.getInt("language", Config.SETTINGS_LANGUAGE_DEFAULT) ||
                    oldTheme != settings.getInt("theme", Config.SETTINGS_THEME_DEFAULT)
                ) {
                    handler.post(() -> {
                        recreate();
                    });
                }
            }
        }
    }

    private void loadMusic(boolean updateRatingAlert) {
        music = Music.loadMusic(this);
        musicAdapter.clear();
        musicAdapter.addAll(music);

        if (music.size() == 0) {
            musicPage.setVisibility(View.GONE);
            emptyPage.setVisibility(View.VISIBLE);
        } else {
            startMusic((int)(Math.random() * musicAdapter.getCount()), false);
        }

        if (updateRatingAlert) {
            RatingAlert.updateAndShow(this);
        }
    }

    private void scrollMusic(int position) {
        musicList.clearFocus();
        musicList.post(() -> {
            if (position < musicList.getFirstVisiblePosition()) {
                musicList.setSelection(position);
            }

            if (position > musicList.getLastVisiblePosition()) {
                musicList.setSelection(position - (musicList.getLastVisiblePosition() - musicList.getFirstVisiblePosition() - 1));
            }
        });
    }

    private void startMusic(int position, boolean autoplay) {
        handler.removeCallbacks(syncPlayer);

        playingPosition = position;
        requestAutoplay = autoplay;

        // Reset and prepare media player
        mediaPlayer.reset();

        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build());

        try {
            Music music = musicAdapter.getItem(playingPosition);
            mediaPlayer.setDataSource(this, music.getUri());
            mediaPlayer.prepareAsync();
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        // Highlight playing music
        musicAdapter.setSelectedPosition(playingPosition);

        // Scroll to playing music
        scrollMusic(playingPosition);
    }

    private void playMusic(boolean delayed) {
        musicPlayButton.setImageResource(R.drawable.ic_pause);

        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }

        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }

        handler.removeCallbacks(syncPlayer);

        if (delayed) {
            handler.postDelayed(syncPlayer, Config.MUSIC_SEEKBAR_UPDATE_TIMEOUT);
        } else {
            handler.post(syncPlayer);
        }
    }

    private void pauseMusic() {
        musicPlayButton.setImageResource(R.drawable.ic_play);

        handler.removeCallbacks(syncPlayer);

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }

        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
}
