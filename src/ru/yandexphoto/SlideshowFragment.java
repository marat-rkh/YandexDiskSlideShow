package ru.yandexphoto;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import com.yandex.disk.client.Credentials;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SlideshowFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<File>> {

    private static final String LOG_TAG = "SlideshowFragment";
    public static final String FRAGMENT_TAG = "SlideshowView";

    private final String LOADED_IMAGES_DIR = "LoadedImages";

    private ViewFlipper viewFlipper = null;
    private View filesLoadingProgressIndicator;

    private Credentials credentials;
    private List<String> imagesPaths;
    private List<String> storePaths;

    public SlideshowFragment(Credentials credentials, List<String> imagesPaths) {
        this.credentials = credentials;
        this.imagesPaths = imagesPaths;
    }

    // Fragment lifecycle methods overriding
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View slideshowView = inflater.inflate(R.layout.shlideshow_layout, container, false);
        if(slideshowView != null) {
            viewFlipper = (ViewFlipper)slideshowView.findViewById(R.id.view_flipper);
            filesLoadingProgressIndicator = slideshowView.findViewById(R.id.progress_indicator);
        }
        return slideshowView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(getActivity().getActionBar() != null) {
            getActivity().getActionBar().hide();
        }
        fillStorePaths();
        filesLoadingProgressIndicator.setVisibility(View.VISIBLE);
        getLoaderManager().initLoader(0, null, this);
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
        filesLoadingProgressIndicator.setVisibility(View.GONE);
        if(((ImagesLoader)listLoader).errorOccurred()) {
            Log.d(LOG_TAG, ((ImagesLoader) listLoader).getLastExceptionMessage());
        } else {
            fillViewFlipper();
            viewFlipper.setAutoStart(true);
            viewFlipper.setFlipInterval(3000);
            Toast.makeText(getActivity(), "Press back to exit", Toast.LENGTH_SHORT).show();
            viewFlipper.startFlipping();
        }
    }

    private void fillViewFlipper() {
        for(String loadedImagePath : storePaths) {
            File imgFile = new  File(loadedImagePath);
            if(imgFile.exists()){
                Bitmap bitmap = decodeWithScaling(imgFile.getAbsolutePath());
                ImageView imageView = new ImageView(getActivity());
                imageView.setImageBitmap(bitmap);
                viewFlipper.addView(imageView);
            }
        }
    }

    private Bitmap decodeWithScaling(String imgFilePath) {
        /* Get the size of the ImageView */
        WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int screenWidth = display.getWidth();
        int screenHeight = display.getHeight();

        /* Get the size of the image */
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imgFilePath, bmOptions);
        int imageWidth = bmOptions.outWidth;
        int imageHeight = bmOptions.outHeight;

        /* Figure out which way needs to be reduced less */
        int scaleFactor = Math.min(imageWidth/screenWidth, imageHeight/screenHeight);

        /* Set bitmap options to scale the image decode target */
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
//        bmOptions.inPurgeable = true;
//
        return BitmapFactory.decodeFile(imgFilePath, bmOptions);
    }

    @Override
    public void onLoaderReset(Loader<List<File>> listLoader) {}
}
