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
    private List<String> artists;
    private String album;
    private String title;
    private int position;
    private long duration;
    private Uri uri;
    private Uri coverUri;

    public Music(long id, List<String> artists, String album, String title, int position, long duration, Uri uri, Uri coverUri) {
        this.id = id;
        this.artists = artists;
        this.album = album;
        this.title = title;
        this.position = position;
        this.duration = duration;
        this.uri = uri;
        this.coverUri = coverUri;
    }

    public long getId() {
        return id;
    }

    public List<String> getArtists() {
        return artists;
    }

    public String getAlbum() {
        return album;
    }

    public String getTitle() {
        return title;
    }

    public int getPosition() {
        return position;
    }

    public long getDuration() {
        return duration;
    }

    public Uri getUri() {
        return uri;
    }

    public Uri getCoverUri() {
        return coverUri;
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
            new String[] { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.CD_TRACK_NUMBER, MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID },
            null, null, null
        );
        if (musicCursor != null) {
            while (musicCursor.moveToNext()) {
                long musicId = musicCursor.getLong(musicCursor.getColumnIndex(MediaStore.Audio.Media._ID));
                String artist = musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)).trim();
                String album = musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)).trim();
                String title = musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)).trim();
                String trackNumber = musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Media.CD_TRACK_NUMBER));
                long duration = musicCursor.getLong(musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                long albumId = musicCursor.getLong(musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));

                List<String> artists = new ArrayList<String>();
                int position = 1;
                if (artist != null && !artist.equals("<unknown>")) {
                    String[] artistParts = artist.split(",");
                    for (String artistPart : artistParts) {
                        artists.add(artistPart.trim());
                    }

                    if (trackNumber != null) {
                        String[] trackNumberParts = trackNumber.split("/");
                        if (trackNumberParts.length >= 1) {
                            position = Integer.parseInt(trackNumberParts[0]);
                        }
                    }
                } else {
                    String[] titleParts = title.split("-");
                    if (titleParts.length >= 2) {
                        artists.add(titleParts[0].trim());

                        int i = 2;
                        try {
                            position = Integer.parseInt(titleParts[1].trim());
                            album = titleParts[0].trim();
                        } catch (Exception exception) {
                            album = titleParts[1].trim();
                            try {
                                position = Integer.parseInt(titleParts[2].trim());
                                i = 3;
                            } catch (Exception exception2) {}
                        }

                        title = "";
                        for (; i < titleParts.length; i++) {
                            title += titleParts[i].trim();
                            if (i != titleParts.length - 1) title += " ";
                        }
                    }
                }

                if (title.equals("")) {
                    title = album;
                }

                if (artists.size() == 0) {
                    artists.add("Unkown artist");
                }

                Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, musicId);

                Uri coverUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);

                music.add(new Music(musicId, artists, album, title, position, duration, contentUri, coverUri));
            }
            musicCursor.close();
        }

        Collections.sort(music, (Music a, Music b) -> a.getPosition() - b.getPosition());
        Collections.sort(music, (Music a, Music b) -> a.getAlbum().compareToIgnoreCase(b.getAlbum()));
        Collections.sort(music, (Music a, Music b) -> a.getArtists().get(0).compareToIgnoreCase(b.getArtists().get(0)));

        return music;
    }
}
