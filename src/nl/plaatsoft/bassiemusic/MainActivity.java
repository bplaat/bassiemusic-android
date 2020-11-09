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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.SeekBar;
import android.Manifest;
import java.util.ArrayList;

public class MainActivity extends BaseActivity {
    public static final int SEARCH_ACTIVITY_REQUEST_CODE = 1;
    public static final int SETTINGS_ACTIVITY_REQUEST_CODE = 2;
    public static final int STORAGE_PERMISSION_REQUEST_CODE = 1;
    public static final int SEEK_TIME_SKIP = 10000;

    private int oldLanguage = -1;
    private int oldTheme = -1;

    private LinearLayout musicPage;
    private LinearLayout musicPlayer;
    private ListView musicList;
    private LinearLayout emptyPage;
    private LinearLayout accessPage;

    private PowerManager.WakeLock wakeLock;
    private ArrayList<Music> music;
    private MusicAdapter musicAdapter;
    private MediaPlayer mediaPlayer;
    private Handler handler;
    private Runnable syncPlayer;
    private int playingPosition;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        musicPage = (LinearLayout)findViewById(R.id.music_page);
        musicPlayer = (LinearLayout)findViewById(R.id.music_player);

        emptyPage = (LinearLayout)findViewById(R.id.empty_page);

        accessPage = (LinearLayout)findViewById(R.id.access_page);

        wakeLock = ((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BassieMusic::WakeLock");

        // Music page
        musicList = (ListView)findViewById(R.id.music_list);
        musicAdapter = new MusicAdapter(this);
        musicList.setAdapter(musicAdapter);
        musicList.setOnItemClickListener((AdapterView<?> adapterView, View view, int position, long id) -> {
            playMusic(position);
        });

        ((ImageView)findViewById(R.id.music_shuffle_button)).setOnClickListener((View view) -> {
            playMusic((int)(Math.random() * musicAdapter.getCount()));
        });

        ((ImageView)findViewById(R.id.music_search_button)).setOnClickListener((View view) -> {
            startActivityForResult(new Intent(this, SearchActivity.class), MainActivity.SEARCH_ACTIVITY_REQUEST_CODE);
        });

        ((ImageView)findViewById(R.id.music_settings_button)).setOnClickListener((View view) -> {
            oldLanguage = settings.getInt("language", SettingsActivity.LANGUAGE_DEFAULT);
            oldTheme = settings.getInt("theme", SettingsActivity.THEME_DEFAULT);
            startActivityForResult(new Intent(this, SettingsActivity.class), MainActivity.SETTINGS_ACTIVITY_REQUEST_CODE);
        });

        // Empty page
        View.OnClickListener refreshOnClick = (View view) -> {
            musicPage.setVisibility(View.VISIBLE);
            musicAdapter.setSelectedPosition(-1);
            musicPlayer.setVisibility(View.GONE);
            emptyPage.setVisibility(View.GONE);
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            loadMusic();
        };
        ((TextView)findViewById(R.id.music_refresh_button)).setOnClickListener(refreshOnClick);
        ((ImageView)findViewById(R.id.empty_refresh_button)).setOnClickListener(refreshOnClick);
        ((Button)findViewById(R.id.empty_button)).setOnClickListener(refreshOnClick);

        ImageView musicPlayButton = (ImageView)findViewById(R.id.music_play_button);
        ((ImageView)findViewById(R.id.music_previous_button)).setOnClickListener((View view) -> {
            if (mediaPlayer.getCurrentPosition() > 2500) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mediaPlayer.seekTo(0, MediaPlayer.SEEK_CLOSEST_SYNC);
                } else {
                    mediaPlayer.seekTo(0);
                }

                if (!mediaPlayer.isPlaying()) {
                    musicPlayButton.setImageResource(R.drawable.ic_pause);
                    mediaPlayer.start();
                    wakeLock.acquire();
                }
            } else {
                playMusic(playingPosition == 0 ? musicAdapter.getCount() - 1 : playingPosition - 1);
            }
        });

        ((ImageView)findViewById(R.id.music_seek_back_button)).setOnClickListener((View view) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mediaPlayer.seekTo(Math.max(mediaPlayer.getCurrentPosition() - SEEK_TIME_SKIP, 0), MediaPlayer.SEEK_CLOSEST_SYNC);
            } else {
                mediaPlayer.seekTo(Math.max(mediaPlayer.getCurrentPosition() - SEEK_TIME_SKIP, 0));
            }

