package ru.yandexphoto;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class SlideshowFragment extends Fragment {

    private static final String LOG_TAG = "SlideshowFragment";
    public static final String FRAGMENT_TAG = "SlideshowView";

    private ViewFlipper viewFlipper = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View slideshowView = inflater.inflate(R.layout.shlideshow_layout, container, false);
        if(slideshowView != null) {
            viewFlipper = (ViewFlipper) slideshowView.findViewById(R.id.view_flipper);
        }
        if(viewFlipper != null) {
            ImageView imageView = new ImageView(getActivity());
            imageView.setImageDrawable(getResources().getDrawable(R.drawable.lightning));
            viewFlipper.addView(imageView);
            imageView = new ImageView(getActivity());
            imageView.setImageDrawable(getResources().getDrawable(R.drawable.color_baloons));
            viewFlipper.addView(imageView);
            imageView = new ImageView(getActivity());
            imageView.setImageDrawable(getResources().getDrawable(R.drawable.natural_wall));
            viewFlipper.addView(imageView);

            viewFlipper.setAutoStart(true);
            viewFlipper.setFlipInterval(3000);
        }
        return slideshowView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(getActivity().getActionBar() != null) {
            getActivity().getActionBar().hide();
        }
    }

    @Override
    public void onResume () {
        super.onResume();
        Toast.makeText(getActivity(), "Press back to exit", Toast.LENGTH_SHORT).show();
        viewFlipper.startFlipping();
    }
}
