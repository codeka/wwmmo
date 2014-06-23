package au.com.codeka.warworlds.game.solarsystem;

import java.util.Locale;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.ShipDesign;
import au.com.codeka.warworlds.ActivityBackgroundGenerator;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.WarWorldsActivity;
import au.com.codeka.warworlds.ctrl.PlanetDetailsView;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

/**
 * Activity for interacting with enemy planets (note it's not nessecarily an enemy, per se, it
 * could also be an ally or faction member).
 */
public class EnemyPlanetActivity extends BaseActivity
                                 implements StarManager.StarFetchedHandler,
                                            EmpireShieldManager.ShieldUpdatedHandler {
    private Star mStar;
    private Planet mPlanet;
    private Colony mColony;
    private Empire mColonyEmpire;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.planet_enemy);

        View rootView = findViewById(android.R.id.content);
        ActivityBackgroundGenerator.setBackground(rootView);

        Button attackBtn = (Button) findViewById(R.id.attack_btn);
        attackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAttackClick();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        EmpireShieldManager.i.addShieldUpdatedHandler(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EmpireShieldManager.i.removeShieldUpdatedHandler(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
            @Override
            public void onHelloComplete(boolean success, ServerGreeting greeting) {
                if (!success) {
                    startActivity(new Intent(EnemyPlanetActivity.this, WarWorldsActivity.class));
                } else {
                    String starKey = getIntent().getExtras().getString("au.com.codeka.warworlds.StarKey");
                    StarManager.getInstance().requestStar(starKey, false, EnemyPlanetActivity.this);
                    StarManager.getInstance().addStarUpdatedListener(starKey, EnemyPlanetActivity.this);
                }
            }
        });
    }

    /** Called when an empire's shield is updated, we'll have to refresh the list. */
    @Override
    public void onShieldUpdated(int empireID) {
        if (mColonyEmpire != null && Integer.parseInt(mColonyEmpire.getKey()) == empireID) {
            refreshEmpireDetails();
        }
    }

    private Object mEventHandler = new Object() {
        @EventHandler
        public void onStarFetched() {
    }
    @Override
    public void onStarFetched(Star s) {
        int planetIndex = getIntent().getExtras().getInt("au.com.codeka.warworlds.PlanetIndex");

        mStar = s;
        mPlanet = (Planet) s.getPlanets()[planetIndex - 1];
        for (BaseColony colony : s.getColonies()) {
            if (colony.getPlanetIndex() == planetIndex) {
                mColony = (Colony) colony;
            }
        }

        final Button attackBtn = (Button) findViewById(R.id.attack_btn);
        if (mColony != null) {
            mColonyEmpire = EmpireManager.i.getEmpire(mColony.getEmpireKey());
            if (mColonyEmpire == null) {
                attackBtn.setEnabled(false);
                EmpireManager.i.fetchEmpire(mColony.getEmpireKey(), new EmpireManager.EmpireFetchedHandler() {
                    @Override
                    public void onEmpireFetched(Empire empire) {
                        mColonyEmpire = empire;
                        attackBtn.setEnabled(true);
                        refreshEmpireDetails();
                    }
                });
            } else {
                refreshEmpireDetails();
            }
        } else {
            attackBtn.setVisibility(View.GONE);
        }

        PlanetDetailsView planetDetails = (PlanetDetailsView) findViewById(R.id.planet_details);
        planetDetails.setup(mStar, mPlanet, mColony);
    }

    private void refreshEmpireDetails() {
        ImageView enemyIcon = (ImageView) findViewById(R.id.enemy_empire_icon);
        TextView enemyName = (TextView) findViewById(R.id.enemy_empire_name);
        TextView enemyDefence = (TextView) findViewById(R.id.enemy_empire_defence);

        int defence = (int) (0.25 * mColony.getPopulation() * mColony.getDefenceBoost());
        if (defence < 1) {
            defence = 1;
        }
        enemyIcon.setImageBitmap(EmpireShieldManager.i.getShield(this, mColonyEmpire));
        enemyName.setText(mColonyEmpire.getDisplayName());
        enemyDefence.setText(String.format(Locale.ENGLISH, "Defence: %d", defence));
    }

    private void onAttackClick() {
        int defence = (int)(0.25 * mColony.getPopulation() * mColony.getDefenceBoost());

        final MyEmpire myEmpire = EmpireManager.i.getEmpire();
        int attack = 0;
        for (BaseFleet fleet : mStar.getFleets()) {
            if (fleet.getEmpireKey() == null) {
                continue;
            }
            if (fleet.getEmpireKey().equals(myEmpire.getKey())) {
                ShipDesign design = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, fleet.getDesignID());
                if (design.hasEffect("troopcarrier")) {
                    attack += Math.ceil(fleet.getNumShips());
                }
            }
        }

        StyledDialog.Builder b = new StyledDialog.Builder(this);
        b.setMessage(Html.fromHtml(String.format(Locale.ENGLISH,
                "<p>Do you want to attack this %s colony?</p>" +
                "<p><b>Colony defence:</b> %d<br />" +
                "   <b>Your attack capability:</b> %d</p>",
                mColonyEmpire.getDisplayName(), defence, attack)));
        b.setPositiveButton("Attack!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                myEmpire.attackColony(mStar, mColony,
                    new MyEmpire.AttackColonyCompleteHandler() {
                        @Override
                        public void onComplete() {
                            dialog.dismiss();
                        }
                    });
            }
        });
        b.setNegativeButton("Cancel", null);
        b.create().show();
    }
}
