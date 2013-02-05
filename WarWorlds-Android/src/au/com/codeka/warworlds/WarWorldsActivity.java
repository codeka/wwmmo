
package au.com.codeka.warworlds;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import au.com.codeka.warworlds.ctrl.TransparentWebView;
import au.com.codeka.warworlds.game.starfield.StarfieldActivity;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.StarSummary;

/**
 * Main activity. Displays the message of the day and lets you select "Start Game", "Options", etc.
 */
public class WarWorldsActivity extends BaseActivity {
    private static Logger log = LoggerFactory.getLogger(WarWorldsActivity.class);
    private Context mContext = this;
    private Button mStartGameButton;
    private TextView mConnectionStatus;
    private String mStarKey;
    private HelloWatcher mHelloWatcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log.info("WarWorlds activity starting...");
        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar

        setHomeScreenContent();
    }

    @Override
    public void onResume() {
        super.onResume();
        log.debug("WarWorldsActivity.onResume...");

        SharedPreferences prefs = Util.getSharedPreferences(mContext);
        if (prefs.getBoolean("WarmWelcome",  false) == false) {
            // if we've never done the warm-welcome, do it now
            log.info("Starting Warm Welcome");
            startActivity(new Intent(this, WarmWelcomeActivity.class));
            return;
        }

        if (prefs.getString("AccountName", null) == null) {
            log.info("No accountName saved, switching to AccountsActivity");
            startActivity(new Intent(this, AccountsActivity.class));
            return;
        }

        mStartGameButton.setEnabled(false);
        mConnectionStatus.setText("Connecting...");

        mHelloWatcher = new HelloWatcher();
        ServerGreeter.addHelloWatcher(mHelloWatcher);

        ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
            @Override
            public void onHelloComplete(boolean success, ServerGreeter.ServerGreeting greeting) {
                if (success) {
                    TransparentWebView motdView = (TransparentWebView) findViewById(R.id.motd);

                    // we'll display a bit of debugging info along with the 'connected' message
                    long maxMemoryBytes = Runtime.getRuntime().maxMemory();
                    int memoryClass = ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).getMemoryClass();
                    PackageInfo packageInfo = null;
                    try {
                        packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                    } catch (NameNotFoundException e) {
                    }

                    DecimalFormat formatter = new DecimalFormat("#,##0");
                    String msg = String.format(Locale.ENGLISH,
                                               "Connected\r\nMemory Class: %d - Max bytes: %s\r\nVersion: %s%s",
                                               memoryClass,
                                               formatter.format(maxMemoryBytes),
                                               packageInfo == null ? "Unknown" : packageInfo.versionName,
                                               Util.isDebug() ? " (debug)" : " (rel)");
                    mConnectionStatus.setText(msg);

                    motdView.loadHtml("html/skeleton.html", greeting.getMessageOfTheDay());
                    findColony(greeting.getColonies());
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        ServerGreeter.removeHelloWatcher(mHelloWatcher);
    }

    private class HelloWatcher implements ServerGreeter.HelloWatcher {
        @Override
        public void onRetry(final int retries) {
            mConnectionStatus.setText(String.format("Retrying (#%d)...", retries+1));
        }
    }

    /**
     * Says "hello" to the server. Lets it know who we are, fetches the MOTD and if there's
     * no empire registered yet, switches over to the \c EmpireSetupActivity.
     * 
     * @param motd The \c WebView we'll install the MOTD to.
     */

    private void findColony(List<Colony> colonies) {
        // we'll want to start off near one of your stars. If you
        // only have one, that's easy -- but if you've got lots
        // what then?
        mStarKey = null;
        if (colonies == null) {
            return;
        }
        for (Colony c : colonies) {
            mStarKey = c.getStarKey();
        }

        if (mStarKey != null) {
            mStartGameButton.setEnabled(false);
            StarManager.getInstance().requestStarSummary(mContext, mStarKey,
                    new StarManager.StarSummaryFetchedHandler() {
                @Override
                public void onStarSummaryFetched(StarSummary s) {
                    mStartGameButton.setEnabled(true);

                    // we don't do anything with the star, we just want
                    // to make sure it's in the cache before we start
                    // the activity. Now the start button is ready to go!
                    mStartGameButton.setEnabled(true);

                    boolean showSituationReport = getIntent().getBooleanExtra("au.com.codeka.warworlds.ShowSituationReport", false);
                    if (showSituationReport) {
                        Intent intent = new Intent(mContext, StarfieldActivity.class);
                        intent.putExtra("au.com.codeka.warworlds.StarKey", mStarKey);
                        intent.putExtra("au.com.codeka.warworlds.ShowSituationReport", true);
                        startActivity(intent);
                    }
                }
            });
        } else {
            mStartGameButton.setEnabled(true);
        }
    }

    private void setHomeScreenContent() {
        setContentView(R.layout.home);

        View rootView = findViewById(android.R.id.content);
        ActivityBackgroundGenerator.setBackground(rootView);

        mStartGameButton = (Button) findViewById(R.id.start_game_btn);
        mConnectionStatus = (TextView) findViewById(R.id.connection_status);
        final Button logOutButton = (Button) findViewById(R.id.log_out_btn);
        final Button optionsButton = (Button) findViewById(R.id.options_btn);

        logOutButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(mContext, AccountsActivity.class));
            }
        });

        optionsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mContext, GlobalOptionsActivity.class));
            }
        });

        mStartGameButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                final Intent intent = new Intent(mContext, StarfieldActivity.class);
                intent.putExtra("au.com.codeka.warworlds.StarKey", mStarKey);
                startActivity(intent);
            }
        });
    }
}
