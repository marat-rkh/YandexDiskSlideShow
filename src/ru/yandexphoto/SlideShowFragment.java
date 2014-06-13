package ru.yandexphoto;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.yandex.disk.client.Credentials;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class SlideShowFragment extends Fragment {
    private static final String LOG_TAG = "SlideShowFragment";
    public static final String FRAGMENT_TAG = "SlideShowView";
    public static final String CREDENTIALS_ARG = "CredentialsArg";
    public static final String IMG_PATHS_ARG = "ImgPathsArg";

    private final String LOADED_IMAGES_DIR = "LoadedImages";

    private ImageView mainImageView = null;
    private View filesLoadingProgressView;

    private ExecutorCompletionService<String> downloadsService = null;
    private ExecutorService downloadsExecutor = null;
    private final int PRE_LOADS_LIMIT = 3;
    private ScheduledExecutorService scheduleTaskExecutor = null;
    private final int FLIPS_DELAY = 4;
    private int currentImgToLoad = 0;
    private int proceededTasks = 0;
    private int imageToDelete = 0;

    private Credentials credentials;
    private List<String> imagesPaths;
    private List<String> storePaths;

    // Fragment lifecycle methods overriding
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View slideShowView = inflater.inflate(R.layout.shlideshow_layout, container, false);
        if(slideShowView != null) {
            mainImageView = (ImageView)slideShowView.findViewById(R.id.main_image_view);
            filesLoadingProgressView = slideShowView.findViewById(R.id.progress_view);
        }
        return slideShowView;
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
            startLoadTask();
            startSlideShow();
        } else {
            showNoFilesScreen();
        }
        showHint("Press back to exit");
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

    private void startLoadTask() {
        downloadsExecutor = Executors.newSingleThreadExecutor();
        downloadsService = new ExecutorCompletionService<String>(downloadsExecutor);
        int allImgs = imagesPaths.size();
        for(currentImgToLoad = 0; currentImgToLoad < PRE_LOADS_LIMIT && currentImgToLoad < allImgs; currentImgToLoad++) {
            submitDownloadTask();
        }
    }

    private void submitDownloadTask() {
        String imgPath = imagesPaths.get(currentImgToLoad);
        String storePath = storePaths.get(currentImgToLoad);
        downloadsService.submit(new LoadImageTask(imgPath, storePath, credentials, getActivity()));
    }

    private void startSlideShow() {
        proceededTasks = 0;
        imageToDelete = 0;
        scheduleTaskExecutor = Executors.newScheduledThreadPool(1);
        scheduleTaskExecutor.scheduleAtFixedRate(new FlipImagesTask(), 0, FLIPS_DELAY, TimeUnit.SECONDS);
    }

    private void showNoFilesScreen() {
        getView().findViewById(R.id.progress_indicator).setVisibility(View.INVISIBLE);
        TextView textLabel = (TextView)getView().findViewById(R.id.progress_label);
        textLabel.setText(R.string.no_image_files);
        textLabel.setVisibility(View.VISIBLE);
    }

    private void showHint(String hint) {
        Toast.makeText(getActivity(), hint, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        stopExecutors();
        clearMainImageView();
        deleteLoadedImages();
        super.onDestroy();
    }

    private void stopExecutors() {
        if(downloadsExecutor != null) {
            downloadsExecutor.shutdownNow();
        }
        if(scheduleTaskExecutor != null) {
            scheduleTaskExecutor.shutdownNow();
        }
        mainImageView.removeCallbacks(null);
    }

    private void clearMainImageView() {
        Drawable drawable = mainImageView.getDrawable();
        mainImageView.setImageDrawable(null);
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            bitmap.recycle();
        }
    }

    private void deleteLoadedImages() {
        for(; imageToDelete < proceededTasks; ++imageToDelete) {
            String loadedImagePath = storePaths.get(imageToDelete);
            boolean succeeded = new File(loadedImagePath).delete();
            if(!succeeded) {
                Log.d(LOG_TAG, "file not deleted or not found: " + loadedImagePath);
            }
        }
    }

    private class FlipImagesTask implements Runnable {
        @Override
        public void run() {
            if(proceededTasks != imagesPaths.size()) {
                File imgFile = null;
                do {
                    submitNextTaskIfNeeded();
                    setProgressBar(View.VISIBLE);
                    String loadedImgPath;
                    try {
                        loadedImgPath = downloadsService.take().get();
                    } catch (InterruptedException e) {
                        return;
                    } catch (ExecutionException e) {
                        String imageNumber =  (proceededTasks + 1) + "/" + imagesPaths.size();
                        showInfoMessage("Loading problems: image " + imageNumber + " was skipped");
                        continue;
                    } finally {
                        ++proceededTasks;
                    }
                    imgFile = new File(loadedImgPath);
                } while (proceededTasks < imagesPaths.size() && (imgFile == null || !imgFile.exists()));
                if (imgFile != null) {
                    flipImage(imgFile);
                    deleteLoadedImages();
                }
            }
        }

        private void submitNextTaskIfNeeded() {
            if (currentImgToLoad < imagesPaths.size()) {
                submitDownloadTask();
                ++currentImgToLoad;
            }
        }

        private void setProgressBar(final int mode) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    filesLoadingProgressView.setVisibility(mode);
                }
            });
        }

        private void showInfoMessage(final String msg) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showHint(msg);
                }
            });
        }

        private void flipImage(File imgFile) {
            final Bitmap bitmap = decodeWithScaling(imgFile.getAbsolutePath());
            setProgressBar(View.GONE);
            setMainImageView(bitmap);
            showInfoMessage(proceededTasks + "/" + imagesPaths.size());
        }

        private void setMainImageView(final Bitmap bitmap) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mainImageView.setImageBitmap(bitmap);
                }
            });
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
    }
}
