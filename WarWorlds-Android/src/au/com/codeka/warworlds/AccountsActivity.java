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
import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import au.com.codeka.warworlds.api.ApiClient;

import com.google.android.c2dm.C2DMessaging;

/**
 * Account selections activity - handles device registration and unregistration.
 */
public class AccountsActivity extends Activity {
    final Logger log = LoggerFactory.getLogger(AccountsActivity.class);

    /**
     * The selected position in the ListView of accounts.
     */
    private int mAccountSelectedPosition = 0;

    /**
     * True if we are waiting for App Engine authorisation.
     */
    private boolean mPendingAuth = false;

    /**
     * The current context.
     */
    private Context mContext = this;

    private ProgressDialog mPleaseWaitDialog;
    
    /**
     * Begins the activity.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = Util.getSharedPreferences(mContext);
        String deviceRegistrationID = prefs.getString(Util.DEVICE_REGISTRATION_ID, null);
        if (deviceRegistrationID == null) {
            // Show the 'connect' screen if we are not connected
            setScreenContent(R.layout.log_in);
        } else {
            // Show the 'log out' screen if we are logged in
            setScreenContent(R.layout.log_out);
        }
    }

    /**
     * Resumes the activity.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (mPendingAuth) {
            mPendingAuth = false;
            String regId = C2DMessaging.getRegistrationId(mContext);
            if (regId != null && ! "".equals(regId)) {
                DeviceRegistrar.register(mContext, regId);
            } else {
                C2DMessaging.register(mContext, Setup.SENDER_ID);
            }
        }
    }

    // Manage UI Screens

    /**
     * Sets up the 'log in' screen content.
     */
    private void setLogInScreenContent() {
        List<String> accounts = getGoogleAccounts();
        if (accounts.size() == 0) {
            // Show a dialog and invoke the "Add Account" activity if requested
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setMessage(R.string.needs_account);
            builder.setPositiveButton(R.string.add_account, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(Settings.ACTION_ADD_ACCOUNT));
                }
            });
            builder.setNegativeButton(R.string.skip, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            builder.setIcon(android.R.drawable.stat_sys_warning);
            builder.setTitle(R.string.attention);
            builder.show();
        } else {
            final ListView listView = (ListView) findViewById(R.id.select_account);
            listView.setAdapter(new ArrayAdapter<String>(mContext, R.layout.account, accounts));
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            listView.setItemChecked(mAccountSelectedPosition, true);

            final Button logInButton = (Button) findViewById(R.id.log_in_btn);
            logInButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    // Get account name
                    mAccountSelectedPosition = listView.getCheckedItemPosition();
                    TextView account = (TextView) listView.getChildAt(mAccountSelectedPosition);

                    // Register
                    register((String) account.getText(), new Callable<Void>() {
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
    }

    /**
     * Sets up the 'disconnected' screen.
     */
    private void setLogOutScreenContent() {
        final SharedPreferences prefs = Util.getSharedPreferences(mContext);
        String accountName = prefs.getString(Util.ACCOUNT_NAME, "Unknown");

        // Format the disconnect message with the currently connected account name
        TextView logOutMsg = (TextView) findViewById(R.id.log_out_msg);
        String message = getResources().getString(R.string.log_out_msg);
        String formatted = String.format(message, accountName);
        logOutMsg.setText(formatted);

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

    /**
     * Sets the screen content based on the screen id.
     */
    private void setScreenContent(int screenId) {
        setContentView(screenId);
        switch (screenId) {
            case R.layout.log_out:
                setLogOutScreenContent();
                break;
            case R.layout.log_in:
                setLogInScreenContent();
                break;
        }
    }

    // Register and Unregister

    /**
     * Registers for C2DM messaging with the given account name.
     * 
     * @param accountName a String containing a Google account name
     */
    private void register(final String accountName, final Callable<Void> onComplete) {
        // Store the account name in shared preferences
        final SharedPreferences prefs = Util.getSharedPreferences(mContext);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Util.ACCOUNT_NAME, accountName);
        editor.remove(Util.DEVICE_REGISTRATION_ID);
        editor.commit();

        log.info("Registering \"{}\"...", accountName);
        mPleaseWaitDialog = ProgressDialog.show(mContext, null, "Logging in...", true);

        // Obtain an auth token and register, on a background thread
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... arg0) {
                String authCookie = Authenticator.authenticate(mContext, AccountsActivity.this,
                        accountName);
                ApiClient.getCookies().add(authCookie);
                C2DMReceiver.register(AccountsActivity.this, Setup.SENDER_ID, onComplete);

                return null;
            }
        }.execute();
    }

    private void unregister(final Callable<Void> onComplete) {
        mPleaseWaitDialog = ProgressDialog.show(mContext, null, "Logging out...", true);

    	C2DMReceiver.unregister(this, onComplete);
    }

    // Utility Methods

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
