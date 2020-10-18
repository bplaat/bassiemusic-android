package nl.plaatsoft.bassiemusic;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.SeekBar;
import android.Manifest;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private static final int REQUEST_STORAGE_PERMISSION = 1;
    private static final int SEEK_TIME_SKIP = 10000;

    private LinearLayout musicPage;
    private LinearLayout searchPage;
    private EditText searchInput;
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
        SharedPreferences preferences = getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (preferences.getBoolean("dark-theme", false)) {
            setTheme(R.style.dark_app_theme);
        }
        setContentView(R.layout.activity_main);

        musicPage = (LinearLayout)findViewById(R.id.music_page);
        LinearLayout musicPlayer = (LinearLayout)findViewById(R.id.music_player);

        searchPage = (LinearLayout)findViewById(R.id.search_page);
        searchInput = (EditText)findViewById(R.id.search_input);

        emptyPage = (LinearLayout)findViewById(R.id.empty_page);

        accessPage = (LinearLayout)findViewById(R.id.access_page);

        wakeLock = ((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BassieMusic::WakeLock");

        // Music page
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

        // Empty page
        View.OnClickListener refreshOnClick = new View.OnClickListener() {
            public void onClick(View view) {
                musicPage.setVisibility(View.VISIBLE);
                musicPlayer.setVisibility(View.GONE);
                emptyPage.setVisibility(View.GONE);
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                loadMusic();
            }
        };
        ((ImageView)findViewById(R.id.music_refresh_button)).setOnClickListener(refreshOnClick);
        ((ImageView)findViewById(R.id.empty_refresh_button)).setOnClickListener(refreshOnClick);
        ((Button)findViewById(R.id.empty_button)).setOnClickListener(refreshOnClick);

        ((ImageView)findViewById(R.id.music_search_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                searchPage.setVisibility(View.VISIBLE);
                searchPage.setAlpha(0f);
                searchPage.setTranslationY(64);
                searchPage.animate().alpha(1).translationY(0).setDuration(150);

                searchInput.requestFocus();
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }
        });

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
                    mediaPlayer.seekTo(Math.max(mediaPlayer.getCurrentPosition() - SEEK_TIME_SKIP, 0), MediaPlayer.SEEK_CLOSEST_SYNC);
                } else {
                    mediaPlayer.seekTo(Math.max(mediaPlayer.getCurrentPosition() - SEEK_TIME_SKIP, 0));
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
                    mediaPlayer.seekTo(Math.min(mediaPlayer.getCurrentPosition() + SEEK_TIME_SKIP, mediaPlayer.getDuration()), MediaPlayer.SEEK_CLOSEST_SYNC);
                } else {
                    mediaPlayer.seekTo(Math.min(mediaPlayer.getCurrentPosition() + SEEK_TIME_SKIP, mediaPlayer.getDuration()));
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

        handler = new Handler(Looper.getMainLooper());

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

        // Search page
        ((ImageView)findViewById(R.id.search_back_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                closeSearchPage();
            }
        });

        ListView searchList = (ListView)findViewById(R.id.search_list);

        MusicAdapter searchAdapter = new MusicAdapter(this);
        searchList.setAdapter(searchAdapter);

        searchList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Music musicItem = searchAdapter.getItem(position);
                for (int i = 0; i < music.size(); i++) {
                    Music otherMusicItem = music.get(i);
                    if (musicItem.getId() == otherMusicItem.getId()) {
                        int goodPosition = musicAdapter.getPosition(otherMusicItem);
                        musicList.setSelection(goodPosition);

                        closeSearchPage();

                        playMusic(goodPosition);

                        return;
                    }
                }
            }
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                String searchQuery = charSequence.toString().toLowerCase();

                searchAdapter.clear();

                if (searchQuery.length() >= 1) {
                    for (Music musicItem : music) {
                        if (musicItem.getTitle().toLowerCase().contains(searchQuery)) {
                            searchAdapter.add(musicItem);
                        }
                    }
                }

                searchList.setSelectionAfterHeaderView();
            }

            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            public void afterTextChanged(Editable editable) {}
        });

        ((ImageView)findViewById(R.id.search_clear_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                searchInput.setText("");
            }
        });

        // Media player
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
            // Access page
            View.OnClickListener accessOnClick = new View.OnClickListener() {
                public void onClick(View view) {
                    requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, MainActivity.REQUEST_STORAGE_PERMISSION);
                }
            };
            ((ImageView)findViewById(R.id.access_refresh_button)).setOnClickListener(accessOnClick);
            ((Button)findViewById(R.id.access_button)).setOnClickListener(accessOnClick);

            // Request permission
            if (new ContextWrapper(this).checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                accessPage.setVisibility(View.VISIBLE);
                musicPage.setVisibility(View.GONE);
                requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, MainActivity.REQUEST_STORAGE_PERMISSION);
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
        if (requestCode == MainActivity.REQUEST_STORAGE_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            musicPage.setVisibility(View.VISIBLE);
            accessPage.setVisibility(View.GONE);
            loadMusic();
        }
    }

    private void closeSearchPage() {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        searchInput.setText("");
        searchInput.clearFocus();

        searchPage.animate().alpha(0).translationY(64).setDuration(150).withEndAction(new Runnable() {
            public void run() {
                searchPage.setVisibility(View.GONE);
            }
        });
    }

    private void loadMusic() {
        music = new ArrayList<Music>();

        Cursor musicCursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            new String[] { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DURATION },
            null, null, MediaStore.Audio.Media.TITLE);

        if (musicCursor != null) {
            while (musicCursor.moveToNext()) {
                long musicId = musicCursor.getLong(musicCursor.getColumnIndex(MediaStore.Audio.Media._ID));
                music.add(new Music(
                    musicId,
                    musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                    musicCursor.getLong(musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION)),
                    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, musicId)
                ));
            }
            musicCursor.close();
        }

        musicAdapter.clear();
        musicAdapter.addAll(music);

        if (music.size() == 0) {
            musicPage.setVisibility(View.GONE);
            emptyPage.setVisibility(View.VISIBLE);
        }
    }

    @SuppressWarnings("deprecation")
    private void playMusic(int position) {
        handler.removeCallbacks(syncPlayer);

        playingPosition = position;

        mediaPlayer.reset();

        if (Build.VERSION.SDK_INT >= 21) {
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build());
        } else {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }

        try {
            mediaPlayer.setDataSource(this, musicAdapter.getItem(position).getUri());
            mediaPlayer.prepareAsync();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
