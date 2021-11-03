package nl.plaatsoft.bassiemusic.tasks;

import android.content.Context;
import android.os.Looper;
import android.os.Handler;
import android.net.Uri;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.List;
import nl.plaatsoft.bassiemusic.Config;
import nl.plaatsoft.bassiemusic.Utils;

public class FetchDataTask implements Task {
    public static interface OnLoadListener {
        public abstract void onLoad(String data);
    }

    public static interface OnErrorListener {
        public abstract void onError(Exception exception);
    }

    private static final Executor executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final List<FetchDataTask> tasks = new ArrayList<FetchDataTask>();

    private Context context;
    private Uri uri;
    private boolean isLoadedFomCache;
    private boolean isSavedToCache;
    private OnLoadListener onLoadListener;
    private OnErrorListener onErrorListener;
    private boolean isFetching;
    private boolean isCanceled;
    private boolean isFinished;

    private FetchDataTask(Context context) {
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

    public static FetchDataTask with(Context context) {
        return new FetchDataTask(context);
    }

    public FetchDataTask load(Uri uri) {
        this.uri = uri;
        return this;
    }

    public FetchDataTask load(String url) {
        uri = Uri.parse(url);
        return this;
    }

    public FetchDataTask withCache() {
        isLoadedFomCache = true;
        isSavedToCache = true;
        return this;
    }

    public FetchDataTask fromCache() {
        isLoadedFomCache = true;
        return this;
    }

    public FetchDataTask toCache() {
        isSavedToCache = true;
        return this;
    }

    public FetchDataTask then(OnLoadListener onLoadListener) {
        this.onLoadListener = onLoadListener;
        return this;
    }

    public FetchDataTask then(OnLoadListener onLoadListener, OnErrorListener onErrorListener) {
        this.onLoadListener = onLoadListener;
        this.onErrorListener = onErrorListener;
        return this;
    }

    public FetchDataTask fetch() {
        tasks.add(this);
        for (FetchDataTask task : tasks) {
            if (task.isFetching() && task.getUri().equals(uri)) {
                return this;
            }
        }

        isFetching = true;
        executor.execute(() -> {
            try {
                String data = fetchData();
                handler.post(() -> {
                    onLoad(data);
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
        tasks.remove(this);
    }

    private String fetchData() throws Exception {
        // Check if the uri is already cached
        File file = new File(context.getCacheDir(), Utils.md5(uri.toString()));
        if (isLoadedFomCache && file.exists()) {
            // Then read the cached file
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append(System.lineSeparator());
            }
            bufferedReader.close();
            return stringBuilder.toString();
        }

        // Or fetch the data from the internet
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new URL(uri.toString()).openStream()));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line).append(System.lineSeparator());
        }
        bufferedReader.close();

        // And write to a cache file when needed
        String data = stringBuilder.toString();
        if (isSavedToCache) {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(data);
            fileWriter.close();
        }
        return data;
    }

    public void onLoad(String data) {
        if (!isCanceled) {
            finish();

            if (onLoadListener != null) {
                onLoadListener.onLoad(data);
            }

            if (isFetching) {
                for (int i = 0; i < tasks.size(); i++) {
                    FetchDataTask task = tasks.get(i);
                    if (task.getUri().equals(uri) && !task.isFetching()) {
                        task.onLoad(data);
                        i--;
                    }
                }
            }
        }
    }

    public void onException(Exception exception) {
        if (!isCanceled) {
            finish();

            if (onErrorListener != null) {
                onErrorListener.onError(exception);
            } else {
                Log.e(Config.LOG_TAG, "An exception catched!", exception);
            }

            if (isFetching) {
                for (int i = 0; i < tasks.size(); i++) {
                    FetchDataTask task = tasks.get(i);
                    if (task.getUri().equals(uri) && !task.isFetching()) {
                        task.onException(exception);
                        i--;
                    }
                }
            }
        }
    }
}
