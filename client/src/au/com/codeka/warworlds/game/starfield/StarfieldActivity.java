package au.com.codeka.warworlds.game.starfield;

import java.io.IOException;
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
import au.com.codeka.common.design.DesignKind;
import au.com.codeka.common.design.ShipDesign;
import au.com.codeka.common.model.Colony;
import au.com.codeka.common.model.Empire;
import au.com.codeka.common.model.Fleet;
import au.com.codeka.common.model.Model;
import au.com.codeka.common.model.Planet;
import au.com.codeka.common.model.Star;
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
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.EmpireHelper;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.PurchaseManager;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.SpriteManager;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.StarType;
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
    private Fleet mSelectedFleet;

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
    public void onCreate(final Bundle savedInstanceState) {
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
                    intent.putExtra("au.com.codeka.warworlds.SectorX", centreStar.sector_x);
                    intent.putExtra("au.com.codeka.warworlds.SectorY", centreStar.sector_y);
                    intent.putExtra("au.com.codeka.warworlds.OffsetX", centreStar.offset_x);
                    intent.putExtra("au.com.codeka.warworlds.OffsetY", centreStar.offset_y);
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
                intent.putExtra("au.com.codeka.warworlds.StarKey", mSelectedStar.key);
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

                if (savedInstanceState != null) {
                    Star selectedStar = null;
                    Fleet selectedFleet = null;

                    byte[] star_bytes = savedInstanceState.getByteArray("au.com.codeka.warworlds.SelectedStar");
                    if (star_bytes != null) {
                        try {
                            selectedStar = Model.wire.parseFrom(star_bytes, Star.class);
                        } catch (IOException e) { }
                    }

                    byte[] fleet_bytes = savedInstanceState.getByteArray("au.com.codeka.warworlds.SelectedFleet");
                    if (fleet_bytes != null) {
                        try {
                            selectedFleet = Model.wire.parseFrom(fleet_bytes, Fleet.class);
                        } catch (IOException e) { }
                    }

                    if (selectedStar != null) {
                        mSelectedStar = selectedStar;
                        mStarfield.selectStar(selectedStar.key);
                        mStarfield.scrollTo(selectedStar.sector_x, selectedStar.sector_y,
                                            selectedStar.offset_x, selectedStar.offset_y,
                                            true);
                    }
                    if (selectedFleet != null) {
                        mStarfield.selectFleet(selectedFleet);
                    }
                }
                if (savedInstanceState == null) {
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
                }

                Empire myEmpire = EmpireManager.i.getEmpire();
                if (myEmpire == null) {
                    return;
                }

                Star homeStar = myEmpire.home_star;
                if (homeStar != null) {
                    mStarfield.scrollTo(homeStar.sector_x, homeStar.sector_y,
                                        homeStar.offset_x, homeStar.offset_y,
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

        StarManager.i.addStarUpdatedListener(null, this);
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

        StarManager.i.removeStarUpdatedListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (mSelectedStar != null) {
            state.putByteArray("au.com.codeka.warworlds.SelectedStar", mSelectedStar.toByteArray());
        }

        if (mSelectedFleet != null) {
            state.putByteArray("au.com.codeka.warworlds.SelectedFleet", mSelectedFleet.toByteArray());
        }
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
            starKey = c.star_key;
        }

        if (starKey != null) {
            StarManager.i.requestStarSummary(starKey, new StarManager.StarSummaryFetchedHandler() {
                @Override
                public void onStarSummaryFetched(Star s) {
                    mStarfield.scrollTo(s.sector_x, s.sector_y,
                                        s.offset_x, s.offset_y,
                                        true);
                }
            });
        }
    }

    public void openEmpireActivityAtFleet(Star star, Fleet fleet) {
        Intent intent = new Intent(mContext, EmpireActivity.class);
        intent.putExtra("au.com.codeka.warworlds.StarKey", star.key);
        intent.putExtra("au.com.codeka.warworlds.FleetKey", fleet.key);
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
        navigateToPlanet(star.sector_x, star.sector_y, star.key,
                         star.offset_x, star.offset_y, planet.index,
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
        Star star = SectorManager.i.findStar(starKey);
        if (star == null) {
            StarManager.i.requestStar(starKey, false,
                new StarManager.StarFetchedHandler() {
                    @Override
                    public void onStarFetched(Star s) {
                        Fleet fleet = Model.findFleet(s, fleetKey);
                        if (fleet != null) {
                            navigateToFleet(s, fleet);
                        }
                    }
                });
        } else {
            Fleet fleet = Model.findFleet(star, fleetKey);
            if (fleet != null) {
                navigateToFleet(star, fleet);
            }
        }
    }

    public void navigateToFleet(Star star, Fleet fleet) {
        int offsetX = star.offset_x - (int) ((mStarfield.getWidth() / 2) / mStarfield.getPixelScale());
        int offsetY = star.offset_y - (int) ((mStarfield.getHeight() / 2) / mStarfield.getPixelScale());

        // todo: if the fleet is moving, scroll to it...

        mStarfield.scrollTo(star.sector_x, star.sector_y, offsetX, offsetY);

        if (fleet.state == Fleet.FLEET_STATE.MOVING) {
            mStarfield.selectFleet(fleet);
        } else {
            mStarfield.selectStar(star.key);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == SOLAR_SYSTEM_REQUEST && intent != null) {
            boolean wasSectorUpdated = intent.getBooleanExtra(
                    "au.com.codeka.warworlds.SectorUpdated", false);
            long sectorX = intent.getLongExtra("au.com.codeka.warworlds.SectorX", 0);
            long sectorY = intent.getLongExtra("au.com.codeka.warworlds.SectorY", 0);
            String starKey = intent.getStringExtra("au.com.codeka.warworlds.StarKey");

            if (wasSectorUpdated) {
                SectorManager.i.refreshSector(sectorX, sectorY);
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
        if (mSelectedStar != null && mSelectedStar.key.equals(star.key)) {
            updateStarSelection();
            return;
        }

        final View selectionLoadingContainer = findViewById(R.id.loading_container);
        final View selectedStarContainer = findViewById(R.id.selected_star);
        final View selectedFleetContainer = findViewById(R.id.selected_fleet);

        // load the rest of the star's details as well
        selectionLoadingContainer.setVisibility(View.VISIBLE);
        selectedStarContainer.setVisibility(View.GONE);
        selectedFleetContainer.setVisibility(View.GONE);
        mFetchingStarKey = star.key;
        mFetchingFleetKey = null;
        mSelectedFleet = null;

        StarManager.i.requestStar(star.key, true, this);
    }

    @Override
    public void onStarFetched(Star star) {
        if (mSelectedStar != null && mSelectedStar.key.equals(star.key)) {
            // if it's the star we already have selected, then we may as well refresh
            // whatever we've got.
            mFetchingStarKey = mSelectedStar.key;
        }
        if (mFetchingStarKey == null ||
            !mFetchingStarKey.equals(star.key)) {
            return;
        }

        mSelectedStar = star;
        updateStarSelection();
    }

    private void updateStarSelection() {
        final View selectionLoadingContainer = findViewById(R.id.loading_container);
        final View selectedStarContainer = findViewById(R.id.selected_star);
        final View selectedFleetContainer = findViewById(R.id.selected_fleet);
        final TextView starName = (TextView) findViewById(R.id.star_name);
        final TextView starKind = (TextView) findViewById(R.id.star_kind);
        final ImageView starIcon = (ImageView) findViewById(R.id.star_icon);
        final Button renameButton = (Button) findViewById(R.id.rename_btn);

        selectionLoadingContainer.setVisibility(View.GONE);
        selectedStarContainer.setVisibility(View.VISIBLE);
        selectedFleetContainer.setVisibility(View.GONE);

        mPlanetList.setStar(mSelectedStar);
        mFleetList.setStar(mSelectedStar);

        Empire myEmpire = EmpireManager.i.getEmpire();
        int numMyEmpire = 0;
        int numOtherEmpire = 0;
        for (Colony colony : mSelectedStar.colonies) {
            if (colony.empire_key == null) {
                continue;
            }
            if (colony.empire_key.equals(myEmpire.key)) {
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

        starName.setText(mSelectedStar.name);
        int offsetX = (int)(mSelectedStar.offset_x / (float) Model.SECTOR_SIZE * 1000.0f);
        if (mSelectedStar.sector_x < 0) {
            offsetX = 1000 - offsetX;
        }
        offsetX /= Model.PIXELS_PER_PARSEC;
        int offsetY = (int)(mSelectedStar.offset_y / (float) Model.SECTOR_SIZE * 1000.0f);
        if (mSelectedStar.sector_y < 0) {
            offsetY = 1000 - offsetY;
        }
        offsetY /= Model.PIXELS_PER_PARSEC;
        starKind.setText(String.format(Locale.ENGLISH, "%s [%d.%02d,%d.%02d]",
                StarType.get(mSelectedStar).getShortName(),
                mSelectedStar.sector_x, offsetX,
                mSelectedStar.sector_y, offsetY));
        Sprite starImage = StarImageManager.getInstance().getSprite(mSelectedStar, 80, true);
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
        mFetchingFleetKey = fleet.key;
        mFetchingStarKey = null;
        mSelectedStar = null;
        mSelectedFleet = fleet;

        ShipDesign design = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, fleet.design_id);
        EmpireManager.i.fetchEmpire(fleet.empire_key, new EmpireManager.EmpireFetchedHandler() {
            @Override
            public void onEmpireFetched(Empire empire) {
                if (mFetchingFleetKey == null ||
                    !mFetchingFleetKey.equals(fleet.key)) {
                    return;
                }
                empireName.setText(empire.display_name);
                empireIcon.setImageBitmap(EmpireHelper.getShield(mContext, empire));
            }
        });

        fleetDesign.setText(design.getDisplayName());
        fleetIcon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(design.getSpriteName())));

        String eta = "???";
        Star srcStar = SectorManager.i.findStar(fleet.star_key);
        Star destStar = SectorManager.i.findStar(fleet.destination_star_key);
        if (srcStar != null && destStar != null) {
            float timeRemainingInHours = Model.getTimeToDestination(fleet, srcStar, destStar);
            eta = TimeInHours.format(timeRemainingInHours);
        }

        String details = String.format(Locale.ENGLISH,
            "<b>Ships:</b> %d<br />" +
            "<b>Speed:</b> %.2f pc/hr<br />" +
            "<b>Destination:</b> %s<br />" +
            "<b>ETA:</b> %s",
            (int) Math.ceil(fleet.num_ships), design.getSpeedInParsecPerHour(),
            (destStar == null ? "???" : destStar.name),
            eta);
        fleetDetails.setText(Html.fromHtml(details));

        selectionLoadingContainer.setVisibility(View.GONE);
        selectedStarContainer.setVisibility(View.GONE);
        selectedFleetContainer.setVisibility(View.VISIBLE);
    }
}
