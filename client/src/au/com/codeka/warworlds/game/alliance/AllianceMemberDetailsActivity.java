package au.com.codeka.warworlds.game.alliance;

import java.text.DecimalFormat;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import au.com.codeka.common.model.BaseEmpireRank;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.game.starfield.StarfieldActivity;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.MyEmpire;

import com.google.protobuf.InvalidProtocolBufferException;

public class AllianceMemberDetailsActivity extends BaseActivity
                                           implements AllianceManager.AllianceUpdatedHandler,
                                                      EmpireManager.EmpireFetchedHandler,
                                                      EmpireShieldManager.EmpireShieldUpdatedHandler {
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
                    Messages.Alliance alliance_pb = Messages.Alliance.parseFrom(alliance_bytes);
                    mAlliance = new Alliance();
                    mAlliance.fromProtocolBuffer(alliance_pb);
                } catch (InvalidProtocolBufferException e) {
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
        EmpireShieldManager.i.addEmpireShieldUpdatedHandler(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        AllianceManager.i.removeAllianceUpdatedHandler(this);
        EmpireManager.i.removeEmpireUpdatedListener(this);
        EmpireShieldManager.i.removeEmpireShieldUpdatedHandler(this);
    }

    @Override
    public void onEmpireFetched(Empire empire) {
        if (mEmpireKey.equals(empire.getKey())) {
            mEmpire = empire;
            refresh();
        }
    }

    @Override
    public void onAllianceUpdated(Alliance alliance) {
        if (alliance.getID() == mAllianceID) {
            mAlliance = alliance;
            refresh();
        }
    }

    /** Called when an empire's shield is updated, we'll have to refresh the list. */
    @Override
    public void onEmpireShieldUpdated(int empireID) {
        if (Integer.parseInt(mEmpire.getKey()) == empireID) {
            refresh();
        }
    }

    private void fullRefresh() {
        Alliance myAlliance = (Alliance) EmpireManager.i.getEmpire().getAlliance();
        if (myAlliance == null || myAlliance.getID() != mAllianceID) {
            setContentView(R.layout.alliance_member_details_enemy);
        } else {
            setContentView(R.layout.alliance_member_details_mine);
        }

        Button viewBtn = (Button) findViewById(R.id.view_btn);
        viewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BaseStar star = mEmpire.getHomeStar();
                Intent intent = new Intent(AllianceMemberDetailsActivity.this, StarfieldActivity.class);
                intent.putExtra("au.com.codeka.warworlds.StarKey", star.getKey());
                intent.putExtra("au.com.codeka.warworlds.SectorX", star.getSectorX());
                intent.putExtra("au.com.codeka.warworlds.SectorY", star.getSectorY());
                intent.putExtra("au.com.codeka.warworlds.OffsetX", star.getOffsetX());
                intent.putExtra("au.com.codeka.warworlds.OffsetY", star.getOffsetY());
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
        empireName.setText(mEmpire.getDisplayName());
        empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(this, mEmpire));

        BaseEmpireRank rank = mEmpire.getRank();
        if (rank != null) {
            totalStars.setText(Html.fromHtml(String.format("Stars: <b>%s</b>",
                    formatter.format(rank.getTotalStars()))));
            totalColonies.setText(Html.fromHtml(String.format("Colonies: <b>%s</b>",
                    formatter.format(rank.getTotalColonies()))));

            MyEmpire myEmpire = EmpireManager.i.getEmpire();
            if (mEmpire.getKey().equals(myEmpire.getKey()) || rank.getTotalStars() >= 10) {
                totalShips.setText(Html.fromHtml(String.format("Ships: <b>%s</b>",
                       formatter.format(rank.getTotalShips()))));
                totalBuildings.setText(Html.fromHtml(String.format("Buildings: <b>%s</b>",
                       formatter.format(rank.getTotalBuildings()))));
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
        if (kickBtn != null) {
            if (mEmpire == null || mEmpire.getKey().equals(EmpireManager.i.getEmpire().getKey())) {
                kickBtn.setEnabled(false);
            } else {
                kickBtn.setEnabled(true);
            }
        }
    }
}
