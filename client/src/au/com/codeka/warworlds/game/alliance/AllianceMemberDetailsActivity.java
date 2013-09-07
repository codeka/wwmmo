package au.com.codeka.warworlds.game.alliance;

import java.io.IOException;
import java.text.DecimalFormat;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import au.com.codeka.common.model.Alliance;
import au.com.codeka.common.model.Empire;
import au.com.codeka.common.model.EmpireRank;
import au.com.codeka.common.model.Model;
import au.com.codeka.common.model.Star;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.game.starfield.StarfieldActivity;
import au.com.codeka.warworlds.model.AllianceManager;
import au.com.codeka.warworlds.model.EmpireHelper;
import au.com.codeka.warworlds.model.EmpireManager;

public class AllianceMemberDetailsActivity extends BaseActivity
                                           implements AllianceManager.AllianceUpdatedHandler,
                                                      EmpireManager.EmpireFetchedHandler {
    private int mAllianceID;
    private String mEmpireKey;
    private Alliance mAlliance;
    private Empire mEmpire;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAllianceID = Integer.parseInt(extras.getString("au.com.codeka.warworlds.AllianceKey"));
            byte[] alliance_bytes = extras.getByteArray("au.com.codeka.warworlds.Alliance");
            if (alliance_bytes != null) {
                try {
                    mAlliance = Model.wire.parseFrom(alliance_bytes, Alliance.class);
                } catch (IOException e) {
                }
            }

            mEmpireKey = extras.getString("au.com.codeka.warworlds.EmpireKey");
            mEmpire = EmpireManager.i.getEmpire(mEmpireKey);

            AllianceManager.i.fetchAlliance(mAllianceID, new AllianceManager.FetchAllianceCompleteHandler() {
                @Override
                public void onAllianceFetched(Alliance alliance) {
                    mAlliance = alliance;
                    //refreshAlliance();
                }
            });
        }

        fullRefresh();
    }

    @Override
    public void onStart() {
        super.onStart();
        AllianceManager.i.addAllianceUpdatedHandler(this);
        EmpireManager.i.addEmpireUpdatedListener(null, this);
    }

    @Override
    public void onStop() {
        super.onStop();
        AllianceManager.i.removeAllianceUpdatedHandler(this);
        EmpireManager.i.removeEmpireUpdatedListener(this);
    }

    @Override
    public void onEmpireFetched(Empire empire) {
        if (mEmpireKey.equals(empire.key)) {
            mEmpire = empire;
            refresh();
        }
    }

    @Override
    public void onAllianceUpdated(Alliance alliance) {
        if (Integer.parseInt(alliance.key) == mAllianceID) {
            mAlliance = alliance;
            refresh();
        }
    }

    private void fullRefresh() {
        Alliance myAlliance = (Alliance) EmpireManager.i.getEmpire().alliance;
        if (myAlliance == null || Integer.parseInt(myAlliance.key) != mAllianceID) {
            setContentView(R.layout.alliance_member_details_enemy);
        } else {
            setContentView(R.layout.alliance_member_details_mine);
        }

        Button viewBtn = (Button) findViewById(R.id.view_btn);
        viewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Star star = mEmpire.home_star;
                Intent intent = new Intent(AllianceMemberDetailsActivity.this, StarfieldActivity.class);
                intent.putExtra("au.com.codeka.warworlds.StarKey", star.key);
                intent.putExtra("au.com.codeka.warworlds.SectorX", star.sector_x);
                intent.putExtra("au.com.codeka.warworlds.SectorY", star.sector_y);
                intent.putExtra("au.com.codeka.warworlds.OffsetX", star.offset_x);
                intent.putExtra("au.com.codeka.warworlds.OffsetY", star.offset_y);
                startActivity(intent);
            }
        });

        Button kickBtn = (Button) findViewById(R.id.kick_btn);
        if (kickBtn != null) kickBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                KickRequestDialog dialog = new KickRequestDialog();
                dialog.setup(mAlliance, mEmpire);
                dialog.show(getSupportFragmentManager(), "");
            }
        });

        refresh();
    }

    private void refresh() {
        ImageView empireIcon = (ImageView) findViewById(R.id.empire_icon);
        TextView empireName = (TextView) findViewById(R.id.empire_name);
        TextView totalStars = (TextView) findViewById(R.id.total_stars);
        TextView totalColonies = (TextView) findViewById(R.id.total_colonies);
        TextView totalShips = (TextView) findViewById(R.id.total_ships);
        TextView totalBuildings = (TextView) findViewById(R.id.total_buildings);
        Button kickBtn = (Button) findViewById(R.id.kick_btn);

        DecimalFormat formatter = new DecimalFormat("#,##0");
        empireName.setText(mEmpire.display_name);
        empireIcon.setImageBitmap(EmpireHelper.getShield(this, mEmpire));

        EmpireRank rank = mEmpire.rank;
        if (rank != null) {
            totalStars.setText(Html.fromHtml(String.format("Stars: <b>%s</b>",
                    formatter.format(rank.total_stars))));
            totalColonies.setText(Html.fromHtml(String.format("Colonies: <b>%s</b>",
                    formatter.format(rank.total_colonies))));

            Empire myEmpire = EmpireManager.i.getEmpire();
            if (mEmpire.key.equals(myEmpire.key) || rank.total_stars >= 10) {
                totalShips.setText(Html.fromHtml(String.format("Ships: <b>%s</b>",
                       formatter.format(rank.total_ships))));
                totalBuildings.setText(Html.fromHtml(String.format("Buildings: <b>%s</b>",
                       formatter.format(rank.total_buildings))));
            } else {
                totalShips.setText("");
                totalBuildings.setText("");
            }
        } else {
            totalStars.setText("");
            totalColonies.setText("");
            totalShips.setText("");
            totalBuildings.setText("");
        }

        // you can't vote to kick yourself, so just disable the button
        if (mEmpire == null || mEmpire.key.equals(EmpireManager.i.getEmpire().key)) {
            kickBtn.setEnabled(false);
        } else {
            kickBtn.setEnabled(true);
        }
    }
}
