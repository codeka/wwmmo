package au.com.codeka.warworlds.game;

import java.text.DecimalFormat;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import au.com.codeka.Cash;
import au.com.codeka.common.model.BaseEmpireRank;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.WarWorldsActivity;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.alliance.KickRequestDialog;
import au.com.codeka.warworlds.game.chat.ChatActivity;
import au.com.codeka.warworlds.game.starfield.StarfieldActivity;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceShieldManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.ShieldManager;

public class EnemyEmpireActivity extends BaseActivity {
    private Context mContext = this;
    private int mEmpireID;
    private Empire mEmpire;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.enemy_empire);

        Button viewBtn = (Button) findViewById(R.id.view_btn);
        viewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEmpireViewClick();
            }
        });

        Button privateMsgBtn = (Button) findViewById(R.id.private_message_btn);
        privateMsgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPrivateMessageClick();
            }
        });

        Button kickBtn = (Button) findViewById(R.id.kick_btn);
        kickBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onKickClick();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
            @Override
            public void onHelloComplete(boolean success, ServerGreeting greeting) {
                if (!success) {
                    startActivity(new Intent(EnemyEmpireActivity.this, WarWorldsActivity.class));
                } else {
                    mEmpireID = Integer.parseInt(
                            getIntent().getExtras().getString("au.com.codeka.warworlds.EmpireKey"));
                    mEmpire = EmpireManager.i.getEmpire(mEmpireID);
                    refresh();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        ShieldManager.eventBus.register(mEventHandler);
    }

    @Override
    protected void onStop() {
        super.onStart();
        ShieldManager.eventBus.unregister(mEventHandler);
    }


    private Object mEventHandler = new Object() {
        @EventHandler
        public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
            refresh();
        }

        @EventHandler
        public void onEmpireFetched(Empire empire) {
            if (mEmpireID == empire.getID()) {
                mEmpire = empire;
                refresh();
            }
        }
    };

    private void refresh() {
        MyEmpire myEmpire = EmpireManager.i.getEmpire();

        TextView empireName = (TextView) findViewById(R.id.empire_name);
        ImageView empireIcon = (ImageView) findViewById(R.id.empire_icon);

        empireName.setText(mEmpire.getDisplayName());
        empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(mContext, mEmpire));

        TextView tv = (TextView) findViewById(R.id.private_message_btn_msg);
        tv.setText(String.format(tv.getText().toString(), mEmpire.getDisplayName()));

        tv = (TextView) findViewById(R.id.view_msg);
        tv.setText(String.format(tv.getText().toString(), mEmpire.getDisplayName()));

        TextView allianceName = (TextView) findViewById(R.id.alliance_name);
        ImageView allianceIcon = (ImageView) findViewById(R.id.alliance_icon);

        Alliance alliance = (Alliance) mEmpire.getAlliance();
        if (alliance != null) {
            allianceName.setText(alliance.getName());
            allianceIcon.setImageBitmap(AllianceShieldManager.i.getShield(this, alliance));
        } else {
            allianceName.setText("");
            allianceIcon.setImageBitmap(null);
        }

        TextView totalStars = (TextView) findViewById(R.id.total_stars);
        TextView totalColonies = (TextView) findViewById(R.id.total_colonies);
        TextView totalShips = (TextView) findViewById(R.id.total_ships);
        TextView totalBuildings = (TextView) findViewById(R.id.total_buildings);
        TextView rankNumber = (TextView) findViewById(R.id.rank);
        TextView taxes = (TextView) findViewById(R.id.taxes);
        DecimalFormat formatter = new DecimalFormat("#,##0");

        if (mEmpire.getTaxCollectedPerHour() != null) {
            float taxesPerHour = (float) (double) mEmpire.getTaxCollectedPerHour();
            float taxesPerDay = taxesPerHour * 24;
            taxes.setVisibility(View.VISIBLE);
            taxes.setText(Html.fromHtml(String.format(Locale.ENGLISH,
                    "Taxes: <b>%s</b> / day", Cash.format(taxesPerDay))));
        }

        BaseEmpireRank rank = mEmpire.getRank();
        if (rank != null) {
            rankNumber.setText(formatter.format(rank.getRank()));

            totalStars.setText(Html.fromHtml(String.format("Stars: <b>%s</b>",
                    formatter.format(rank.getTotalStars()))));
            totalColonies.setText(Html.fromHtml(String.format("Colonies: <b>%s</b>",
                    formatter.format(rank.getTotalColonies()))));

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
            rankNumber.setText("");
            totalStars.setText("");
            totalColonies.setText("");
            totalShips.setText("");
            totalBuildings.setText("");
        }

        View horzSep = findViewById(R.id.horz_sep_3);
        TextView kickInfo = (TextView) findViewById(R.id.kick_info);
        Button kickBtn = (Button) findViewById(R.id.kick_btn);
        if (mEmpire.getAlliance() != null && myEmpire.getAlliance() != null &&
            mEmpire.getID() != myEmpire.getID() &&
            mEmpire.getAlliance().getKey().equals(myEmpire.getAlliance().getKey())) {
            horzSep.setVisibility(View.VISIBLE);
            kickInfo.setVisibility(View.VISIBLE);
            kickBtn.setVisibility(View.VISIBLE);
        } else {
            horzSep.setVisibility(View.GONE);
            kickInfo.setVisibility(View.GONE);
            kickBtn.setVisibility(View.GONE);
        }
    }

    private void onKickClick() {
        KickRequestDialog dialog = new KickRequestDialog();
        dialog.setup((Alliance) mEmpire.getAlliance(), mEmpire);
        dialog.show(getSupportFragmentManager(), "");
    }

    public void onEmpireViewClick() {
        if (mEmpire != null && mEmpire.getHomeStar() != null) {
            BaseStar homeStar = mEmpire.getHomeStar();
            Intent intent = new Intent(mContext, StarfieldActivity.class);
            intent.putExtra("au.com.codeka.warworlds.StarKey", homeStar.getKey());
            intent.putExtra("au.com.codeka.warworlds.SectorX", homeStar.getSectorX());
            intent.putExtra("au.com.codeka.warworlds.SectorY", homeStar.getSectorY());
            intent.putExtra("au.com.codeka.warworlds.OffsetX", homeStar.getOffsetX());
            intent.putExtra("au.com.codeka.warworlds.OffsetY", homeStar.getOffsetY());
            startActivity(intent);
        }
    }

    public void onPrivateMessageClick() {
        if (mEmpire != null) {
            Intent intent = new Intent(mContext, ChatActivity.class);
            intent.putExtra("au.com.codeka.warworlds.NewConversationEmpireKey", mEmpire.getKey());
            startActivity(intent);

        }
    }
}
