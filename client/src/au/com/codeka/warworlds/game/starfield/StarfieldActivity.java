package au.com.codeka.warworlds.game.starfield;

import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import au.com.codeka.TimeInHours;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.ShipDesign;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.ctrl.FleetListSimple;
import au.com.codeka.warworlds.ctrl.InfobarView;
import au.com.codeka.warworlds.ctrl.PlanetListSimple;
import au.com.codeka.warworlds.ctrl.SelectionView;
import au.com.codeka.warworlds.game.EmpireActivity;
import au.com.codeka.warworlds.game.ScoutReportDialog;
import au.com.codeka.warworlds.game.SitrepActivity;
import au.com.codeka.warworlds.game.StarRenameDialog;
import au.com.codeka.warworlds.game.alliance.AllianceActivity;
import au.com.codeka.warworlds.game.solarsystem.SolarSystemActivity;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.PurchaseManager;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.SpriteManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.StarSummary;
import au.com.codeka.warworlds.model.billing.IabException;
import au.com.codeka.warworlds.model.billing.IabHelper;
import au.com.codeka.warworlds.model.billing.IabResult;
import au.com.codeka.warworlds.model.billing.Purchase;
import au.com.codeka.warworlds.model.billing.SkuDetails;

/**
 * The \c StarfieldActivity is the "home" screen of the game, and displays the
 * starfield where you scroll around and interact with stars, etc.
 */
