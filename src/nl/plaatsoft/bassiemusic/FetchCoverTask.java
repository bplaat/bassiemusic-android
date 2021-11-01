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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FetchCoverTask {
    private static final Executor executor = Executors.newFixedThreadPool(8);
    private static final Handler handler = new Handler(Looper.getMainLooper());

    public static interface OnLoadListener {
        public abstract void onLoad(Bitmap image);
    }

    public static interface OnErrorListener {
        public abstract void onError(Exception exception);
    }

    private final Context context;

    private Uri uri;
    private boolean isFadedIn = false;
    private OnLoadListener onLoadListener = null;
    private OnErrorListener onErrorListener = null;
    private ImageView imageView;
    private boolean isFinished = false;
    private boolean isCanceled = false;

    private FetchCoverTask(Context context) {
        this.context = context;
    }

    public static FetchCoverTask with(Context context) {
        return new FetchCoverTask(context);
    }

    public FetchCoverTask load(Uri uri) {
        this.uri = uri;
        return this;
    }

    public FetchCoverTask fadeIn() {
        this.isFadedIn = true;
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
                if (!uri.equals(previousFetchCoverTask.getUri())) {
                    if (!previousFetchCoverTask.isFinished()) {
                        previousFetchCoverTask.cancel();
                    }
                } else {
                    cancel();
                    return this;
                }
            }

            imageView.setTag(this);
            imageView.setImageBitmap(null);
        }

        long startTime = System.currentTimeMillis();
        executor.execute(() -> {
            try {
                Bitmap image = fetchCover();
                handler.post(() -> {
                    finish();
                    if (!isCanceled) {
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
                });
            } catch (Exception exception) {
                handler.post(() -> {
                    finish();
                    if (!isCanceled) {
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

    public Uri getUri() {
        return uri;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public boolean isCanceled() {
        return isCanceled;
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

        BufferedInputStream bufferedInputStream = new BufferedInputStream(context.getContentResolver().openInputStream(uri));
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
}
