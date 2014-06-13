package ru.yandexphoto;

import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.yandex.disk.client.Credentials;
import com.yandex.disk.client.ListItem;

import java.util.ArrayList;
import java.util.List;

public class FileListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<List<ListItem>> {

    private final String LOG_TAG = "ListExampleFragment";
    public static final String FRAGMENT_TAG = "FileList";

    private final String CURRENT_DIR_KEY = "current.dir.key";
    private final String ROOT_DIR = "/";

    private final String LOADING_MSG = "Loading files list...";
    private final String ERROR_LOADING_MSG = "Sorry, can't load files list";

    private Credentials credentials;
    private String currentDir = ROOT_DIR;

    private FileListAdapter fileListAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setCredentials();
        setEmptyText(LOADING_MSG);
        setHasOptionsMenu(true);
        setCurrentDirIfNeeded();
        if(getActivity().getActionBar() != null) {
            getActivity().getActionBar().show();
            getActivity().getActionBar().setDisplayHomeAsUpEnabled(!ROOT_DIR.equals(currentDir));
        }
        initFileListLoading();
    }

    private void setCredentials() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String username = preferences.getString(MainAuthActivity.USERNAME_ENTRY, null);
        String token = preferences.getString(MainAuthActivity.TOKEN_ENTRY, null);
        credentials = new Credentials(username, token);
    }

    private void setCurrentDirIfNeeded() {
        Bundle args = getArguments();
        if (args != null) {
            currentDir = args.getString(CURRENT_DIR_KEY);
        }
    }

    private void initFileListLoading() {
        fileListAdapter = new FileListAdapter(getActivity());
        setListAdapter(fileListAdapter);
        setListShown(false);
        getLoaderManager().initLoader(0, null, this);
    }

    // action bar methods overriding
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                getActivity().getSupportFragmentManager().popBackStack();
                break;
            case R.id.start_slideshow:
                launchSlideShow();
                break;
            case R.id.refresh_list:
                setListShown(false);
                getLoaderManager().restartLoader(0, null, this);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void launchSlideShow() {
        ArrayList<String> imagePaths = getImagesPaths();
        Bundle args = new Bundle();
        args.putParcelable(SlideShowFragment.CREDENTIALS_ARG, credentials);
        args.putStringArrayList(SlideShowFragment.IMG_PATHS_ARG, imagePaths);
        Fragment slideShowFragment = new SlideShowFragment();
        slideShowFragment.setArguments(args);
        replaceFragment(slideShowFragment, SlideShowFragment.FRAGMENT_TAG);
    }

    private ArrayList<String> getImagesPaths() {
        ArrayList<String> paths = new ArrayList<String>();
        for (int i = 0; i < fileListAdapter.getCount(); i++) {
            ListItem listItem = fileListAdapter.getItem(i);
            if(!listItem.isCollection() && listItem.getContentType().startsWith("image")) {
                paths.add(listItem.getFullPath());
            }
        }
        return paths;
    }

    // LoaderCallbacks methods overriding
    @Override
    public Loader<List<ListItem>> onCreateLoader(int i, Bundle bundle) {
        return new FileListLoader(getActivity(), credentials, currentDir);
    }

    @Override
    public void onLoadFinished(final Loader<List<ListItem>> loader, List<ListItem> data) {
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
        if (data.isEmpty()) {
            String errorMessage = ((FileListLoader) loader).getLastErrorMessage();
            if (errorMessage != null) {
                Log.d(LOG_TAG, "FileListLoader exception: " + errorMessage);
            }
            setEmptyText(ERROR_LOADING_MSG);
        } else {
            fileListAdapter.setData(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<ListItem>> loader) {
        fileListAdapter.setData(null);
    }

    //ListFragment methods overriding
    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        ListItem item = (ListItem) getListAdapter().getItem(position);
        if (item.isCollection()) {
            changeDir(item.getFullPath());
        } else {
            Toast.makeText(getActivity(), "Not a directory", Toast.LENGTH_SHORT).show();
        }
    }

    private void changeDir(String dir) {
        Bundle args = new Bundle();
        args.putString(CURRENT_DIR_KEY, dir);

        FileListFragment fragment = new FileListFragment();
        fragment.setArguments(args);
        replaceFragment(fragment, FRAGMENT_TAG);
    }

    private void replaceFragment(Fragment fragment, String fragmentTag) {
        getActivity().getSupportFragmentManager()
        .beginTransaction()
        .replace(android.R.id.content, fragment, fragmentTag)
        .addToBackStack(fragmentTag)
        .commit();
    }

    private static class FileListAdapter extends ArrayAdapter<ListItem> {
        private final LayoutInflater inflater;

        public FileListAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_2);
            inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setData(List<ListItem> data) {
            clear();
            if (data != null) {
                addAll(data);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if(convertView == null) {
                convertView = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.textView = (TextView)convertView.findViewById(android.R.id.text1);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder)convertView.getTag();
            }
            viewHolder.textView.setText(getItem(position).getDisplayName());
            if(getItem(position).isCollection()) {
                viewHolder.textView.setTypeface(null, Typeface.BOLD);
            } else {
                viewHolder.textView.setTypeface(null, Typeface.NORMAL);
            }
            return convertView;
        }

        private static class ViewHolder {
            public TextView textView;
        }
    }
}