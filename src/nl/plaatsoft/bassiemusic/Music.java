package nl.plaatsoft.bassiemusic;

public class Music {
    private String title;
    private long duration;
    private String path;
    public Music(String title, long duration, String path) {
        this.title = title;
        this.duration = duration;
        this.path = path;
    }
    public String getTitle() {
        return title;
    }
    public long getDuration() {
        return duration;
    }
    public String getPath() {
        return path;
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