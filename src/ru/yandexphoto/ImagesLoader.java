package ru.yandexphoto;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;
import com.yandex.disk.client.Credentials;
import com.yandex.disk.client.ProgressListener;
import com.yandex.disk.client.TransportClient;
import com.yandex.disk.client.exceptions.CancelledPropfindException;
import com.yandex.disk.client.exceptions.WebdavException;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ImagesLoader extends AsyncTaskLoader<List<File>> {
    private List<String> imagePaths;
    private List<String> storePaths;
    private Credentials credentials;

    private String lastExceptionMessage = null;
    private final String ARRAY_LENGTH_ERROR_MESSAGE = "Arrays of input and output paths must have the same length";

    public ImagesLoader(Context context, List<String> imagePaths, List<String> storePaths, Credentials credentials) {
        super(context);
        this.imagePaths = imagePaths;
        this.storePaths = storePaths;
        this.credentials = credentials;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    public List<File> loadInBackground() {
        lastExceptionMessage = null;
        TransportClient client = null;
        if(storePaths.size() != imagePaths.size()) {
            lastExceptionMessage = ARRAY_LENGTH_ERROR_MESSAGE;
            return null;
        }
        try {
            client = TransportClient.getInstance(getContext(), credentials);
            for(int i = 0; i < imagePaths.size(); i++) {
                File imageOnLocalStorage = new File(storePaths.get(i));
                client.downloadFile(imagePaths.get(i), imageOnLocalStorage, new ProgressListener() {
                    @Override
                    public void updateProgress(long loaded, long total) {}

                    @Override
                    public boolean hasCancelled() { return false; }
                });
            }
        } catch (WebdavException e) {
            lastExceptionMessage = e.getMessage();
        } catch (IOException e) {
            lastExceptionMessage = e.getMessage();
        } finally {
            TransportClient.shutdown(client);
        }
        return null;
    }

    @Override
    public void deliverResult(List<File> data) {
        super.deliverResult(data);
    }

    public boolean errorOccurred() {
        return lastExceptionMessage != null;
    }

    public String getLastExceptionMessage() {
        return lastExceptionMessage;
    }
}
