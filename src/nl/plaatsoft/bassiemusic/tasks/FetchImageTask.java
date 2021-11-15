package nl.plaatsoft.bassiemusic.tasks;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Looper;
import android.os.Handler;
import android.util.Log;
import android.util.LruCache;
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
import nl.plaatsoft.bassiemusic.Config;
import nl.plaatsoft.bassiemusic.Utils;
import nl.plaatsoft.bassiemusic.R;

public class FetchImageTask implements Task {
    public static interface OnLoadListener {
        public abstract void onLoad(Bitmap image);
    }

    public static interface OnErrorListener {
        public abstract void onError(Exception exception);
    }

    public class AlreadyFailedImage extends Exception {
        private static final long serialVersionUID = 1;

        public AlreadyFailedImage(String message) {
            super(message);
        }
    }

    private static final Executor executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final List<FetchImageTask> tasks = new ArrayList<FetchImageTask>();
    private static final List<Uri> failedImagesCache = new ArrayList<Uri>();
    private static final LruCache<Uri, Bitmap> bitmapCache = new LruCache<Uri, Bitmap>((int)(Runtime.getRuntime().freeMemory() / 2)) {
        @Override
        protected int sizeOf(Uri uri, Bitmap bitmap) {
            return bitmap.getByteCount();
        }
    };

    private Context context;
    private Uri uri;
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

    @Override
    public Uri getUri() {
        return uri;
    }

    public boolean isFetching() {
        return isFetching;
    }

    @Override
    public boolean isCanceled() {
        return isCanceled;
    }

    @Override
    public boolean isFinished() {
        return isFinished;
    }

    public long getStartTime() {
        return startTime;
    }

    public static FetchImageTask with(Context context) {
        return new FetchImageTask(context);
    }

    public FetchImageTask load(Uri uri) {
        this.uri = uri;
        return this;
    }

    public FetchImageTask load(String url) {
        uri = Uri.parse(url);
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
            if (imageView.getTag() instanceof Task) {
                Task previousTask = (Task)imageView.getTag();
                if (previousTask != null) {
                    if (previousTask.getUri().equals(uri)) {
                        cancel();
                        return this;
                    } else {
                        if (!previousTask.isFinished()) {
                            previousTask.cancel();
                        }
                    }
                }
            }

            imageView.setTag(this);
            if (!(bitmapCache.get(uri) != null || failedImagesCache.contains(uri))) {
                imageView.setImageBitmap(null);
            }
        }

        tasks.add(this);
        for (FetchImageTask task : tasks) {
            if (task.isFetching() && task.getUri().equals(uri)) {
                startTime = task.getStartTime();
                return this;
            }
        }

        startTime = System.currentTimeMillis();
        isFetching = true;

        // Check of bitmap is already in cache
        if (bitmapCache.get(uri) != null) {
            finish();
            onLoad(bitmapCache.get(uri));
            return this;
        }

        // Check if the image failed before
        if (failedImagesCache.contains(uri)) {
            finish();
            onException(new AlreadyFailedImage("This image already failed before"));
            return this;
        }

        executor.execute(() -> {
            try {
                Bitmap image = fetchImage();
                handler.post(() -> {
                    onLoad(image);
                });
            } catch (Exception exception) {
                handler.post(() -> {
                    onException(exception);
                });
            }
        });

        return this;
    }

    @Override
    public void cancel() {
        isCanceled = true;
        finish();
    }

    @Override
    public void finish() {
        isFinished = true;
        if (imageView != null) {
            imageView.setTag(null);
        }
        tasks.remove(this);
    }

    private Bitmap fetchImage() throws Exception {
        Bitmap image;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = isTransparent ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;

        // Check if the uri is a local content uri: for example an album art
        if (uri.getScheme().equals("content")) {
            image = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, options);
        } else {
            // Check if the file exists in the cache
            File file = new File(context.getCacheDir(), Utils.md5(uri.toString()));
            if (isLoadedFomCache && file.exists()) {
                image = BitmapFactory.decodeFile(file.getPath(), options);
            } else {
                // Or fetch the image from the internet in to a byte array buffer
                BufferedInputStream bufferedInputStream = new BufferedInputStream(new URL(uri.toString()).openStream());
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int number_read = 0;
                while ((number_read = bufferedInputStream.read(buffer, 0, buffer.length)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, number_read);
                }
                byteArrayOutputStream.close();
                bufferedInputStream.close();

                byte[] imageBytes = byteArrayOutputStream.toByteArray();

                // When needed save the image to a cache file
                if (isSavedToCache) {
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    fileOutputStream.write(imageBytes);
                    fileOutputStream.close();
                }

                image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
            }
        }

        // Put bitmap in cache when it is not
        synchronized (bitmapCache) {
            if (bitmapCache.get(uri) == null) {
                bitmapCache.put(uri, image);
            }
        }
        return image;
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
                    if (task.getUri().equals(uri) && !task.isFetching()) {
                        task.onLoad(image);
                        i--;
                    }
                }
            }
        }
    }

    public void onException(Exception exception) {
        if (!isCanceled) {
            finish();

            if (!failedImagesCache.contains(uri)) {
                failedImagesCache.add(uri);
            }

            if (onErrorListener != null) {
                onErrorListener.onError(exception);
            } else {
                Log.e(Config.LOG_TAG, "An exception catched!", exception);
            }

            if (isFetching) {
                for (int i = 0; i < tasks.size(); i++) {
                    FetchImageTask task = tasks.get(i);
                    if (task.getUri().equals(uri) && !task.isFetching()) {
                        task.onException(exception);
                        i--;
                    }
                }
            }
        }
    }
}
