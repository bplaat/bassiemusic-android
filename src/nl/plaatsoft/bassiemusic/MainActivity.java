package nl.plaatsoft.bassiemusic;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.provider.MediaStore;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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

public class MainActivity extends Activity {
    private LinearLayout musicPage;
    private LinearLayout emptyPage;
    private LinearLayout accessPage;
    private PowerManager.WakeLock wakeLock;
    private MusicAdapter musicAdapter;
    private MediaPlayer mediaPlayer;
    private Handler handler;
    private Runnable syncPlayer;
    private int playingPosition;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences preferences = getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (preferences.getBoolean("dark-theme", false)) {
            setTheme(R.style.dark_app_theme);
        }
        setContentView(R.layout.activity_main);

        musicPage = (LinearLayout)findViewById(R.id.music_page);
        LinearLayout musicPlayer = (LinearLayout)findViewById(R.id.music_player);
        emptyPage = (LinearLayout)findViewById(R.id.empty_page);
        accessPage = (LinearLayout)findViewById(R.id.access_page);

        wakeLock = ((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BassieMusic::WakeLock");

        ListView musicList = (ListView)findViewById(R.id.music_list);
        musicAdapter = new MusicAdapter(this);
        musicList.setAdapter(musicAdapter);
        musicList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                playMusic(position);
            }
        });

        ((ImageView)findViewById(R.id.music_shuffle_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                int position = (int)(Math.random() * musicAdapter.getCount());
                musicList.setSelection(position);
                playMusic(position);
            }
        });

        ((TextView)findViewById(R.id.light_dark_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("dark-theme", !preferences.getBoolean("dark-theme", false));
                editor.apply();

                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
        });

        View.OnClickListener refreshOnClick = new View.OnClickListener() {
            public void onClick(View view) {
                musicPage.setVisibility(View.VISIBLE);
                musicPlayer.setVisibility(View.GONE);
                emptyPage.setVisibility(View.GONE);
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                musicAdapter.clear();
                loadMusic();
            }
        };
        ((ImageView)findViewById(R.id.music_refresh_button)).setOnClickListener(refreshOnClick);
        ((ImageView)findViewById(R.id.empty_refresh_button)).setOnClickListener(refreshOnClick);
        ((Button)findViewById(R.id.empty_button)).setOnClickListener(refreshOnClick);

        ImageView musicPlayButton = (ImageView)findViewById(R.id.music_play_button);
        ((ImageView)findViewById(R.id.music_previous_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (mediaPlayer.getCurrentPosition() > 2500) {
                    if (Build.VERSION.SDK_INT >= 26) {
                        mediaPlayer.seekTo(0, MediaPlayer.SEEK_CLOSEST_SYNC);
                    } else {
                        mediaPlayer.seekTo(0);
                    }
                    if (!mediaPlayer.isPlaying()) {
                        musicPlayButton.setImageResource(preferences.getBoolean("dark-theme", false) ? R.drawable.ic_pause_dark : R.drawable.ic_pause_light);
                        mediaPlayer.start();
                        wakeLock.acquire();
                    }
                } else {
                    playMusic(playingPosition == 0 ? musicAdapter.getCount() - 1 : playingPosition - 1);
                }
            }
        });
        ((ImageView)findViewById(R.id.music_seek_back_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= 26) {
                    mediaPlayer.seekTo(Math.max(mediaPlayer.getCurrentPosition() - 10000, 0), MediaPlayer.SEEK_CLOSEST_SYNC);
                } else {
                    mediaPlayer.seekTo(Math.max(mediaPlayer.getCurrentPosition() - 10000, 0));
                }
                if (!mediaPlayer.isPlaying()) {
                    musicPlayButton.setImageResource(preferences.getBoolean("dark-theme", false) ? R.drawable.ic_pause_dark : R.drawable.ic_pause_light);
                    mediaPlayer.start();
                    wakeLock.acquire();
                }
            }
        });
        musicPlayButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (mediaPlayer.isPlaying()) {
                    musicPlayButton.setImageResource(preferences.getBoolean("dark-theme", false) ? R.drawable.ic_play_dark : R.drawable.ic_play_light);
                    mediaPlayer.pause();
                    wakeLock.release();
                } else {
                    musicPlayButton.setImageResource(preferences.getBoolean("dark-theme", false) ? R.drawable.ic_pause_dark : R.drawable.ic_pause_light);
                    mediaPlayer.start();
                    wakeLock.acquire();
                }
            }
        });
        ((ImageView)findViewById(R.id.music_seek_forward_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= 26) {
                    mediaPlayer.seekTo(Math.min(mediaPlayer.getCurrentPosition() + 10000, mediaPlayer.getDuration()), MediaPlayer.SEEK_CLOSEST_SYNC);
                } else {
                    mediaPlayer.seekTo(Math.min(mediaPlayer.getCurrentPosition() + 10000, mediaPlayer.getDuration()));
                }
                if (!mediaPlayer.isPlaying()) {
                    musicPlayButton.setImageResource(preferences.getBoolean("dark-theme", false) ? R.drawable.ic_pause_dark : R.drawable.ic_pause_light);
                    mediaPlayer.start();
                    wakeLock.acquire();
                }
            }
        });
        ((ImageView)findViewById(R.id.music_next_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                playMusic(playingPosition == musicAdapter.getCount() - 1 ? 0 : playingPosition + 1);
            }
        });

        TextView musicTimeCurrentLabel = (TextView)findViewById(R.id.music_time_current_label);
        TextView musicTimeUntilLabel = (TextView)findViewById(R.id.music_time_until_label);
        SeekBar musicSeekBar = (SeekBar)findViewById(R.id.music_seekbar);
        handler = new Handler();
        syncPlayer = new Runnable() {
            public void run() {
                musicTimeCurrentLabel.setText(Music.formatDuration(mediaPlayer.getCurrentPosition()));
                musicTimeUntilLabel.setText("-" + Music.formatDuration(mediaPlayer.getDuration() - mediaPlayer.getCurrentPosition()));
                musicSeekBar.setProgress(mediaPlayer.getCurrentPosition());
                handler.postDelayed(this, 100);
            }
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
                if (Build.VERSION.SDK_INT >= 26) {
                    mediaPlayer.seekTo(seekBar.getProgress(), MediaPlayer.SEEK_CLOSEST_SYNC);
                } else {
                    mediaPlayer.seekTo(seekBar.getProgress());
                }
                if (!mediaPlayer.isPlaying()) {
                    musicPlayButton.setImageResource(preferences.getBoolean("dark-theme", false) ? R.drawable.ic_pause_dark : R.drawable.ic_pause_light);
                    mediaPlayer.start();
                    wakeLock.acquire();
                }
                handler.post(syncPlayer);
            }
        });

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mediaPlayer) {
                musicPlayer.setVisibility(View.VISIBLE);
                musicSeekBar.setMax(mediaPlayer.getDuration());
                musicPlayButton.setImageResource(preferences.getBoolean("dark-theme", false) ? R.drawable.ic_pause_dark : R.drawable.ic_pause_light);
                mediaPlayer.start();
                wakeLock.acquire();
                handler.post(syncPlayer);
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void	onCompletion(MediaPlayer mediaPlayer) {
                playMusic(playingPosition == musicAdapter.getCount() - 1 ? 0 : playingPosition + 1);
            }
        });

        if (Build.VERSION.SDK_INT >= 23) {
            View.OnClickListener accessOnClick = new View.OnClickListener() {
                public void onClick(View view) {
                    requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, 1);
                }
            };
            ((ImageView)findViewById(R.id.access_refresh_button)).setOnClickListener(accessOnClick);
            ((Button)findViewById(R.id.access_button)).setOnClickListener(accessOnClick);

            if (new ContextWrapper(this).checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                accessPage.setVisibility(View.VISIBLE);
                musicPage.setVisibility(View.GONE);
                requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, 1);
            } else {
                loadMusic();
            }
        } else {
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
        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            musicPage.setVisibility(View.VISIBLE);
            accessPage.setVisibility(View.GONE);
            loadMusic();
        }
    }

    private void loadMusic() {
        Cursor musicCursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[] { MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.DATA }, null, null, MediaStore.Audio.Media.TITLE);
        if (musicCursor != null) {
            while (musicCursor.moveToNext()) {
                musicAdapter.add(new Music(
                    musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                    musicCursor.getLong(musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION)),
                    musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                ));
            }
            musicCursor.close();
        }
        if (musicAdapter.getCount() == 0) {
            musicPage.setVisibility(View.GONE);
            emptyPage.setVisibility(View.VISIBLE);
        }
    }

    private void playMusic(int position) {
        handler.removeCallbacks(syncPlayer);
        playingPosition = position;
        mediaPlayer.reset();
        try {
            mediaPlayer.setDataSource(musicAdapter.getItem(position).getPath());
            mediaPlayer.prepareAsync();
        } catch (Exception e) {}
    }
}