            if (!mediaPlayer.isPlaying()) {
                musicPlayButton.setImageResource(R.drawable.ic_pause);
                mediaPlayer.start();
                wakeLock.acquire();
            }
        });

        musicPlayButton.setOnClickListener((View view) -> {
            if (mediaPlayer.isPlaying()) {
                musicPlayButton.setImageResource(R.drawable.ic_play);
                mediaPlayer.pause();
                wakeLock.release();
            } else {
                musicPlayButton.setImageResource(R.drawable.ic_pause);
                mediaPlayer.start();
                wakeLock.acquire();
            }
        });

        ((ImageView)findViewById(R.id.music_seek_forward_button)).setOnClickListener((View view) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mediaPlayer.seekTo(Math.min(mediaPlayer.getCurrentPosition() + SEEK_TIME_SKIP, mediaPlayer.getDuration()), MediaPlayer.SEEK_CLOSEST_SYNC);
            } else {
                mediaPlayer.seekTo(Math.min(mediaPlayer.getCurrentPosition() + SEEK_TIME_SKIP, mediaPlayer.getDuration()));
            }

            if (!mediaPlayer.isPlaying()) {
                musicPlayButton.setImageResource(R.drawable.ic_pause);
                mediaPlayer.start();
                wakeLock.acquire();
            }
        });

        ((ImageView)findViewById(R.id.music_next_button)).setOnClickListener((View view) -> {
            playMusic(playingPosition == musicAdapter.getCount() - 1 ? 0 : playingPosition + 1);
        });

        TextView musicTimeCurrentLabel = (TextView)findViewById(R.id.music_time_current_label);
        TextView musicTimeUntilLabel = (TextView)findViewById(R.id.music_time_until_label);
        SeekBar musicSeekBar = (SeekBar)findViewById(R.id.music_seekbar);

        handler = new Handler(Looper.getMainLooper());

        syncPlayer = () -> {
            musicTimeCurrentLabel.setText(Music.formatDuration(mediaPlayer.getCurrentPosition()));
            musicTimeUntilLabel.setText("-" + Music.formatDuration(mediaPlayer.getDuration() - mediaPlayer.getCurrentPosition()));
            musicSeekBar.setProgress(mediaPlayer.getCurrentPosition());
            handler.postDelayed(syncPlayer, 100);
        };

        musicSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(syncPlayer);
            }

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    musicTimeCurrentLabel.setText(Music.formatDuration(progress));
                    musicTimeUntilLabel.setText("-" + Music.formatDuration(mediaPlayer.getDuration() - progress));
                }
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mediaPlayer.seekTo(seekBar.getProgress(), MediaPlayer.SEEK_CLOSEST_SYNC);
                } else {
                    mediaPlayer.seekTo(seekBar.getProgress());
                }

                if (!mediaPlayer.isPlaying()) {
                    musicPlayButton.setImageResource(R.drawable.ic_pause);
                    mediaPlayer.start();
                    wakeLock.acquire();
                }

                handler.post(syncPlayer);
            }
        });

        // Media player
        mediaPlayer = new MediaPlayer();

        mediaPlayer.setOnPreparedListener((MediaPlayer mediaPlayer) -> {
            musicPlayer.setVisibility(View.VISIBLE);
            musicSeekBar.setMax(mediaPlayer.getDuration());
            musicPlayButton.setImageResource(R.drawable.ic_pause);
            mediaPlayer.start();
            wakeLock.acquire();
            handler.post(syncPlayer);
        });

        mediaPlayer.setOnCompletionListener((MediaPlayer mediaPlayer) -> {
            playMusic(playingPosition == musicAdapter.getCount() - 1 ? 0 : playingPosition + 1);
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Access page
            View.OnClickListener accessOnClick = (View view) -> {
                requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, MainActivity.STORAGE_PERMISSION_REQUEST_CODE);
            };
            ((ImageView)findViewById(R.id.access_refresh_button)).setOnClickListener(accessOnClick);
            ((Button)findViewById(R.id.access_button)).setOnClickListener(accessOnClick);

            // Request permission
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                accessPage.setVisibility(View.VISIBLE);
                musicPage.setVisibility(View.GONE);
                requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, MainActivity.STORAGE_PERMISSION_REQUEST_CODE);
            } else {
                RatingAlert.check(this);
                loadMusic();
            }
        } else {
            RatingAlert.check(this);
            loadMusic();
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
            loadMusic();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MainActivity.SEARCH_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            long id = data.getLongExtra("id", -1);
            if (id != -1) {
                for (Music musicItem : music) {
                    if (id == musicItem.getId()) {
                        handler.post(() -> {
                            playMusic(musicAdapter.getPosition(musicItem));
                        });
                        return;
                    }
                }
            }
        }

        if (requestCode == MainActivity.SETTINGS_ACTIVITY_REQUEST_CODE) {
            if (oldLanguage != -1 && oldTheme != -1) {
                if (
                    oldLanguage != settings.getInt("language", SettingsActivity.LANGUAGE_DEFAULT) ||
                    oldTheme != settings.getInt("theme", SettingsActivity.THEME_DEFAULT)
                ) {
                    handler.post(() -> {
                        recreate();
                    });
                }
            }
        }
    }

    private void loadMusic() {
        music = Music.loadMusic(this);
        musicAdapter.addAll(music);

        if (music.size() == 0) {
            musicPage.setVisibility(View.GONE);
            emptyPage.setVisibility(View.VISIBLE);
        }
    }

    private void playMusic(int position) {
        handler.removeCallbacks(syncPlayer);

        playingPosition = position;

        musicAdapter.setSelectedPosition(position);

        if (position < musicList.getFirstVisiblePosition()) {
            musicList.setSelection(position);
        }

        if (position > musicList.getLastVisiblePosition()) {
            if (musicPlayer.getVisibility() == View.VISIBLE) {
                musicList.setSelection(position - (musicList.getLastVisiblePosition() - musicList.getFirstVisiblePosition() - 1));
            } else {
                musicList.setSelection(position - (musicList.getLastVisiblePosition() - musicList.getFirstVisiblePosition() - 2));
            }
        }

        mediaPlayer.reset();

        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build());

        try {
            mediaPlayer.setDataSource(this, musicAdapter.getItem(position).getUri());
            mediaPlayer.prepareAsync();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
