package nl.plaatsoft.bassiemusic;

import android.content.Context;
import android.os.Looper;
import android.os.Handler;
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

public class FetchDataTask {
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
    private String url;
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

    public static FetchDataTask with(Context context) {
        return new FetchDataTask(context);
    }

    public FetchDataTask load(String url) {
        this.url = url;
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
            if (task.getUrl().equals(url) && task.isFetching()) {
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

    private String fetchData() throws Exception {
        // Check if the url is already cached
        File file = new File(context.getCacheDir(), Utils.md5(url));
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
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
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
                    if (task.getUrl().equals(url) && !task.isFetching()) {
                        task.onLoad(data);
                        i--;
                    }
                }
            }
        }
    }
}
