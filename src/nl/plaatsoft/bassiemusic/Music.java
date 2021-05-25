package nl.plaatsoft.bassiemusic;

import android.content.Context;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class Music {
    private long id;
    private String title;
    private long duration;
    private Uri uri;

    public Music(long id, String title, long duration, Uri uri) {
        this.id = id;
        this.title = title;
        this.duration = duration;
        this.uri = uri;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public long getDuration() {
        return duration;
    }

    public Uri getUri() {
        return uri;
    }

    public static String formatDuration(long ms) {
        long s = ms / 1000;
        if (s >= 3600) {
            return String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
        } else {
            return String.format("%d:%02d", s / 60, s % 60);
        }
    }

    public static List<Music> loadMusic(Context context) {
        List<Music> music = new ArrayList<Music>();

        Cursor musicCursor = context.getContentResolver().query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            new String[] { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DURATION },
            null, null, null
        );
        if (musicCursor != null) {
            while (musicCursor.moveToNext()) {
                long musicId = musicCursor.getLong(musicCursor.getColumnIndex(MediaStore.Audio.Media._ID));
                music.add(new Music(
                    musicId,
                    musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)).trim(),
                    musicCursor.getLong(musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION)),
                    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, musicId)
                ));
            }
            musicCursor.close();
        }

        Collections.sort(music, (Music a, Music b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));

        return music;
    }
}
