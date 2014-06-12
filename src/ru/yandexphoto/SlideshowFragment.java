package ru.yandexphoto;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.yandex.disk.client.Credentials;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SlideshowFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<File>> {

    private static final String LOG_TAG = "SlideshowFragment";
    public static final String FRAGMENT_TAG = "SlideshowView";
    public static final String CREDENTIALS_ARG = "CredentialsArg";
    public static final String IMG_PATHS_ARG = "ImgPathsArg";

    private final String LOADED_IMAGES_DIR = "LoadedImages";

    private ImageView mainImageView = null;
    private View filesLoadingProgressView;
    private int currentImage = 0;

    private ScheduledExecutorService scheduleTaskExecutor = null;

    private Credentials credentials;
    private List<String> imagesPaths;
    private List<String> storePaths;

    // Fragment lifecycle methods overriding
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View slideshowView = inflater.inflate(R.layout.shlideshow_layout, container, false);
        if(slideshowView != null) {
            mainImageView = (ImageView)slideshowView.findViewById(R.id.main_image_view);
            filesLoadingProgressView = slideshowView.findViewById(R.id.progress_view);
        }
        return slideshowView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(getActivity().getActionBar() != null) {
            getActivity().getActionBar().hide();
        }
        getDataFromArguments();
        fillStorePaths();
        filesLoadingProgressView.setVisibility(View.VISIBLE);
        if(storePaths.size() != 0) {
            getLoaderManager().initLoader(0, null, this);
        } else {
            getView().findViewById(R.id.progress_indicator).setVisibility(View.INVISIBLE);
            TextView textLabel = (TextView)getView().findViewById(R.id.progress_label);
            textLabel.setText(R.string.no_image_files);
            textLabel.setVisibility(View.VISIBLE);
            showNavigationHint();
        }
    }

    private void getDataFromArguments() {
        credentials = (Credentials)getArguments().get(CREDENTIALS_ARG);
        imagesPaths = (List<String>)getArguments().get(IMG_PATHS_ARG);
    }

    private void fillStorePaths() {
        storePaths = new ArrayList<String>();
        for(String imagePath : imagesPaths) {
            int delimiterLastIndex = imagePath.lastIndexOf("/");
            String imageName = delimiterLastIndex >= 0 ? imagePath.substring(delimiterLastIndex) : imagePath;
            File dirToStore = getActivity().getDir(LOADED_IMAGES_DIR, Context.MODE_PRIVATE);
            storePaths.add(dirToStore + imageName);
        }
    }

    @Override
    public void onDestroy() {
        if(scheduleTaskExecutor != null) {
            scheduleTaskExecutor.shutdownNow();
        }

        mainImageView.removeCallbacks(null);
        Drawable drawable = mainImageView.getDrawable();
        mainImageView.setImageDrawable(null);

        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            bitmap.recycle();
        }

        for(String loadedImagePath : storePaths) {
            boolean succeeded = new File(loadedImagePath).delete();
            if(!succeeded) {
                Log.d(LOG_TAG, "file not deleted or not found: " + loadedImagePath);
            }
        }
        super.onDestroy();
    }

    // LoaderCallbacks methods overriding
    @Override
    public Loader<List<File>> onCreateLoader(int i, Bundle bundle) {
        return new ImagesLoader(getActivity(), imagesPaths, storePaths, credentials);
    }

    @Override
    public void onLoadFinished(Loader<List<File>> listLoader, List<File> files) {
        filesLoadingProgressView.setVisibility(View.GONE);
        if(((ImagesLoader)listLoader).errorOccurred()) {
            Log.d(LOG_TAG, ((ImagesLoader) listLoader).getLastExceptionMessage());
        } else {
            startSlideshow();
        }
    }

    private void startSlideshow() {
        scheduleTaskExecutor = Executors.newScheduledThreadPool(1);
        scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                File imgFile;
                do {
                    imgFile = new  File(storePaths.get(currentImage));
                    currentImage = (currentImage + 1) % storePaths.size();
                } while (!imgFile.exists());
                final Bitmap bitmap;
                if(imgFile.exists()) {
                    bitmap = decodeWithScaling(imgFile.getAbsolutePath());
                } else {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainImageView.setImageBitmap(bitmap);
                    }
                });
            }
        }, 0, 3, TimeUnit.SECONDS);
        showNavigationHint();
    }

    private void showNavigationHint() {
        Toast.makeText(getActivity(), "Press back to exit", Toast.LENGTH_SHORT).show();
    }

    private Bitmap decodeWithScaling(String imgFilePath) {
        WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int screenWidth = display.getWidth();
        int screenHeight = display.getHeight();

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imgFilePath, bmOptions);
        int imageWidth = bmOptions.outWidth;
        int imageHeight = bmOptions.outHeight;

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = Math.max(imageWidth / screenWidth, imageHeight / screenHeight);
        return BitmapFactory.decodeFile(imgFilePath, bmOptions);
    }

    @Override
    public void onLoaderReset(Loader<List<File>> listLoader) {}
}
