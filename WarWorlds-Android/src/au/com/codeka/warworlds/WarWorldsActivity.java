
package au.com.codeka.warworlds;

import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ConnectException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import warworlds.Warworlds.Hello;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.WebView;
import android.widget.Button;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.game.EmpireManager;
import au.com.codeka.warworlds.game.StarfieldActivity;
import au.com.codeka.warworlds.model.Empire;

/**
 * Main activity - requests "Hello, World" messages from the server and provides
 * a menu item to invoke the accounts activity.
 */
public class WarWorldsActivity extends Activity {
    private static Logger log = LoggerFactory.getLogger(WarWorldsActivity.class);
    private Context mContext = this;

    /**
     * Begins the activity.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        log.info("WarWorlds activity starting...");
        super.onCreate(savedInstanceState);

        // initialize the Util class
        Util.loadSettings(mContext, this);
        Authenticator.configure(mContext);

        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar
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
     * We don't have a menu, returning \c false makes sure the button doesn't appear.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
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
     * Says "hello" to the server. Lets it know who we are, fetches the MOTD and if there's
     * no empire registered yet, switches over to the \c EmpireSetupActivity.
     * 
     * @param motd The \c WebView we'll install the MOTD to.
     */
    private void sayHello(final WebView motdView) {
        setWebViewTransparent(motdView);

        final ProgressDialog pleaseWaitDialog = ProgressDialog.show(mContext, null, 
                "Connecting...", true);

        // if we've saved off the authentication cookie, cool!
        SharedPreferences prefs = Util.getSharedPreferences(mContext);
        final String accountName = prefs.getString(Util.ACCOUNT_NAME, null);
        if (accountName == null) {
            // TODO error!
        }

        new AsyncTask<Void, Void, String>() {
            private boolean mNeedsEmpireSetup;

            @Override
            protected String doInBackground(Void... arg0) {
                // re-authenticate and get a new cookie
                String authCookie = Authenticator.authenticate(WarWorldsActivity.this, accountName);
                ApiClient.getCookies().add(authCookie);

                // say hello to the server
                String message;
            //    try {
                    Hello hello = ApiClient.getProtoBuf("hello", Hello.class);
                    if (hello.hasEmpire()) {
                        mNeedsEmpireSetup = false;
                        EmpireManager.getInstance().setup(
                                Empire.fromProtocolBuffer(hello.getEmpire()));
                    } else {
                        mNeedsEmpireSetup = true;
                    }
                    message = hello.getMotd().getMessage();
            //    } catch(ConnectException e) {
             //       message = "<p>An error occured talking to the server, check data connection.</p>";
            //    }

                String tmpl = getHtmlFile("motd-template.html");
                return String.format(tmpl, message);
            }

            @Override
            protected void onPostExecute(String result) {
                pleaseWaitDialog.dismiss();
                if (mNeedsEmpireSetup) {
                    startActivity(new Intent(mContext, EmpireSetupActivity.class));
                } else {
                    motdView.loadData(result, "text/html", "utf-8");
                    setWebViewTransparent(motdView);
                }
            }
        }.execute();
    }
    
    private void setWebViewTransparent(WebView webView) {
        webView.setBackgroundColor(Color.TRANSPARENT);

        // this is required to make the background of the WebView actually transparent
        // on Honeycomb+ (this API is only available on Honeycomb+ as well, so we need
        // to call it via reflection...):
        // motdView.setLayerType(View.LAYER_TYPE_SOFTWARE, new Paint());
        try {
            Method setLayerType = View.class.getMethod("setLayerType", int.class, Paint.class);
            if (setLayerType != null) {
                setLayerType.invoke(webView, 1, new Paint());
          }
        // ignore if the method isn't supported on this platform...
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }
    }

    /**
     * Sets up the contents of the home screen.
     */
    private void setHomeScreenContent() {
        setContentView(R.layout.home);

        final Button startGameButton = (Button) findViewById(R.id.start_game_btn);
        final Button logOutButton = (Button) findViewById(R.id.log_out_btn);

        final WebView motd = (WebView) findViewById(R.id.home_motd);
        sayHello(motd);

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
