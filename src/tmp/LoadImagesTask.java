package tmp;

import android.content.Context;
import com.yandex.disk.client.Credentials;
import com.yandex.disk.client.ProgressListener;
import com.yandex.disk.client.TransportClient;
import com.yandex.disk.client.exceptions.WebdavException;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class LoadImagesTask implements Runnable {
    private final LoadsCounter loadsCounter;
    private final BooleanLock isWaitingMode = new BooleanLock(false);

    private List<String> imagePaths;
    private List<String> storePaths;
    private Credentials credentials;
    private Context context;
    private int loadsLimit;

    private String lastExceptionMessage = null;
    private final String ARRAY_LENGTH_ERROR_MESSAGE = "Arrays of input and output paths must have the same length";

    private ProgressListener emptyListener = new ProgressListener() {
        @Override
        public void updateProgress(long loaded, long total) { }
        @Override
        public boolean hasCancelled() { return false; }
    };

    public LoadImagesTask(List<String> imagePaths, List<String> storePaths,
                          Credentials credentials, Context context, int loadsLimit, LoadsCounter loadsCounter) {
        this.imagePaths = imagePaths;
        this.storePaths = storePaths;
        this.credentials = credentials;
        this.context = context;
        this.loadsLimit = loadsLimit;
        this.loadsCounter = loadsCounter;
    }

    @Override
    public void run() {
        lastExceptionMessage = null;
        if(storePaths.size() != imagePaths.size()) {
            setExceptionMessage(ARRAY_LENGTH_ERROR_MESSAGE);
        } else {
            TransportClient client = null;
            try {
                client = TransportClient.getInstance(context, credentials);
                for (int i = 0; i < imagePaths.size(); i++) {
                    File imageOnLocalStorage = new File(storePaths.get(i));
                    client.downloadFile(imagePaths.get(i), imageOnLocalStorage, emptyListener);
                    synchronized (loadsCounter) {
                        loadsCounter.increment();
                        loadsCounter.notify();
                    }
                    if(loadsCounter.getValue() % loadsLimit == 0) {
                        isWaitingMode.set(true);
                        synchronized (isWaitingMode) {
                            try {
                                while (isWaitingMode.isTrue()) {
                                    isWaitingMode.wait();
                                }
                            } catch (InterruptedException e) {
                                return;
                            }
                        }
                    }
                }
            } catch (WebdavException e) {
                setExceptionMessage(e.getMessage());
            } catch (IOException e) {
                setExceptionMessage(e.getMessage());
            } finally {
                TransportClient.shutdown(client);
            }
        }
    }

    public boolean isWaiting() {
        return isWaitingMode.isTrue();
    }

    public void resetWaitingMode() {
        isWaitingMode.set(false);
        synchronized (isWaitingMode) {
            isWaitingMode.notify();
        }
    }

    public boolean errorOccurred() {
        return lastExceptionMessage != null;
    }

    public String getLastExceptionMessage() {
        return lastExceptionMessage;
    }

    private void setExceptionMessage(String message) {
        lastExceptionMessage = message;
        synchronized (loadsCounter) {
            loadsCounter.notify();
        }
    }
}
