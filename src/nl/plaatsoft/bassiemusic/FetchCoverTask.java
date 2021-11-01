package nl.plaatsoft.bassiemusic;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Looper;
import android.os.Handler;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

public class FetchCoverTask {
    public static interface OnLoadListener {
        public abstract void onLoad(Bitmap image);
    }

    public static interface OnErrorListener {
        public abstract void onError(Exception exception);
    }

    private static final Executor executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static final Handler handler = new Handler(Looper.getMainLooper());

    private Context context;
    private Music music;
    private boolean isFadedIn;
    private boolean isLoadedFomCache = true;
    private boolean isSavedToCache = true;
    private OnLoadListener onLoadListener;
    private OnErrorListener onErrorListener;
    private ImageView imageView;
    private boolean isCanceled;
    private boolean isFinished;
    private long startTime;

    private FetchCoverTask(Context context) {
        this.context = context;
    }

    public Music getMusic() {
        return music;
    }

    public boolean isCanceled() {
        return isCanceled;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public static FetchCoverTask with(Context context) {
        return new FetchCoverTask(context);
    }

    public FetchCoverTask fromMusic(Music music) {
        this.music = music;
        return this;
    }

    public FetchCoverTask fadeIn() {
        isFadedIn = true;
        return this;
    }

    public FetchCoverTask noCache() {
        isLoadedFomCache = false;
        isSavedToCache = false;
        return this;
    }

    public FetchCoverTask notFromCache() {
        isLoadedFomCache = false;
        return this;
    }

    public FetchCoverTask notToCache() {
        isSavedToCache = false;
        return this;
    }

    public FetchCoverTask then(OnLoadListener onLoadListener) {
        this.onLoadListener = onLoadListener;
        return this;
    }

    public FetchCoverTask then(OnLoadListener onLoadListener, OnErrorListener onErrorListener) {
        this.onLoadListener = onLoadListener;
        this.onErrorListener = onErrorListener;
        return this;
    }

    public FetchCoverTask into(ImageView imageView) {
        this.imageView = imageView;
        return this;
    }

    public FetchCoverTask fetch() {
        if (imageView != null) {
            FetchCoverTask previousFetchCoverTask = (FetchCoverTask)imageView.getTag();
            if (previousFetchCoverTask != null) {
                if (previousFetchCoverTask.getMusic().getAlbum().equals(music.getAlbum())) {
                    cancel();
                    return this;
                } else {
                    if (!previousFetchCoverTask.isFinished()) {
                        previousFetchCoverTask.cancel();
                    }
                }
            }

            imageView.setTag(this);
            imageView.setImageBitmap(null);
        }

        startTime = System.currentTimeMillis();
        executor.execute(() -> {
            try {
                Bitmap image = fetchCover();
                handler.post(() -> {
                    onLoad(image);
                });
            } catch (Exception exception) {
                // When an album cover dont exists fetch and cache it from the nice and open Deezer API
                // I know this code is a callback / exception nightmare, I'm working on it
                handler.post(() -> {
                    if (!isCanceled) {
                        try {
                            String url = Config.DEEZER_API_URL + "/search/album?q=" +
                                URLEncoder.encode(music.getArtists().get(0) + " - " + music.getAlbum(), "UTF-8") + "&limit=1";
                            FetchDataTask.with(context).load(url).withCache().then(data -> {
                                if (!isCanceled) {
                                    try {
                                        JSONArray albumsJson = new JSONObject(data).getJSONArray("data");
                                        if (albumsJson.length() > 0) {
                                            JSONObject albumJson = albumsJson.getJSONObject(0);
                                            FetchImageTask.with(context).load(albumJson.getString("cover_medium")).fadeIn().then(image -> {
                                                onLoad(image);
                                            }, exception2 -> {
                                                onExpection(exception2);
                                            }).fetch();
                                        }
                                    } catch (Exception exception2) {
                                        onExpection(exception2);
                                    }
                                }
                            }, exception2 -> {
                                onExpection(exception2);
                            }).fetch();
                        } catch (Exception exception2) {
                            onExpection(exception2);
                        }
                    }
                });
            }
        });

        return this;
    }

    public void cancel() {
        isCanceled = true;
        finish();
    }

    private void finish() {
        isFinished = true;
    }

    private Bitmap fetchCover() throws Exception {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        BufferedInputStream bufferedInputStream = new BufferedInputStream(context.getContentResolver().openInputStream(music.getAlbumCoverUri()));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int number_read = 0;
        while ((number_read = bufferedInputStream.read(buffer, 0, buffer.length)) != -1) {
            byteArrayOutputStream.write(buffer, 0, number_read);
        }
        byteArrayOutputStream.close();
        bufferedInputStream.close();

        byte[] image = byteArrayOutputStream.toByteArray();
        return BitmapFactory.decodeByteArray(image, 0, image.length, options);
    }

    private void onLoad(Bitmap image) {
        if (!isCanceled) {
            finish();

            if (imageView != null) {
                boolean isWaitingLong = (System.currentTimeMillis() - startTime) > Config.ANIMATION_IMAGE_LOADING_TIMEOUT;
                if (isFadedIn && isWaitingLong) {
                    imageView.setImageAlpha(0);
                }

                imageView.setImageBitmap(image);

                if (isFadedIn && isWaitingLong) {
                    ValueAnimator animation = ValueAnimator.ofInt(0, 255);
                    animation.setDuration(context.getResources().getInteger(R.integer.animation_duration));
                    animation.setInterpolator(new AccelerateDecelerateInterpolator());
                    animation.addUpdateListener(animator -> {
                        imageView.setImageAlpha((int)animator.getAnimatedValue());
                    });
                    animation.start();
                }
            }

            if (onLoadListener != null) {
                onLoadListener.onLoad(image);
            }
        }
    }

    private void onExpection(Exception exception) {
        if (!isCanceled) {
            finish();
            if (onErrorListener != null) {
                onErrorListener.onError(exception);
            } else {
                exception.printStackTrace();
            }
        }
    }
}