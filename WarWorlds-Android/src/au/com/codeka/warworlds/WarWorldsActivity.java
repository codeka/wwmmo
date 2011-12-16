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
import org.restlet.engine.Engine;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
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
import au.com.codeka.warworlds.game.StarfieldActivity;
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

    private String mMotd = null;

    /**
     * Begins the activity.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "War Worlds is starting up...");
        super.onCreate(savedInstanceState);

        // IPv4 for now (restlet works better like this)
        System.setProperty("java.net.preferIPv6Addresses", "false");

        Engine.getInstance().getRegisteredClients().clear();
        Engine.getInstance().getRegisteredClients().add(new MyHttpClientHelper(null));

        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar

        if (savedInstanceState != null) {
            mMotd = savedInstanceState.getString("motd");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("motd", mMotd);

        super.onSaveInstanceState(outState);
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

        if (mMotd != null) {
            motd.loadData(mMotd, "text/html", "utf-8");
            return;
        }

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

                mMotd = result;
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
                startActivity(new Intent(mContext, StarfieldActivity.class));
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
