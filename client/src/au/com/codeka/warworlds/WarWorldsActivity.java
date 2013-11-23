
package au.com.codeka.warworlds;

import java.text.DecimalFormat;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import au.com.codeka.warworlds.ctrl.TransparentWebView;
import au.com.codeka.warworlds.game.starfield.StarfieldActivity;
import au.com.codeka.warworlds.model.RealmManager;

/**
 * Main activity. Displays the message of the day and lets you select "Start Game", "Options", etc.
 */
public class WarWorldsActivity extends BaseActivity {
    private static Logger log = LoggerFactory.getLogger(WarWorldsActivity.class);
    private Context mContext = this;
    private Button mStartGameButton;
    private TextView mConnectionStatus;
    private HelloWatcher mHelloWatcher;
    private TextView mRealmName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log.info("WarWorlds activity starting...");
        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar

        setContentView(R.layout.welcome);
        Util.setup(mContext);

        View rootView = findViewById(android.R.id.content);
        ActivityBackgroundGenerator.setBackground(rootView);

        mStartGameButton = (Button) findViewById(R.id.start_game_btn);
        mConnectionStatus = (TextView) findViewById(R.id.connection_status);
        mRealmName = (TextView) findViewById(R.id.realm_name);
        final Button realmSelectButton = (Button) findViewById(R.id.realm_select_btn);
        final Button optionsButton = (Button) findViewById(R.id.options_btn);

        realmSelectButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                RealmManager.i.selectRealm(null);
                startActivity(new Intent(mContext, RealmSelectActivity.class));
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
                startActivity(intent);
            }
        });

        ((Button) findViewById(R.id.help_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("http://www.war-worlds.com/doc/getting-started"));
                startActivity(i);
            }
        });

        ((Button) findViewById(R.id.website_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("http://www.war-worlds.com/"));
                startActivity(i);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        log.debug("WarWorldsActivity.onResume...");

        SharedPreferences prefs = Util.getSharedPreferences();
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

        if (RealmContext.i.getCurrentRealm() == null) {
            log.info("No realm selected, switching to RealmSelectActivity");
            startActivity(new Intent(this, RealmSelectActivity.class));
            return;
        }

        mStartGameButton.setEnabled(false);
        mConnectionStatus.setText("Connecting...");
        mRealmName.setText(String.format(Locale.ENGLISH, "Realm: %s", RealmContext.i.getCurrentRealm().getDisplayName()));

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
                    mStartGameButton.setEnabled(true);
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
}
