package nl.plaatsoft.bassiemusic;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.ContextWrapper;
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
    private ImageView refreshButton;
    private LinearLayout musicPage;
    private MusicAdapter musicAdapter;
    private LinearLayout musicPlayer;
    private ImageView musicPlayButton;
    private SeekBar musicSeekBar;
    private LinearLayout emptyPage;
    private LinearLayout accessPage;
    private MediaPlayer mediaPlayer;
    private Handler handler;
    private Runnable syncPlayer;
    private int playingPosition;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        musicPage = (LinearLayout)findViewById(R.id.music_page);
        musicPlayer = (LinearLayout)findViewById(R.id.music_player);
        emptyPage = (LinearLayout)findViewById(R.id.empty_page);
        accessPage = (LinearLayout)findViewById(R.id.access_page);

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void	onCompletion(MediaPlayer mediaPlayer) {
                playMusic(playingPosition == musicAdapter.getCount() - 1 ? 0 : playingPosition + 1);
            }
        });

        musicAdapter = new MusicAdapter(this);
        ListView musicList = (ListView)findViewById(R.id.music_list);
        musicList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                playMusic(position);
            }
        });
        musicList.setAdapter(musicAdapter);

        View.OnClickListener refreshOnClick = new View.OnClickListener() {
            public void onClick(View v) {
                musicPage.setVisibility(View.VISIBLE);
                musicPlayer.setVisibility(View.GONE);
                emptyPage.setVisibility(View.GONE);
                mediaPlayer.stop();
                musicAdapter.clear();
                loadMusic();
            }
        };
        refreshButton = (ImageView)findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(refreshOnClick);
        ((Button)findViewById(R.id.empty_button)).setOnClickListener(refreshOnClick);

        ((ImageView)findViewById(R.id.music_previous_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                playMusic(playingPosition == 0 ? musicAdapter.getCount() - 1 : playingPosition - 1);
            }
        });
        musicPlayButton = (ImageView)findViewById(R.id.music_play_button);
        musicPlayButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (mediaPlayer.isPlaying()) {
                    musicPlayButton.setImageResource(R.drawable.ic_play_arrow);
                    mediaPlayer.pause();
                } else {
                    musicPlayButton.setImageResource(R.drawable.ic_pause);
                    mediaPlayer.start();
                }
            }
        });
        ((ImageView)findViewById(R.id.music_next_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                playMusic(playingPosition == musicAdapter.getCount() - 1 ? 0 : playingPosition + 1);
            }
        });

        musicSeekBar = (SeekBar)findViewById(R.id.music_seekbar);
        musicSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(syncPlayer);
            }
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!mediaPlayer.isPlaying()) {
                    musicPlayButton.setImageResource(R.drawable.ic_pause);
                    mediaPlayer.start();
                }
                mediaPlayer.seekTo(seekBar.getProgress());
                handler.post(syncPlayer);
            }
        });

        TextView musicTimeCurrent = (TextView)findViewById(R.id.music_time_current);
        TextView musicTimeUntil = (TextView)findViewById(R.id.music_time_until);
        handler = new Handler();
        syncPlayer = new Runnable() {
            public void run() {
                musicTimeCurrent.setText(Music.formatDuration(mediaPlayer.getCurrentPosition()));
                musicTimeUntil.setText(Music.formatDuration(mediaPlayer.getDuration() - mediaPlayer.getCurrentPosition()));
                musicSeekBar.setProgress(mediaPlayer.getCurrentPosition());
                handler.postDelayed(this, 100);
            }
        };

        if (Build.VERSION.SDK_INT >= 23) {
            ((Button)findViewById(R.id.access_button)).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    musicPage.setVisibility(View.VISIBLE);
                    accessPage.setVisibility(View.GONE);
                    requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, 1);
                }
            });

            if (new ContextWrapper(this).checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                refreshButton.setVisibility(View.GONE);
                requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, 1);
            } else {
                loadMusic();
            }
        } else {
            loadMusic();
        }
    }
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(syncPlayer);
        mediaPlayer.release();
    }
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                refreshButton.setVisibility(View.VISIBLE);
                loadMusic();
            } else {
                musicPage.setVisibility(View.GONE);
                accessPage.setVisibility(View.VISIBLE);
            }
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
            mediaPlayer.prepare();
        } catch (Exception e) {}
        musicPlayer.setVisibility(View.VISIBLE);
        musicPlayButton.setImageResource(R.drawable.ic_pause);
        musicSeekBar.setMax(mediaPlayer.getDuration());
        mediaPlayer.start();
        handler.post(syncPlayer);
    }
}