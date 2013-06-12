package au.com.codeka.warworlds;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import au.com.codeka.warworlds.model.Realm;
import au.com.codeka.warworlds.model.RealmManager;

public class RealmSelectActivity extends BaseActivity {
    private Context mContext = this;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar
        setContentView(R.layout.realm_select);

        View rootView = findViewById(android.R.id.content);
        ActivityBackgroundGenerator.setBackground(rootView);

        Util.setup(mContext);
        if (RealmManager.i.getRealms().size() == 1) {
            // if there's only one realm, select it an move on.
            RealmManager.i.selectRealm(mContext, RealmManager.i.getRealms().get(0).getDisplayName());
            finish();
            startActivity(new Intent(mContext, WarWorldsActivity.class));
            return;
        }

        CharSequence[] realmNames = new CharSequence[RealmManager.i.getRealms().size()];
        for (int i = 0; i < realmNames.length; i++) {
            Realm realm = RealmManager.i.getRealms().get(i);
            realmNames[i] = Html.fromHtml(String.format("<font color=\"#ffffff\"><b>%s</b></font><br/><small>%s</small>",
                        realm.getDisplayName(), realm.getDescription()
                    ));
        }

        final ListView realmsListView = (ListView) findViewById(R.id.realms);
        realmsListView.setAdapter(new ArrayAdapter<CharSequence>(mContext, R.layout.account, realmNames));
        realmsListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        for (int i = 0; i < realmNames.length; i++) {
            Realm realm = RealmManager.i.getRealms().get(i);
            if (realm.getDisplayName().equals("Beta")) {
                realmsListView.setItemChecked(i, true);
            }
        }

        Button startBtn = (Button) findViewById(R.id.start_btn);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedPosition = realmsListView.getCheckedItemPosition();
                TextView realmName = (TextView) realmsListView.getChildAt(selectedPosition);

                DeviceRegistrar.unregister(mContext); // need to register device in new realm
                RealmManager.i.selectRealm(mContext, (String) realmName.getText());
                ServerGreeter.clearHello();

                // this activity is finished, move to the main WarWorldsActivity
                finish();
                startActivity(new Intent(mContext, WarWorldsActivity.class));
            }
        });

        final Button logOutButton = (Button) findViewById(R.id.log_out_btn);
        logOutButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                finish();
                startActivity(new Intent(mContext, AccountsActivity.class));
            }
        });
    }
}
