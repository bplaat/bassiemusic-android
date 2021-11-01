package nl.plaatsoft.bassiemusic;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Looper;
import android.os.Handler;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.List;

public class FetchImageTask {
    public static interface OnLoadListener {
        public abstract void onLoad(Bitmap image);
    }

    public static interface OnErrorListener {
        public abstract void onError(Exception exception);
    }

    private static final Executor executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final List<FetchImageTask> tasks = new ArrayList<FetchImageTask>();

    private Context context;
    private String url;
    private boolean isTransparent;
    private boolean isFadedIn;
    private boolean isLoadedFomCache = true;
    private boolean isSavedToCache = true;
    private OnLoadListener onLoadListener;
    private OnErrorListener onErrorListener;
    private ImageView imageView;
    private boolean isFetching;
    private boolean isCanceled;
    private boolean isFinished;
    private long startTime;

    private FetchImageTask(Context context) {
        this.context = context;
    }

    public String getUrl() {
        return url;
    }

    public boolean isFetching() {
        return isFetching;
    }

    public boolean isCanceled() {
        return isCanceled;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public static FetchImageTask with(Context context) {
        return new FetchImageTask(context);
    }

    public FetchImageTask load(String url) {
        this.url = url;
        return this;
    }

    public FetchImageTask transparent() {
        isTransparent = true;
        return this;
    }

    public FetchImageTask fadeIn() {
        isFadedIn = true;
        return this;
    }

    public FetchImageTask noCache() {
        isLoadedFomCache = false;
        isSavedToCache = false;
        return this;
    }

    public FetchImageTask notFromCache() {
        isLoadedFomCache = false;
        return this;
    }

    public FetchImageTask notToCache() {
        isSavedToCache = false;
        return this;
    }

    public FetchImageTask then(OnLoadListener onLoadListener) {
        this.onLoadListener = onLoadListener;
        return this;
    }

    public FetchImageTask then(OnLoadListener onLoadListener, OnErrorListener onErrorListener) {
        this.onLoadListener = onLoadListener;
        this.onErrorListener = onErrorListener;
        return this;
    }

    public FetchImageTask into(ImageView imageView) {
        this.imageView = imageView;
        return this;
    }

    public FetchImageTask fetch() {
        if (imageView != null) {
            FetchImageTask previousFetchImageTask = (FetchImageTask)imageView.getTag();
            if (previousFetchImageTask != null) {
                if (previousFetchImageTask.getUrl().equals(url)) {
                    cancel();
                    return this;
                } else {
                    if (!previousFetchImageTask.isFinished()) {
                        previousFetchImageTask.cancel();
                    }
                }
            }

            imageView.setTag(this);
            imageView.setImageBitmap(null);
        }

        tasks.add(this);
        for (FetchImageTask task : tasks) {
            if (task.getUrl().equals(url) && task.isFetching()) {
                return this;
            }
        }

        isFetching = true;
        startTime = System.currentTimeMillis();
        executor.execute(() -> {
            try {
                Bitmap image = fetchImage();
                handler.post(() -> {
                    onLoad(image);
                });
            } catch (Exception exception) {
                handler.post(() -> {
                    if (!isCanceled) {
                        finish();
                        if (onErrorListener != null) {
                            onErrorListener.onError(exception);
                        } else {
                            exception.printStackTrace();
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
        tasks.remove(this);
    }

    private Bitmap fetchImage() throws Exception {
        // Check if the file exists in the cache
        File file = new File(context.getCacheDir(), Utils.md5(url));
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = isTransparent ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        if (isLoadedFomCache && file.exists()) {
            return BitmapFactory.decodeFile(file.getPath(), options);
        }

        // Or fetch the image from the internet in to a byte array buffer
        BufferedInputStream bufferedInputStream = new BufferedInputStream(new URL(url).openStream());
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int number_read = 0;
        while ((number_read = bufferedInputStream.read(buffer, 0, buffer.length)) != -1) {
            byteArrayOutputStream.write(buffer, 0, number_read);
        }
        byteArrayOutputStream.close();
        bufferedInputStream.close();

        byte[] image = byteArrayOutputStream.toByteArray();

        // When needed save the image to a cache file
        if (isSavedToCache) {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(image);
            fileOutputStream.close();
        }

        return BitmapFactory.decodeByteArray(image, 0, image.length, options);
    }

    public void onLoad(Bitmap image) {
        if (!isCanceled) {
            finish();

            if (imageView != null) {
                if (isTransparent) {
                    imageView.setBackgroundColor(Color.TRANSPARENT);
                }

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

            if (isFetching) {
                for (int i = 0; i < tasks.size(); i++) {
                    FetchImageTask task = tasks.get(i);
                    if (task.getUrl().equals(url) && !task.isFetching()) {
                        task.onLoad(image);
                        i--;
                    }
                }
            }
        }
    }
}
