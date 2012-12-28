package au.com.codeka.warworlds;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import au.com.codeka.warworlds.ctrl.TransparentWebView;

/**
 * This activity is shown the first time you start the game. We give you a
 * quick intro, some links to the website and stuff like that.
 */
public class WarmWelcomeActivity extends BaseActivity {
    private Context mContext = this;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar
        setContentView(R.layout.warm_welcome);

        View rootView = findViewById(android.R.id.content);
        ActivityBackgroundGenerator.setBackground(rootView);

        TransparentWebView welcome = (TransparentWebView) findViewById(R.id.welcome);
        String msg = TransparentWebView.getHtmlFile(this, "html/warm-welcome.html");
        welcome.loadHtml("html/skeleton.html", msg);

        Button startBtn = (Button) findViewById(R.id.start_btn);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // save the fact that we've finished the warm welcome
                SharedPreferences prefs = Util.getSharedPreferences(mContext);
                prefs.edit().putBoolean("WarmWelcome", true).apply();

                // this activity is finished, move to the AccountActivity
                finish();
                startActivity(new Intent(mContext, AccountsActivity.class));
            }
        });
    }
}
