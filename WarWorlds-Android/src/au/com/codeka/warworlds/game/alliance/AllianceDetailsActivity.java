package au.com.codeka.warworlds.game.alliance;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceManager;
import au.com.codeka.warworlds.model.EmpireManager;

public class AllianceDetailsActivity extends BaseActivity {
    private Context mContext = this;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            String allianceKey = extras.getString("au.com.codeka.warworlds.AllianceKey");

            Alliance myAlliance = EmpireManager.getInstance().getEmpire().getAlliance();
            if (myAlliance == null) {
                setContentView(R.layout.alliance_details_potential);
            } else if (myAlliance.getKey().equals(allianceKey)) {
                setContentView(R.layout.alliance_details_mine);
            } else {
                //setContentView(R.layout.alliance_details_enemy);
            }

            Alliance alliance = (Alliance) extras.getParcelable("au.com.codeka.warworlds.Alliance");
            if (alliance != null) {
                refreshAlliance(alliance);
            }

            AllianceManager.getInstance().fetchAlliance(allianceKey, new AllianceManager.FetchAllianceCompleteHandler() {
                @Override
                public void onAllianceFetched(Alliance alliance) {
                    refreshAlliance(alliance);
                }
            });
        }
    }

    private void refreshAlliance(Alliance alliance) {
        TextView allianceName = (TextView) findViewById(R.id.alliance_name);
        allianceName.setText(alliance.getName());

        TextView allianceMembers = (TextView) findViewById(R.id.alliance_num_members);
        allianceMembers.setText(String.format("Members: %d", alliance.getNumMembers()));

        
    }
}
