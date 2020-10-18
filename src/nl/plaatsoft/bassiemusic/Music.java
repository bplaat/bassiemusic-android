package nl.plaatsoft.bassiemusic;

import android.net.Uri;

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
}
