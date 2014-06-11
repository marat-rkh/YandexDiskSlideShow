package ru.yandexphoto;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorDescription;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Toast;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainAuthActivity extends FragmentActivity {

    private static final String LOG_TAG = "ExampleActivity";

    private static final int GET_ACCOUNT_CREDS_INTENT = 100;

    public static final String CLIENT_ID = "da724839add94dd39d3f5dbcdf45217d";
    public static final String CLIENT_SECRET = "852d8d46d7a349f0a55e96c99749c91d";

    public static final String ACCOUNT_TYPE = "com.yandex";
    public static final String AUTH_URL = "https://oauth.yandex.ru/authorize?response_type=token&client_id=" + CLIENT_ID;
    private static final String ACTION_ADD_ACCOUNT = "com.yandex.intent.ADD_ACCOUNT";
    private static final String KEY_CLIENT_SECRET = "clientSecret";

    public static final String USERNAME_ENTRY = "example.username";
    public static final String TOKEN_ENTRY = "example.token";
    private static final String USERNAME_VALUE = "";


    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null && getIntent().getData() != null) {
            onLogin();
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String token = preferences.getString(TOKEN_ENTRY, null);
        if (token == null) {
            getToken();
            return;
        }

        if (savedInstanceState == null) {
            startFileListFragment();
        }
    }

    private void startFileListFragment() {
        getSupportFragmentManager()
        .beginTransaction()
        .replace(android.R.id.content, new FileListFragment(), FileListFragment.FRAGMENT_TAG)
        .commit();
    }

    private void onLogin () {
        Uri data = getIntent().getData();
        setIntent(null);
        Pattern pattern = Pattern.compile("access_token=(.*?)(&|$)");
        Matcher matcher = pattern.matcher(data.toString());
        if (matcher.find()) {
            final String token = matcher.group(1);
            if (!TextUtils.isEmpty(token)) {
                Log.d(LOG_TAG, "onLogin: token: " + token);
                saveToken(token);
            } else {
                Log.w(LOG_TAG, "onRegistrationSuccess: empty token");
            }
        } else {
            Log.w(LOG_TAG, "onRegistrationSuccess: token not found in return url");
        }
    }

    private void saveToken(String token) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(USERNAME_ENTRY, USERNAME_VALUE);
        editor.putString(TOKEN_ENTRY, token);
        editor.commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GET_ACCOUNT_CREDS_INTENT) {
            if (resultCode == RESULT_OK) {
                Bundle bundle = data.getExtras();
                String name = bundle.getString(AccountManager.KEY_ACCOUNT_NAME);
                String type = bundle.getString(AccountManager.KEY_ACCOUNT_TYPE);
                Log.d(LOG_TAG, "GET_ACCOUNT_CREDS_INTENT: name=" + name + " type="+type);
                Account account = new Account(name, type);
                getAuthToken(account);
            }
        }
    }

    private void getAuthToken(Account account) {
        AccountManager systemAccountManager = AccountManager.get(getApplicationContext());
        Bundle options = new Bundle();
        options.putString(KEY_CLIENT_SECRET, CLIENT_SECRET);
        systemAccountManager.getAuthToken(account, CLIENT_ID, options, this, new GetAuthTokenCallback(), null);
    }

    private void getToken() {
        AccountManager accountManager = AccountManager.get(getApplicationContext());
        Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);
        Log.d(LOG_TAG, "accounts: " + (accounts != null ? accounts.length : null));

        if (accounts != null && accounts.length > 0) {
            // get the first account, for example (you must show the list and allow user to choose)
            Account account = accounts[0];
            Log.d(LOG_TAG, "account: " + account);
            getAuthToken(account);
            return;
        }

        Log.d(LOG_TAG, "No such accounts: " + ACCOUNT_TYPE);
        for (AuthenticatorDescription authDesc : accountManager.getAuthenticatorTypes()) {
            if (ACCOUNT_TYPE.equals(authDesc.type)) {
                Log.d(LOG_TAG, "Starting " + ACTION_ADD_ACCOUNT);
                Intent intent = new Intent(ACTION_ADD_ACCOUNT);
                startActivityForResult(intent, GET_ACCOUNT_CREDS_INTENT);
                return;
            }
        }

        // no account manager for com.yandex
        new AuthDialogFragment().show(getSupportFragmentManager(), "auth");
    }

    private class GetAuthTokenCallback implements AccountManagerCallback<Bundle> {
        public void run(AccountManagerFuture<Bundle> result) {
            try {
                Bundle bundle = result.getResult();
                Log.d(LOG_TAG, "bundle: " + bundle);

                String message = (String) bundle.get(AccountManager.KEY_ERROR_MESSAGE);
                if (message != null) {
                    Toast.makeText(MainAuthActivity.this, message, Toast.LENGTH_LONG).show();
                }

                Intent intent = (Intent) bundle.get(AccountManager.KEY_INTENT);
                Log.d(LOG_TAG, "intent: " + intent);
                if (intent != null) {
                    // User input required
                    startActivityForResult(intent, GET_ACCOUNT_CREDS_INTENT);
                } else {
                    String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                    Log.d(LOG_TAG, "GetAuthTokenCallback: token="+token);
                    saveToken(token);
                    startFileListFragment();
                }
            } catch (OperationCanceledException ex) {
                Log.d(LOG_TAG, "GetAuthTokenCallback", ex);
                Toast.makeText(MainAuthActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
            } catch (AuthenticatorException ex) {
                Log.d(LOG_TAG, "GetAuthTokenCallback", ex);
                Toast.makeText(MainAuthActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
            } catch (IOException ex) {
                Log.d(LOG_TAG, "GetAuthTokenCallback", ex);
                Toast.makeText(MainAuthActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    public static class AuthDialogFragment extends DialogFragment {

        public AuthDialogFragment () {
            super();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle("Auth required")
                    .setMessage("Confirm?")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick (DialogInterface dialog, int which) {
                            dialog.dismiss();
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(AUTH_URL)));
                        }
                    })
                    .setNegativeButton("No no!", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick (DialogInterface dialog, int which) {
                            dialog.dismiss();
                            getActivity().finish();
                        }
                    })
                    .create();
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
}