public class StarfieldActivity extends BaseActivity
                               implements StarfieldSurfaceView.OnSelectionChangedListener,
                                          StarManager.StarFetchedHandler {
    private static final Logger log = LoggerFactory.getLogger(StarfieldActivity.class);
    private Context mContext = this;
    private StarfieldSurfaceView mStarfield;
    private PlanetListSimple mPlanetList;
    private FleetListSimple mFleetList;
    private Star mSelectedStar;

    private Purchase mStarRenamePurchase;

    // when fetching a star/fleet we set this to the one we're fetching. This
    // way, if there's multiple in progress at once, on the last one to be
    // initiated will actually do anything
    private String mFetchingStarKey;
    private String mFetchingFleetKey;

    private static final int SOLAR_SYSTEM_REQUEST = 1;
    private static final int EMPIRE_REQUEST = 2;
    private static final int SITREP_REQUEST = 3;
    private static final int TACTICAL_MAP_REQUEST = 4;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar

        setContentView(R.layout.starfield);

        mStarfield = (StarfieldSurfaceView) findViewById(R.id.starfield);
        mStarfield.setSelectionView((SelectionView) findViewById(R.id.selection));

        mPlanetList = (PlanetListSimple) findViewById(R.id.planet_list);
        mFleetList = (FleetListSimple) findViewById(R.id.fleet_list);

        findViewById(R.id.selected_star).setVisibility(View.GONE);
        findViewById(R.id.selected_fleet).setVisibility(View.GONE);

        mStarfield.addSelectionChangedListener(this);

        if (isPortrait()) {
            InfobarView infobar = (InfobarView) findViewById(R.id.infobar);
            infobar.hideEmpireName();
        }

        mPlanetList.setPlanetSelectedHandler(new PlanetListSimple.PlanetSelectedHandler() {
            @Override
            public void onPlanetSelected(Planet planet) {
                navigateToPlanet(mSelectedStar, planet, false);
            }
        });
        mFleetList.setFleetSelectedHandler(new FleetListSimple.FleetSelectedHandler() {
            @Override
            public void onFleetSelected(Fleet fleet) {
                if (mSelectedStar == null) {
                    return; //??
                }

                openEmpireActivityAtFleet(mSelectedStar, fleet);
            }
        });

        final Button empireBtn = (Button) findViewById(R.id.empire_btn);
        empireBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEmpireActivity();
            }
        });

        final Button scoutReportBtn = (Button) findViewById(R.id.scout_report_btn);
        scoutReportBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedStar != null) {
                    ScoutReportDialog dialog = new ScoutReportDialog();
                    dialog.setStar(mSelectedStar);
                    dialog.show(getSupportFragmentManager(), "");
                }
            }
        });

        final Button sitrepBtn = (Button) findViewById(R.id.sitrep_btn);
        sitrepBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSitrepActivity();
            }
        });

        final Button tacticalBtn = (Button) findViewById(R.id.tactical_map_btn);
        tacticalBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, TacticalMapActivity.class);
                // get the star closest to the centre of the screen currently, that's what we'll focus
                // on in the tactical map as well.
                Star centreStar = mStarfield.findStarInCentre();
                if (centreStar != null) {
                    intent.putExtra("au.com.codeka.warworlds.SectorX", centreStar.getSectorX());
                    intent.putExtra("au.com.codeka.warworlds.SectorY", centreStar.getSectorY());
                    intent.putExtra("au.com.codeka.warworlds.OffsetX", centreStar.getOffsetX());
                    intent.putExtra("au.com.codeka.warworlds.OffsetY", centreStar.getOffsetY());
                }
                startActivityForResult(intent, TACTICAL_MAP_REQUEST);
            }
        });

        final Button viewBtn = (Button) findViewById(R.id.view_btn);
        viewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedStar == null) {
                    return;
                }

                Intent intent = new Intent(mContext, SolarSystemActivity.class);
                intent.putExtra("au.com.codeka.warworlds.StarKey", mSelectedStar.getKey());
                startActivityForResult(intent, SOLAR_SYSTEM_REQUEST);
            }
        });

        final Button renameBtn = (Button) findViewById(R.id.rename_btn);
        renameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRenameClick();
            }
        });

        final Button allianceBtn = (Button) findViewById(R.id.alliance_btn);
        allianceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAllianceClick();
            }
        });

        ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
            @Override
            public void onHelloComplete(boolean success, ServerGreeting greeting) {
                if (!success) {
                    return;
                }

                Intent intent = getIntent();
                if (intent != null && intent.getExtras() != null) {
                    String starKey = intent.getExtras().getString("au.com.codeka.warworlds.StarKey");
                    if (starKey != null) {
                        long sectorX = intent.getExtras().getLong("au.com.codeka.warworlds.SectorX");
                        long sectorY = intent.getExtras().getLong("au.com.codeka.warworlds.SectorY");
                        int offsetX = intent.getExtras().getInt("au.com.codeka.warworlds.OffsetX");
                        int offsetY = intent.getExtras().getInt("au.com.codeka.warworlds.OffsetY");
                        mStarfield.scrollTo(sectorX, sectorY, offsetX, offsetY, true);
                        return;
                    }
                }

                MyEmpire myEmpire = EmpireManager.getInstance().getEmpire();
                if (myEmpire == null) {
                    return;
                }

                BaseStar homeStar = myEmpire.getHomeStar();
                if (homeStar != null) {
                    mStarfield.scrollTo(homeStar.getSectorX(), homeStar.getSectorY(),
                                        homeStar.getOffsetX(), homeStar.getOffsetY(),
                                        true);
                } else {
                    // this should never happen...
                    findColony(greeting.getColonies());
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        StarManager.getInstance().addStarUpdatedListener(null, this);
    }

    @Override
    public void onPostResume() {
        super.onPostResume();

        if (mStarRenamePurchase != null) {
            showStarRenamePopup(mStarRenamePurchase);
            mStarRenamePurchase = null;
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        StarManager.getInstance().removeStarUpdatedListener(this);
    }

    /**
     * Finds one of our colony's stars and scrolls to it.
     */
    private void findColony(List<Colony> colonies) {
        // we'll want to start off near one of your stars. If you
        // only have one, that's easy -- but if you've got lots
        // what then?
        String starKey = null;
        if (colonies == null) {
            return;
        }
        for (Colony c : colonies) {
            starKey = c.getStarKey();
        }

        if (starKey != null) {
            StarManager.getInstance().requestStarSummary(StarfieldActivity.this, starKey,
                    new StarManager.StarSummaryFetchedHandler() {
                @Override
                public void onStarSummaryFetched(StarSummary s) {
                    mStarfield.scrollTo(s.getSectorX(), s.getSectorY(),
                                        s.getOffsetX(), s.getOffsetY(),
                                        true);
                }
            });
        }
    }

    public void openEmpireActivityAtFleet(Star star, Fleet fleet) {
        Intent intent = new Intent(mContext, EmpireActivity.class);
        intent.putExtra("au.com.codeka.warworlds.StarKey", star.getKey());
        intent.putExtra("au.com.codeka.warworlds.FleetKey", fleet.getKey());
        startActivityForResult(intent, EMPIRE_REQUEST);
    }

    public void openEmpireActivity() {
        Intent intent = new Intent(mContext, EmpireActivity.class);
        startActivityForResult(intent, EMPIRE_REQUEST);
    }

    public void openSitrepActivity() {
        Intent intent = new Intent(mContext, SitrepActivity.class);
        startActivityForResult(intent, SITREP_REQUEST);
    }

    public void onAllianceClick() {
        Intent intent = new Intent(mContext, AllianceActivity.class);
        startActivity(intent);
    }

    /**
     * Navigates to the given planet in the given star. Starts the SolarSystemActivity.
     * 
     * @param star
     * @param planet
     * @param scrollView If \c true, we'll also scroll the current view so that given star
     *         is centered on the given star.
     */
    public void navigateToPlanet(Star star, Planet planet, boolean scrollView) {
        navigateToPlanet(star.getSectorX(), star.getSectorY(), star.getKey(),
                         star.getOffsetX(), star.getOffsetY(), planet.getIndex(),
                         scrollView);
    }

    private void navigateToPlanet(long sectorX, long sectorY, String starKey, int starOffsetX,
                                  int starOffsetY, int planetIndex, boolean scrollView) {
        if (scrollView) {
            int offsetX = starOffsetX - (int) ((mStarfield.getWidth() / 2) / mStarfield.getPixelScale());
            int offsetY = starOffsetY -  (int) ((mStarfield.getHeight() / 2) / mStarfield.getPixelScale());
            mStarfield.scrollTo(sectorX, sectorY, offsetX, offsetY);
        }

        Intent intent = new Intent(mContext, SolarSystemActivity.class);
        intent.putExtra("au.com.codeka.warworlds.StarKey", starKey);
        intent.putExtra("au.com.codeka.warworlds.PlanetIndex", planetIndex);
        startActivityForResult(intent, SOLAR_SYSTEM_REQUEST);
    }

    public void navigateToFleet(final String starKey, final String fleetKey) {
        Star star = SectorManager.getInstance().findStar(starKey);
        if (star == null) {
            StarManager.getInstance().requestStar(mContext, starKey, false,
                new StarManager.StarFetchedHandler() {
                    @Override
                    public void onStarFetched(Star s) {
                        BaseFleet fleet = s.findFleet(fleetKey);
                        navigateToFleet(s, fleet);
                    }
                });
        } else {
            navigateToFleet(star, star.findFleet(fleetKey));
        }
    }

    public void navigateToFleet(Star star, BaseFleet fleet) {
        int offsetX = star.getOffsetX() - (int) ((mStarfield.getWidth() / 2) / mStarfield.getPixelScale());
        int offsetY = star.getOffsetY() - (int) ((mStarfield.getHeight() / 2) / mStarfield.getPixelScale());

        // todo: if the fleet is moving, scroll to it...

        mStarfield.scrollTo(star.getSectorX(), star.getSectorY(), offsetX, offsetY);

        if (fleet.getState() == Fleet.State.MOVING) {
            mStarfield.selectFleet(fleet);
        } else {
            mStarfield.selectStar(star.getKey());
        }
    }

    public void onRenameClick() {
        SkuDetails starRenameSku;
        try {
            starRenameSku = PurchaseManager.getInstance()
                                           .getInventory().getSkuDetails("star_rename");
        } catch (IabException e) {
            log.error("Couldn't get SKU details!", e);
            return;
        }

        new StyledDialog.Builder(this)
                .setMessage(String.format(Locale.ENGLISH,
                        "Renaming stars costs %s. If you wish to continue, you'll be directed "+
                        "to the Play Store where you can purchase a one-time code to rename this "+
                        "star. Are you sure you want to continue?",
                        starRenameSku.getPrice()))
                .setTitle("Rename Star")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doRenameStar();
                        dialog.dismiss();
                    }
                })
                .create().show();
    }

    public void doRenameStar() {
        if (mSelectedStar == null) {
            return;
        }

        try {
            PurchaseManager.getInstance().launchPurchaseFlow(this, "star_rename", new IabHelper.OnIabPurchaseFinishedListener() {
                @Override
                public void onIabPurchaseFinished(IabResult result, final Purchase info) {
                    if (mSelectedStar == null) {
                        return;
                    }

                    boolean isSuccess = result.isSuccess();
                    if (result.isFailure() && result.getResponse() == IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
                        // if they've already purchased a star-renamed, but not reclaimed it, then
                        // we let them through anyway.
                        isSuccess = true;
                    }

                    if (isSuccess) {
                        try {
                            showStarRenamePopup(info);
                        } catch(IllegalStateException e) {
                            // this can be called before the activity is resumed, so we just set a
                            // flag that'll cause us to pop up the dialog when the activity is resumed.
                            mStarRenamePurchase = info;
                        }
                    }
                }
            });
        } catch (IabException e) {
            log.error("Couldn't get SKU details!", e);
            return;
        }
    }

    private void showStarRenamePopup(Purchase purchase) {
        StarRenameDialog dialog = new StarRenameDialog();
        dialog.setPurchaseInfo(purchase);
        dialog.setStar(mSelectedStar);
        dialog.show(getSupportFragmentManager(), "");
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putBoolean("au.com.codeka.warworlds.IsFirstRefresh", false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == SOLAR_SYSTEM_REQUEST && intent != null) {
            boolean wasSectorUpdated = intent.getBooleanExtra(
                    "au.com.codeka.warworlds.SectorUpdated", false);
            long sectorX = intent.getLongExtra("au.com.codeka.warworlds.SectorX", 0);
            long sectorY = intent.getLongExtra("au.com.codeka.warworlds.SectorY", 0);
            String starKey = intent.getStringExtra("au.com.codeka.warworlds.StarKey");

            if (wasSectorUpdated) {
                SectorManager.getInstance().refreshSector(sectorX, sectorY);
            } else if (starKey != null) {
                // make sure we re-select the star you had selected before.
                mStarfield.selectStar(starKey);
            }
        } else if (requestCode == EMPIRE_REQUEST && intent != null) {
            EmpireActivity.EmpireActivityResult res = EmpireActivity.EmpireActivityResult.fromValue(
                    intent.getIntExtra("au.com.codeka.warworlds.Result", 0));

            long sectorX = intent.getLongExtra("au.com.codeka.warworlds.SectorX", 0);
            long sectorY = intent.getLongExtra("au.com.codeka.warworlds.SectorY", 0);
            int starOffsetX = intent.getIntExtra("au.com.codeka.warworlds.StarOffsetX", 0);
            int starOffsetY = intent.getIntExtra("au.com.codeka.warworlds.StarOffsetY", 0);
            String starKey = intent.getStringExtra("au.com.codeka.warworlds.StarKey");

            if (res == EmpireActivity.EmpireActivityResult.NavigateToPlanet) {
                int planetIndex = intent.getIntExtra("au.com.codeka.warworlds.PlanetIndex", 0);

                navigateToPlanet(sectorX, sectorY, starKey, starOffsetX, starOffsetY,
                                 planetIndex, true);
            } else if (res == EmpireActivity.EmpireActivityResult.NavigateToFleet) {
                String fleetKey = intent.getStringExtra("au.com.codeka.warworlds.FleetKey");

                navigateToFleet(starKey, fleetKey);
            }
        } else if (requestCode == TACTICAL_MAP_REQUEST && intent != null) {
            long sectorX = intent.getLongExtra("au.com.codeka.warworlds.SectorX", 0);
            long sectorY = intent.getLongExtra("au.com.codeka.warworlds.SectorY", 0);
            int offsetX = intent.getIntExtra("au.com.codeka.warworlds.OffsetX", 0);
            int offsetY = intent.getIntExtra("au.com.codeka.warworlds.OffsetY", 0);
            String starKey = intent.getStringExtra("au.com.codeka.warworlds.StarKey");

            mStarfield.scrollTo(sectorX, sectorY, offsetX, offsetY, true);
            mStarfield.selectStar(starKey);
        }
    }

    @Override
    public void onStarSelected(Star star) {
        if (mSelectedStar != null && mSelectedStar.getKey().equals(star.getKey())) {
            // same star, ignore...
            return;
        }

        final View selectionLoadingContainer = findViewById(R.id.loading_container);
        final View selectedStarContainer = findViewById(R.id.selected_star);
        final View selectedFleetContainer = findViewById(R.id.selected_fleet);

        // load the rest of the star's details as well
        selectionLoadingContainer.setVisibility(View.VISIBLE);
        selectedStarContainer.setVisibility(View.GONE);
        selectedFleetContainer.setVisibility(View.GONE);
        mFetchingStarKey = star.getKey();
        mFetchingFleetKey = null;

        StarManager.getInstance().requestStar(mContext, star.getKey(), true, this);
    }

    @Override
    public void onStarFetched(Star star) {
        if (mSelectedStar != null && mSelectedStar.getKey().equals(star.getKey())) {
            // if it's the star we already have selected, then we may as well refresh
            // whatever we've got.
            mFetchingStarKey = mSelectedStar.getKey();
        }
        if (mFetchingStarKey == null ||
            !mFetchingStarKey.equals(star.getKey())) {
            return;
        }

        final View selectionLoadingContainer = findViewById(R.id.loading_container);
        final View selectedStarContainer = findViewById(R.id.selected_star);
        final View selectedFleetContainer = findViewById(R.id.selected_fleet);
        final TextView starName = (TextView) findViewById(R.id.star_name);
        final TextView starKind = (TextView) findViewById(R.id.star_kind);
        final ImageView starIcon = (ImageView) findViewById(R.id.star_icon);
        final Button renameButton = (Button) findViewById(R.id.rename_btn);

        mSelectedStar = star;
        selectionLoadingContainer.setVisibility(View.GONE);
        selectedStarContainer.setVisibility(View.VISIBLE);
        selectedFleetContainer.setVisibility(View.GONE);

        mPlanetList.setStar(star);
        mFleetList.setStar(star);

        MyEmpire myEmpire = EmpireManager.getInstance().getEmpire();
        int numMyEmpire = 0;
        int numOtherEmpire = 0;
        for (BaseColony colony : star.getColonies()) {
            if (colony.getEmpireKey() == null) {
                continue;
            }
            if (colony.getEmpireKey().equals(myEmpire.getKey())) {
                numMyEmpire ++;
            } else {
                numOtherEmpire ++;
            }
        }
        if (numMyEmpire > numOtherEmpire) {
            renameButton.setVisibility(View.VISIBLE);
        } else {
            renameButton.setVisibility(View.GONE);
        }

        starName.setText(star.getName());
        starKind.setText(star.getStarType().getDisplayName());
        Sprite starImage = StarImageManager.getInstance().getSprite(mContext, star, 80);
        starIcon.setImageDrawable(new SpriteDrawable(starImage));
    }

    @Override
    public void onFleetSelected(final Fleet fleet) {
        final View selectionLoadingContainer = findViewById(R.id.loading_container);
        final View selectedStarContainer = findViewById(R.id.selected_star);
        final View selectedFleetContainer = findViewById(R.id.selected_fleet);
        final ImageView fleetIcon = (ImageView) findViewById(R.id.fleet_icon);
        final ImageView empireIcon = (ImageView) findViewById(R.id.empire_icon);
        final TextView fleetDesign = (TextView) findViewById(R.id.fleet_design);
        final TextView empireName = (TextView) findViewById(R.id.empire_name);
        final TextView fleetDetails = (TextView) findViewById(R.id.fleet_details);

        empireName.setText("");
        empireIcon.setImageBitmap(null);
        mFetchingFleetKey = fleet.getKey();
        mFetchingStarKey = null;
        mSelectedStar = null;

        ShipDesign design = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, fleet.getDesignID());
        EmpireManager.getInstance().fetchEmpire(mContext, fleet.getEmpireKey(), new EmpireManager.EmpireFetchedHandler() {
            @Override
            public void onEmpireFetched(Empire empire) {
                if (mFetchingFleetKey == null ||
                    !mFetchingFleetKey.equals(fleet.getKey())) {
                    return;
                }
                empireName.setText(empire.getDisplayName());
                empireIcon.setImageBitmap(empire.getShield(mContext));
            }
        });

        fleetDesign.setText(design.getDisplayName());
        fleetIcon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(design.getSpriteName())));

        String eta = "???";
        Star srcStar = SectorManager.getInstance().findStar(fleet.getStarKey());
        Star destStar = SectorManager.getInstance().findStar(fleet.getDestinationStarKey());
        if (srcStar != null && destStar != null) {
            float timeRemainingInHours = fleet.getTimeToDestination(srcStar, destStar);
            eta = TimeInHours.format(timeRemainingInHours);
        }

        String details = String.format(Locale.ENGLISH,
            "<b>Ships:</b> %d<br />" +
            "<b>Speed:</b> %.2f pc/hr<br />" +
            "<b>Destination:</b> %s<br />" +
            "<b>ETA:</b> %s",
            (int) Math.ceil(fleet.getNumShips()), design.getSpeedInParsecPerHour(),
            (destStar == null ? "???" : destStar.getName()),
            eta);
        fleetDetails.setText(Html.fromHtml(details));

        selectionLoadingContainer.setVisibility(View.GONE);
        selectedStarContainer.setVisibility(View.GONE);
        selectedFleetContainer.setVisibility(View.VISIBLE);
    }
}
