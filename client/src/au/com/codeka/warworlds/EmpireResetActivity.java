package au.com.codeka.warworlds;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import au.com.codeka.warworlds.ctrl.TransparentWebView;

/**
 * This activity is shown when we're notified by the server that the player's empire was
 * reset. Usually that's because their last colony was destroyed.
 */
public class EmpireResetActivity extends BaseActivity {
    private Context mContext = this;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar
        setContentView(R.layout.warm_welcome);

        View rootView = findViewById(android.R.id.content);
        ActivityBackgroundGenerator.setBackground(rootView);

        TransparentWebView welcome = (TransparentWebView) findViewById(R.id.welcome);
        String msg;

        String reason = getIntent().getStringExtra("au.com.codeka.warworlds.ResetReason");
        if (reason == null) {
            msg = TransparentWebView.getHtmlFile(this, "html/empire-reset.html");
        } else if (reason.equals("as-requested")) {
            msg = TransparentWebView.getHtmlFile(this, "html/empire-reset-requested.html");
        } else {
            msg = TransparentWebView.getHtmlFile(this, "html/empire-reset-reason.html");
            msg = String.format(msg, reason);
        }
        welcome.loadHtml("html/skeleton.html", msg);

        Button startBtn = (Button) findViewById(R.id.start_btn);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // now we can move to the WarWorlds activity again and get started.
                finish();
                startActivity(new Intent(mContext, WarWorldsActivity.class));
            }
        });
    }
}