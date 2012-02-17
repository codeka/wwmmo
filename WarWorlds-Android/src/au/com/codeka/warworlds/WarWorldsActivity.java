
package au.com.codeka.warworlds;

import java.io.InputStream;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;

import warworlds.Warworlds.MessageOfTheDay;

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
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.game.StarfieldActivity;

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

        // initialize the Util class
        Util.loadSettings(mContext, this);

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
    private void loadMotdAndVerifyAccount(final WebView motdView) {
        motdView.setBackgroundColor(0x0); // transparent...

        if (mMotd != null) {
            motdView.loadData(mMotd, "text/html", "utf-8");
            return;
        }

        final ProgressDialog pleaseWaitDialog = ProgressDialog.show(mContext, null, 
                "Connecting...", true);

        // if we've saved off the authentication cookie, cool!
        SharedPreferences prefs = Util.getSharedPreferences(mContext);
        final String accountName = prefs.getString(Util.ACCOUNT_NAME, null);
        if (accountName == null) {
            // TODO error!
        }

        new AsyncTask<Void, Void, String>() {
            private String message;

            @Override
            protected String doInBackground(Void... arg0) {
                // re-authenticate and get a new cookie
                String authCookie = Authenticator.authenticate(mContext, WarWorldsActivity.this,
                        accountName);
                ApiClient.getCookies().add(authCookie);

                // fetch the message of the day (basically, a test of our new cookie)
                MessageOfTheDay motd = ApiClient.getProtoBuf("motd", MessageOfTheDay.class);
                if (motd == null) {
                    message = "<pre>Try logging in and out again.</pre>";
                } else {
                    message = motd.getMessage();
                }

                String tmpl = getHtmlFile("motd-template.html");
                return String.format(tmpl, message);
            }

            @Override
            protected void onPostExecute(String result) {
                pleaseWaitDialog.dismiss();
                motdView.loadData(result, "text/html", "utf-8");
                motdView.setBackgroundColor(0x0); // transparent...

                mMotd = result;
            }
        }.execute();
    }

    private void setHomeScreenContent() {
        setContentView(R.layout.home);

        final Button startGameButton = (Button) findViewById(R.id.start_game);
        final Button logOutButton = (Button) findViewById(R.id.log_out);

        final WebView motdView = (WebView) findViewById(R.id.motd);
        loadMotdAndVerifyAccount(motdView);

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
