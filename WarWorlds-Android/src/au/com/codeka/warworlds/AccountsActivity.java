/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package au.com.codeka.warworlds;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import au.com.codeka.warworlds.api.ApiClient;

import com.google.android.gcm.GCMRegistrar;

/**
 * Account selections activity - handles device registration and unregistration.
 */
public class AccountsActivity extends BaseActivity {
    final Logger log = LoggerFactory.getLogger(AccountsActivity.class);
    private int mAccountSelectedPosition = 0;
    private boolean mPendingAuth = false;
    private Context mContext = this;
    private ProgressDialog mPleaseWaitDialog;
    private boolean mIsLogIn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar

        View rootView = findViewById(android.R.id.content);
        ActivityBackgroundGenerator.setBackground(rootView);

        SharedPreferences prefs = Util.getSharedPreferences(mContext);
        String accountName = prefs.getString("AccountName", null);
        if (accountName == null) {
            mIsLogIn = true;
            setContentView(R.layout.log_in);

            final ListView listView = (ListView) findViewById(R.id.select_account);
            final Button logInButton = (Button) findViewById(R.id.log_in_btn);
            logInButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    // Get account name
                    mAccountSelectedPosition = listView.getCheckedItemPosition();
                    TextView account = (TextView) listView.getChildAt(mAccountSelectedPosition);

                    // Register
                    register((String) account.getText(), new Callable<Void>() {
                        public Void call() {
                            log.debug("Authentication complete.");

                            if (mPleaseWaitDialog != null) {
                                mPleaseWaitDialog.dismiss();
                            }

                            finish();
                            return null;
                        }
                    });
                }
            });
        } else {
            mIsLogIn = false;
            setContentView(R.layout.log_out);

            final Button logOutButton = (Button) findViewById(R.id.log_out_btn);
            logOutButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    // Unregister
                    unregister(new Callable<Void>() {
                        public Void call() {
                            if (mPleaseWaitDialog != null) {
                                mPleaseWaitDialog.dismiss();
                            }

                            finish();
                            return null;
                        }
                    });
                }
            });
        }

        final Button cancelButton = (Button) findViewById(R.id.cancel_btn);
        cancelButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPendingAuth) {
            mPendingAuth = false;
            String regId = GCMRegistrar.getRegistrationId(mContext);
            if (regId != null && ! "".equals(regId)) {
                DeviceRegistrar.register(mContext, regId);
            } else {
                GCMIntentService.register(this, null);
            }
        }

        if (mIsLogIn) {
            setLogInScreenContent();
        } else {
            setLogOutScreenContent();
        }
    }

    private void setLogInScreenContent() {
        List<String> accounts = getGoogleAccounts();
        if (accounts.size() == 0) {
            // Show a dialog and invoke the "Add Account" activity if requested
            StyledDialog.Builder builder = new StyledDialog.Builder(mContext);
            builder.setMessage("You need a Google Account in order to be able to play War Worlds.");
            builder.setPositiveButton("Add Account", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(Settings.ACTION_ADD_ACCOUNT));
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // TODO: the whole game should exit...
                    finish();
                }
            });
            builder.setTitle("No Google Account");
            builder.create().show();
        } else {
            ListView listView = (ListView) findViewById(R.id.select_account);
            listView.setAdapter(new ArrayAdapter<String>(mContext, R.layout.account, accounts));
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            listView.setItemChecked(mAccountSelectedPosition, true);
        }
    }

    /**
     * Sets up the 'disconnected' screen.
     */
    private void setLogOutScreenContent() {
        final SharedPreferences prefs = Util.getSharedPreferences(mContext);
        String accountName = prefs.getString("AccountName", "Unknown");

        // Format the disconnect message with the currently connected account name
        TextView logOutMsg = (TextView) findViewById(R.id.log_out_msg);
        String message = getResources().getString(R.string.log_out_msg);
        String formatted = String.format(message, accountName);
        logOutMsg.setText(formatted);
    }

    /**
     * Registers for C2DM messaging with the given account name.
     * 
     * @param accountName a String containing a Google account name
     */
    private void register(final String accountName, final Callable<Void> onComplete) {
        // Store the account name in shared preferences
        final SharedPreferences prefs = Util.getSharedPreferences(mContext);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("AccountName", accountName);
        editor.commit();

        log.info("Registering \"{}\"...", accountName);
        mPleaseWaitDialog = ProgressDialog.show(mContext, null, "Logging in...", true);

        // Obtain an auth token and register, on a background thread
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... arg0) {
                String authCookie = Authenticator.authenticate(AccountsActivity.this, accountName);
                ApiClient.getCookies().clear();
                ApiClient.getCookies().add(authCookie);
                GCMIntentService.register(AccountsActivity.this, onComplete);

                // re-configure the authenticator, making sure it has details of the new user.
                Authenticator.configure(mContext);

                return null;
            }
        }.execute();
    }

    private void unregister(final Callable<Void> onComplete) {
        mPleaseWaitDialog = ProgressDialog.show(mContext, null, "Logging out...", true);
        GCMIntentService.unregister(this, new Callable<Void>() {
            public Void call() {
                final SharedPreferences prefs = Util.getSharedPreferences(mContext);
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove("AccountName");
                editor.commit();

                try {
                    onComplete.call();
                } catch(Exception e) {
                }
                return null;
            }
        });
    }

    /**
     * Returns a list of registered Google account names. If no Google accounts
     * are registered on the device, a zero-length list is returned.
     */
    private List<String> getGoogleAccounts() {
        ArrayList<String> result = new ArrayList<String>();
        Account[] accounts = AccountManager.get(mContext).getAccounts();
        for (Account account : accounts) {
            if (account.type.equals("com.google")) {
                result.add(account.name);
            }
        }

        return result;
    }
}
