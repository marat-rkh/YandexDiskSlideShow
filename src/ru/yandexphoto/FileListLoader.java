package ru.yandexphoto;

import android.content.Context;
import android.os.Handler;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;
import com.yandex.disk.client.Credentials;
import com.yandex.disk.client.ListItem;
import com.yandex.disk.client.ListParsingHandler;
import com.yandex.disk.client.TransportClient;
import com.yandex.disk.client.exceptions.CancelledPropfindException;
import com.yandex.disk.client.exceptions.WebdavException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileListLoader extends AsyncTaskLoader<List<ListItem>> {

    private final static String LOG_TAG = "FileListLoader";

    private Credentials credentials;
    private String dir;
    private Handler tasksHandler = new Handler();

    private List<ListItem> fileItemList = new ArrayList<ListItem>();
    private Exception lastException;
    private boolean hasCancelled;

//    private static final int ITEMS_PER_REQUEST = 20;

    public FileListLoader(Context context, Credentials credentials, String dir) {
        super(context);
        this.credentials = credentials;
        this.dir = dir;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    protected void onReset() {
        super.onReset();
        hasCancelled = true;
    }

    @Override
    public List<ListItem> loadInBackground() {
        hasCancelled = false;
        fileItemList.clear();
        TransportClient client = null;
        try {
            client = TransportClient.getInstance(getContext(), credentials);
            client.getList(dir, new CustomListParsingHandler());
            lastException = null;
        } catch (CancelledPropfindException e) {
            return fileItemList;
        } catch (WebdavException e) {
            Log.d(LOG_TAG, "loadInBackground", e);
            lastException = e;
        } catch (IOException e) {
            Log.d(LOG_TAG, "loadInBackground", e);
            lastException = e;
        } finally {
            TransportClient.shutdown(client);
        }
        return fileItemList;
    }

    public Exception getLastException() {
        return lastException;
    }

    private class CustomListParsingHandler extends ListParsingHandler {
        @Override
        public boolean hasCancelled() {
            return hasCancelled;
        }

        @Override
        public void onPageFinished(int itemsOnPage) {
            tasksHandler.post(new Runnable() {
                @Override
                public void run () {
                    //deliver result with first element skipped (first elem is the current collection name)
                    fileItemList.remove(0);
                    deliverResult(new ArrayList<ListItem>(fileItemList));
                }
            });
        }

        @Override
        public boolean handleItem(ListItem item) {
            fileItemList.add(item);
            return true;
        }
    }
}