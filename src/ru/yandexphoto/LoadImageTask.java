package ru.yandexphoto;

import android.content.Context;
import com.yandex.disk.client.Credentials;
import com.yandex.disk.client.ProgressListener;
import com.yandex.disk.client.TransportClient;

import java.io.File;
import java.util.concurrent.Callable;

public class LoadImageTask implements Callable<String> {
    private String imagePath;
    private String storePath;
    private Credentials credentials;
    private Context context;

    private ProgressListener emptyListener = new ProgressListener() {
        @Override
        public void updateProgress(long loaded, long total) { }
        @Override
        public boolean hasCancelled() { return false; }
    };

    public LoadImageTask(String imagePath, String storePath, Credentials credentials, Context context) {
        this.imagePath = imagePath;
        this.storePath = storePath;
        this.credentials = credentials;
        this.context = context;
    }

    @Override
    public String call() throws Exception {
        TransportClient client = null;
        try {
            client = TransportClient.getInstance(context, credentials);
            File imageOnLocalStorage = new File(storePath);
            client.downloadFile(imagePath, imageOnLocalStorage, emptyListener);
        } finally {
            if(client != null) {
                TransportClient.shutdown(client);
            }
        }
        return storePath;
    }
}
