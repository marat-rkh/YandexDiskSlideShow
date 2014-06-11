package ru.yandexphoto;

import android.preference.PreferenceManager;
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

import java.util.List;

public class FileListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<List<ListItem>> {

    private static final String LOG_TAG = "ListExampleFragment";
    public static final String FRAGMENT_TAG = "FileList";

    private static final String CURRENT_DIR_KEY = "example.current.dir";
    private static final String ROOT_DIR = "/";

    private Credentials credentials;
    private String currentDir = ROOT_DIR;

    private FileListAdapter fileListAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setCredentials();
        setDefaultEmptyText();
        setHasOptionsMenu(true);
        setCurrentDirIfNeeded();

        if(getActivity().getActionBar() != null) {
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

    private void setDefaultEmptyText() {
        setEmptyText(getString(R.string.no_files));
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
//
//    public void restartLoader() {
//        getLoaderManager().restartLoader(0, null, this);
//    }

//    @Override
//    public void onCreateContextMenu(ContextMenu main_menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
//        super.onCreateContextMenu(main_menu, v, menuInfo);
//
////        main_menu.setHeaderTitle(getListItem(menuInfo).getDisplayName());
//
////        MenuInflater inflater = getActivity().getMenuInflater();
////        inflater.inflate(R.main_menu.example_context_menu, main_menu);
//    }

//    @Override
//    public boolean onContextItemSelected(MenuItem item) {
//        ListItem listItem = getListItem(item.getMenuInfo());
//        switch (item.getItemId()) {
//            case R.id.example_context_publish:
//                Log.d(LOG_TAG, "onContextItemSelected: publish: listItem="+listItem);
//                if (listItem.getPublicUrl() != null) {
//                    ShowPublicUrlDialogFragment.newInstance(credentials, listItem).show(getFragmentManager(), "showPublicUrlDialog");
//                } else {
//                    MakeItemPublicFragment.newInstance(credentials, listItem.getFullPath(), true).show(getFragmentManager(), "makeItemPublic");
//                }
//                return true;
//            case R.id.example_context_move:
//                RenameMoveDialogFragment.newInstance(credentials, listItem).show(getFragmentManager(), "renameMoveDialog");
//                return true;
//            case R.id.example_context_delete:
//                DeleteItemDialogFragment.newInstance(credentials, listItem).show(getFragmentManager(), "deleteItemDialog");
//                return true;
//            default:
//                return super.onContextItemSelected(item);
//        }
//    }

//    private ListItem getListItem(ContextMenu.ContextMenuInfo menuInfo) {
//        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
//        return (ListItem) getListAdapter().getItem(info.position);
//    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                getFragmentManager().popBackStack();
                break;
//            case R.id.example_add_file:
//                openAddFileDialog();
//                break;
//            case R.id.example_make_folder:
//                MakeFolderDialogFragment.newInstance(credentials, currentDir).show(getFragmentManager(), "makeFolderName");
//                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

//    private void openAddFileDialog() {
//        Intent intent = new Intent();
//        intent.setAction(Intent.ACTION_GET_CONTENT);
//        intent.setType("*/*");
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        startActivityForResult(Intent.createChooser(intent, getText(R.string.example_loading_get_file_to_upload_chooser_title)), GET_FILE_TO_UPLOAD);
//    }

//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        switch (requestCode) {
//            case GET_FILE_TO_UPLOAD:
//                if (resultCode == Activity.RESULT_OK) {
//                    Uri uri = data.getData();
//                    if ("file".equalsIgnoreCase(uri.getScheme())) {
//                        uploadFile(uri.getPath());
//                    } else {
//                        Toast.makeText(getActivity(), R.string.example_get_file_unsupported_scheme, Toast.LENGTH_LONG).show();
//                    }
//                }
//                break;
//        }
//        super.onActivityResult(requestCode, resultCode, data);
//    }

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
            Exception ex = ((FileListLoader) loader).getLastException();
            if (ex != null) {
                setEmptyText(((FileListLoader) loader).getLastException().getMessage());
            } else {
                setDefaultEmptyText();
            }
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
        Log.d(LOG_TAG, "onListItemClick(): " + item);
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

        getFragmentManager()
        .beginTransaction()
        .replace(android.R.id.content, fragment, FRAGMENT_TAG)
        .addToBackStack(null)
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
            return convertView;
        }

        private static class ViewHolder {
            public TextView textView;
        }
    }
}