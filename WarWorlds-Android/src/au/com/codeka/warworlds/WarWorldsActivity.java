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

import java.io.InputStream;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.WebView;
import android.widget.Button;
import au.com.codeka.warworlds.shared.MessageOfTheDay;
import au.com.codeka.warworlds.shared.MessageOfTheDayResource;

/**
 * Main activity - requests "Hello, World" messages from the server and provides
 * a menu item to invoke the accounts activity.
 */
public class WarWorldsActivity extends Activity {
    /**
     * Tag for logging.
     */
    private static final String TAG = "WarWorldsActivity";

    /**
     * The current context.
     */
    private Context mContext = this;

    /**
     * A {@link BroadcastReceiver} to receive the response from a register or
     * unregister request, and to update the UI.
     */
    private final BroadcastReceiver mUpdateUIReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String accountName = intent.getStringExtra(DeviceRegistrar.ACCOUNT_NAME_EXTRA);
            int status = intent.getIntExtra(DeviceRegistrar.STATUS_EXTRA, DeviceRegistrar.ERROR_STATUS);
            String message = null;
            if (status == DeviceRegistrar.REGISTERED_STATUS) {
                message = getResources().getString(R.string.registration_succeeded);
            } else if (status == DeviceRegistrar.UNREGISTERED_STATUS) {
                message = getResources().getString(R.string.unregistration_succeeded);
            } else {
                message = getResources().getString(R.string.registration_error);
            }

            // Display a notification
            Util.generateNotification(mContext, String.format(message, accountName));
        }
    };

    /**
     * Begins the activity.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "War Worlds is starting up...");
        super.onCreate(savedInstanceState);

        // IPv4 for now (restlet works better like this)
        System.setProperty("java.net.preferIPv6Addresses", "false");

        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar

        // Register a receiver to provide register/unregister notifications
        registerReceiver(mUpdateUIReceiver, new IntentFilter(Util.UPDATE_UI_INTENT));
    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences prefs = Util.getSharedPreferences(mContext);
        String deviceRegistrationID = prefs.getString(Util.DEVICE_REGISTRATION_ID, null);
        if (deviceRegistrationID == null) {
            startActivity(new Intent(this, AccountsActivity.class));
            return;
        }

        setScreenContent(R.layout.home);
    }

    /**
     * Shuts down the activity.
     */
    @Override
    public void onDestroy() {
        unregisterReceiver(mUpdateUIReceiver);
        super.onDestroy();
    }

    /**
     * Loads the MOTD template HTML, which is actually just a static asset.
     */
    private String getHtmlFile(String fileName) {
        try {
            AssetManager assetManager = mContext.getAssets();
            InputStream is = assetManager.open("html/"+fileName);
            
            StringWriter writer = new StringWriter();
            IOUtils.copy(is, writer);
            return writer.toString();
        } catch (Exception e) {
        	// any errors (shouldn't be...) and we'll return a "blank" template.
            return "";
        }
    }
    
    /**
     * Loads the MOTD. This also checks that we're correctly registered on the server. If
     * we're not correctly registered, then we can direct them to the \c AccountPropertiesActivity.
     * 
     * @param motd The \c WebView we'll install the MOTD to.
     */
    private void loadMotdAndVerifyAccount(final WebView motd) {
		motd.setBackgroundColor(0x0); // transparent...

		final ProgressDialog pleaseWaitDialog = ProgressDialog.show(mContext, null, 
				"Connecting...", true);
		
        new AsyncTask<Void, Void, String>() {
        	private String message;

        	@Override
        	protected String doInBackground(Void... arg0) {
        		try {
        	    	MessageOfTheDayResource resource = Util.getClientResource(mContext, "/motd", MessageOfTheDayResource.class);
        	    	MessageOfTheDay motd = resource.retrieve();
        	    	message = motd.getMessage();
        		} catch(Exception e) {
        			Log.e(TAG, ExceptionUtils.getStackTrace(e));
        			message = "<pre>" + e.toString() + "</pre>";
        		}

        		String tmpl = getHtmlFile("motd-template.html");
        		return String.format(tmpl, message);
        	}

        	@Override
        	protected void onPostExecute(String result) {
        		pleaseWaitDialog.dismiss();
        		motd.loadData(result, "text/html", "utf-8");
        		motd.setBackgroundColor(0x0); // transparent...
        	}
        }.execute();
    }
    
    // Manage UI Screens

    private void setHomeScreenContent() {
        setContentView(R.layout.home);

        final Button startGameButton = (Button) findViewById(R.id.start_game);
        final Button logOutButton = (Button) findViewById(R.id.log_out);
        final WebView motd = (WebView) findViewById(R.id.motd);

        loadMotdAndVerifyAccount(motd);
        
        logOutButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		startActivity(new Intent(mContext, AccountsActivity.class));
        	}
        });

        startGameButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	startGameButton.setEnabled(false);

                startActivity(new Intent(mContext, GameActivity.class));
/*                
                // Use an AsyncTask to avoid blocking the UI thread
                new AsyncTask<Void, Void, String>() {
                    private String message;

                    @Override
                    protected String doInBackground(Void... arg0) {
                        MyRequestFactory requestFactory = Util.getRequestFactory(mContext,
                                MyRequestFactory.class);
                        final HelloWorldRequest request = requestFactory.helloWorldRequest();
                        Log.i(TAG, "Sending request to server");
                        request.getMessage().fire(new Receiver<String>() {
                            @Override
                            public void onFailure(ServerFailure error) {
                                message = "Failure: " + error.getMessage();
                            }

                            @Override
                            public void onSuccess(String result) {
                                message = result;
                            }
                        });
                        return message;
                    }

                    @Override
                    protected void onPostExecute(String result) {
                        helloWorld.setText(result);
                        sayHelloButton.setEnabled(true);
                    }
                }.execute();
*/
            }
        });
    }

    /**
     * Sets the screen content based on the screen id.
     */
    private void setScreenContent(int screenId) {
        setContentView(screenId);
        switch (screenId) {
            case R.layout.home:
                setHomeScreenContent();
                break;
        }
    }
}
