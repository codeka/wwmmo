package au.com.codeka.warworlds.game.starfield;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Locale;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.model.BaseStar.StarType;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.ctrl.FleetListSimple;
import au.com.codeka.warworlds.ctrl.InfobarView;
import au.com.codeka.warworlds.ctrl.PlanetListSimple;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.ScoutReportDialog;
import au.com.codeka.warworlds.game.SitrepActivity;
import au.com.codeka.warworlds.game.StarRenameDialog;
import au.com.codeka.warworlds.game.alliance.AllianceActivity;
import au.com.codeka.warworlds.game.empire.EmpireActivity;
import au.com.codeka.warworlds.game.solarsystem.SolarSystemActivity;
import au.com.codeka.warworlds.game.wormhole.WormholeFragment;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.PurchaseManager;
import au.com.codeka.warworlds.model.Sector;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.ShieldManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.billing.IabException;
import au.com.codeka.warworlds.model.billing.IabHelper;
import au.com.codeka.warworlds.model.billing.IabResult;
import au.com.codeka.warworlds.model.billing.Purchase;
import au.com.codeka.warworlds.model.billing.SkuDetails;

/**
 * The {@link StarfieldActivity} is the "home" screen of the game, and displays the
 * starfield where you scroll around and interact with stars, etc.
 */
public class StarfieldActivity extends BaseStarfieldActivity {
  private static final Log log = new Log("StarfieldActivity");
  private Context context = this;
  private Star selectedStar;
  private Fleet selectedFleet;
  private Star homeStar;
  private View bottomPane;
  private Button allianceBtn;
  private SelectionDetailsView selectionDetailsView;

  private Purchase starRenamePurchase;

  private Star starToSelect;
  private String starKeyToSelect;
  private Fleet fleetToSelect;

  private boolean doNotNavigateToHomeStar;

  private static final int SOLAR_SYSTEM_REQUEST = 1;
  private static final int EMPIRE_REQUEST = 2;
  private static final int SITREP_REQUEST = 3;

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    bottomPane = findViewById(R.id.bottom_pane);
    selectionDetailsView = (SelectionDetailsView) findViewById(R.id.selection_details);

    if (isPortrait()) {
      InfobarView infobar = (InfobarView) findViewById(R.id.infobar);
      infobar.hideEmpireName();
    }

    selectionDetailsView.setHandlers(new PlanetListSimple.PlanetSelectedHandler() {
      @Override
      public void onPlanetSelected(Planet planet) {
          navigateToPlanet(selectedStar, planet, false);
          }
        }, new FleetListSimple.FleetSelectedHandler() {
          @Override
          public void onFleetSelected(Fleet fleet) {
            if (selectedStar == null) {
              return; //??
            }

            openEmpireActivityAtFleet(selectedStar, fleet);
          }
        }, new View.OnClickListener() { // "Rename" button
          @Override
          public void onClick(View v) {
            onRenameClick();
          }
        }, new View.OnClickListener() { // "View" button
          @Override
          public void onClick(View v) {
            if (selectedStar == null) {
              return;
            }

            Intent intent = new Intent(context, SolarSystemActivity.class);
            intent.putExtra("au.com.codeka.warworlds.StarKey", selectedStar.getKey());
            startActivityForResult(intent, SOLAR_SYSTEM_REQUEST);
          }
        }, new View.OnClickListener() { // "Intel" button
          @Override
          public void onClick(View v) {
            if (selectedStar != null) {
              ScoutReportDialog dialog = new ScoutReportDialog();
              dialog.setStar(selectedStar);
              dialog.show(getSupportFragmentManager(), "");
            }
          }
        }, new SelectionDetailsView.ZoomToStarHandler() {
          @Override
          public void onZoomToStar(Star star) {
            mStarfield.scrollTo(star.getSectorX(), star.getSectorY(),
                star.getOffsetX(), Sector.SECTOR_SIZE - star.getOffsetY());
          }
        });

    findViewById(R.id.empire_btn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        openEmpireActivity();
      }
    });
    findViewById(R.id.sitrep_btn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        openSitrepActivity();
      }
    });

    allianceBtn = (Button) findViewById(R.id.alliance_btn);
    allianceBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onAllianceClick();
      }
    });

    if (savedInstanceState != null) {
      Star selectedStar = null;
      Fleet selectedFleet = null;

      try {
        byte[] star_bytes = savedInstanceState.getByteArray("au.com.codeka.warworlds.SelectedStar");
        if (star_bytes != null) {
          Messages.Star star_pb = Messages.Star.parseFrom(star_bytes);
          selectedStar = new Star();
          selectedStar.fromProtocolBuffer(star_pb);
        }
      } catch (InvalidProtocolBufferException ignore) {
      }

      try {
        byte[] fleet_bytes =
            savedInstanceState.getByteArray("au.com.codeka.warworlds.SelectedFleet");
        if (fleet_bytes != null) {
          Messages.Fleet fleet_pb = Messages.Fleet.parseFrom(fleet_bytes);
          selectedFleet = new Fleet();
          selectedFleet.fromProtocolBuffer(fleet_pb);
        }
      } catch (InvalidProtocolBufferException ignore) {
      }

      starToSelect = selectedStar;
      fleetToSelect = selectedFleet;
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
          mStarfield.scrollTo(sectorX, sectorY, offsetX, Sector.SECTOR_SIZE - offsetY);
          doNotNavigateToHomeStar = true;
        }
      }
    }

    hideBottomPane(true);
  }

  @Override
  public void onResumeFragments() {
    super.onResumeFragments();

    ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
      @Override
      public void onHelloComplete(boolean success, ServerGreeter.ServerGreeting greeting) {
        if (!success) {
          return;
        }

        if (starToSelect != null && mStarfield.getScene() != null) {
          selectedStar = starToSelect;
          mStarfield.getScene().selectStar(starToSelect.getKey());
          mStarfield.scrollTo(starToSelect);
          starToSelect = null;
        }

        if (fleetToSelect != null && mStarfield.getScene() != null) {
          mStarfield.getScene().selectFleet(fleetToSelect.getKey());
          fleetToSelect = null;
        }

        MyEmpire myEmpire = EmpireManager.i.getEmpire();
        if (myEmpire == null) {
          return;
        }

        BaseStar homeStar = myEmpire.getHomeStar();
        if (homeStar != null && (
            StarfieldActivity.this.homeStar == null || !StarfieldActivity.this.homeStar.getKey()
            .equals(homeStar.getKey()))) {
          StarfieldActivity.this.homeStar = (Star) homeStar;
          if (!doNotNavigateToHomeStar) {
            mStarfield.scrollTo(homeStar);
          }
        }

        doNotNavigateToHomeStar = true;
      }
    });
  }

  @Override
  protected int getLayoutID() {
    return R.layout.starfield;
  }

  @Override
  public void onStart() {
    super.onStart();
    StarManager.eventBus.register(eventHandler);
    ShieldManager.eventBus.register(eventHandler);
    StarfieldSceneManager.eventBus.register(eventHandler);
  }

  @Override
  public void onPostResume() {
    super.onPostResume();

    if (starRenamePurchase != null) {
      showStarRenamePopup(starRenamePurchase);
      starRenamePurchase = null;
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    StarfieldSceneManager.eventBus.unregister(eventHandler);
    StarManager.eventBus.unregister(eventHandler);
    ShieldManager.eventBus.unregister(eventHandler);
  }

  @Override
  public void onSaveInstanceState(Bundle state) {
    super.onSaveInstanceState(state);
    if (selectedStar != null) {
      Messages.Star.Builder star_pb = Messages.Star.newBuilder();
      selectedStar.toProtocolBuffer(star_pb);
      state.putByteArray("au.com.codeka.warworlds.SelectedStar", star_pb.build().toByteArray());
    }

    if (selectedFleet != null) {
      Messages.Fleet.Builder fleet_pb = Messages.Fleet.newBuilder();
      selectedFleet.toProtocolBuffer(fleet_pb);
      state.putByteArray("au.com.codeka.warworlds.SelectedFleet", fleet_pb.build().toByteArray());
    }
  }

  public void openEmpireActivityAtFleet(Star star, Fleet fleet) {
    Intent intent = new Intent(context, EmpireActivity.class);
    intent.putExtra("au.com.codeka.warworlds.StarID", star.getID());
    intent.putExtra("au.com.codeka.warworlds.FleetID", Integer.parseInt(fleet.getKey()));
    startActivityForResult(intent, EMPIRE_REQUEST);
  }

  public void openEmpireActivity() {
    Intent intent = new Intent(context, EmpireActivity.class);
    startActivityForResult(intent, EMPIRE_REQUEST);
  }

  public void openSitrepActivity() {
    Intent intent = new Intent(context, SitrepActivity.class);
    startActivityForResult(intent, SITREP_REQUEST);
  }

  public void onAllianceClick() {
    Intent intent = new Intent(context, AllianceActivity.class);
    startActivity(intent);
  }

  public int getBottomPaneHeight() {
    return bottomPane.getHeight();
  }

  private void hideBottomPane(boolean instant) {
    applyBottomPaneAnimation(false, instant);
  }

  private void showBottomPane() {
    applyBottomPaneAnimation(true, false);
  }

  private void applyBottomPaneAnimation(boolean isOpen, boolean instant) {
    float dp;
    if (isPortrait()) {
      if (isOpen) {
        dp = 180;
      } else {
        dp = 34;
      }
    } else {
      if (isOpen) {
        dp = 200;
      } else {
        dp = 100;
      }
    }

    Resources r = getResources();
    float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());

    if (isPortrait()) {
      if (instant) {
        RelativeLayout.LayoutParams lp =
            (RelativeLayout.LayoutParams) bottomPane.getLayoutParams();
        lp.height = (int) px;
        bottomPane.setLayoutParams(lp);
      } else {
        applyBottomPaneAnimationPortrait(px);
      }
    } else {
      RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) allianceBtn.getLayoutParams();
      if (isOpen) {
        // NB: removeRule is not available until API level 17 :/
        lp.addRule(RelativeLayout.BELOW, 0);
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 1);
        lp.topMargin =
            (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 34, r.getDisplayMetrics());
      } else {
        lp.addRule(RelativeLayout.BELOW, R.id.empire_btn);
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        lp.topMargin =
            (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, r.getDisplayMetrics());
      }
      allianceBtn.setLayoutParams(lp);

      if (instant) {
        lp = (RelativeLayout.LayoutParams) bottomPane.getLayoutParams();
        lp.width = (int) px;
        bottomPane.setLayoutParams(lp);
      } else {
        applyBottomPaneAnimationLandscape(px);
      }
    }
  }

  private void applyBottomPaneAnimationLandscape(final float pxWidth) {
    Animation a = new Animation() {
      private int initialWidth;

      @Override
      protected void applyTransformation(float interpolatedTime, Transformation t) {
        final int newWidth = initialWidth + (int) ((pxWidth - initialWidth) * interpolatedTime);
        bottomPane.getLayoutParams().width = newWidth;
        bottomPane.requestLayout();
      }

      @Override
      public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
        initialWidth = width;
      }

      @Override
      public boolean willChangeBounds() {
        return true;
      }
    };
    a.setDuration(500);
    bottomPane.setAnimation(a);

  }

  private void applyBottomPaneAnimationPortrait(final float pxHeight) {
    Animation a = new Animation() {
      private int initialHeight;

      @Override
      protected void applyTransformation(float interpolatedTime, Transformation t) {
        final int newHeight =
            initialHeight + (int) ((pxHeight - initialHeight) * interpolatedTime);
        bottomPane.getLayoutParams().height = newHeight;
        bottomPane.requestLayout();
      }

      @Override
      public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
        initialHeight = height;
      }

      @Override
      public boolean willChangeBounds() {
        return true;
      }
    };
    a.setDuration(500);
    bottomPane.setAnimation(a);
  }

  /**
   * Navigates to the given planet in the given star. Starts the SolarSystemActivity.
   *
   * @param scrollView If {@code true}, we'll also scroll the current view so that given star is
   *                   centered on the given star.
   */
  public void navigateToPlanet(Star star, Planet planet, boolean scrollView) {
    navigateToPlanet(star.getStarType(), star.getSectorX(), star.getSectorY(), star.getKey(),
        star.getOffsetX(), star.getOffsetY(), planet.getIndex(), scrollView);
  }

  private void navigateToPlanet(StarType starType, long sectorX, long sectorY, String starKey,
      int starOffsetX, int starOffsetY, int planetIndex, boolean scrollView) {
    if (scrollView) {
      mStarfield.scrollTo(sectorX, sectorY, starOffsetX, Sector.SECTOR_SIZE - starOffsetY);
    }

    Intent intent;
    if (starType.getType() == Star.Type.Wormhole) {
      intent = new Intent(context, WormholeFragment.class);
    } else {
      intent = new Intent(context, SolarSystemActivity.class);
    }
    intent.putExtra("au.com.codeka.warworlds.StarKey", starKey);
    intent.putExtra("au.com.codeka.warworlds.PlanetIndex", planetIndex);
    startActivityForResult(intent, SOLAR_SYSTEM_REQUEST);
  }

  public void navigateToFleet(final String starKey, final String fleetKey) {
    Star star = StarManager.i.getStar(Integer.parseInt(starKey));
    if (star == null) {
      StarManager.eventBus.register(new Object() {
        @EventHandler
        public void onStarUpdated(Star star) {
          if (star.getKey().equals(starKey)) {
            navigateToFleet(star, star.findFleet(fleetKey));
            StarManager.eventBus.unregister(this);
          }
        }
      });

    } else {
      BaseFleet fleet = star.findFleet(fleetKey);
      if (fleet != null) {
        navigateToFleet(star, star.findFleet(fleetKey));
      }
    }
  }

  public void navigateToFleet(Star star, BaseFleet fleet) {
    int offsetX = star.getOffsetX();
    int offsetY = star.getOffsetY();

    mStarfield
        .scrollTo(star.getSectorX(), star.getSectorY(), offsetX, Sector.SECTOR_SIZE - offsetY);

    if (mStarfield.getScene() == null) {
      // TODO: we should probably remember the fleet then navigate when the scene is ready.
      return;
    }
    if (fleet.getState() == Fleet.State.MOVING) {
      mStarfield.getScene().selectFleet(fleet.getKey());
    } else {
      mStarfield.getScene().selectStar(star.getKey());
    }
  }

  public void onRenameClick() {
    SkuDetails starRenameSku;
    try {
      starRenameSku = PurchaseManager.i.getInventory().getSkuDetails("star_rename");
    } catch (IabException e) {
      log.error("Couldn't get SKU details!", e);
      return;
    }

    new StyledDialog.Builder(this).setMessage(String.format(Locale.ENGLISH,
        "Renaming stars costs %s. If you wish to continue, you'll be directed " +
            "to the Play Store where you can purchase a one-time code to rename this " +
            "star. Are you sure you want to continue?", starRenameSku.getPrice()))
        .setTitle("Rename Star").setNegativeButton("Cancel", null)
        .setPositiveButton("Rename", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            doRenameStar();
            dialog.dismiss();
          }
        }).create().show();
  }

  public void doRenameStar() {
    if (selectedStar == null) {
      return;
    }

    try {
      PurchaseManager.i
          .launchPurchaseFlow(this, "star_rename", new IabHelper.OnIabPurchaseFinishedListener() {
            @Override
            public void onIabPurchaseFinished(IabResult result, final Purchase info) {
              if (selectedStar == null) {
                return;
              }

              Purchase purchase = info;
              boolean isSuccess = result.isSuccess();
              if (result.isFailure()
                  && result.getResponse() == IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
                // if they've already purchased a star-renamed, but not reclaimed it, then
                // we let them through anyway.
                log.debug("Already purchased a star-rename, we'll just show the popup.");
                isSuccess = true;
                try {
                  purchase = PurchaseManager.i.getInventory().getPurchase("star_rename");
                } catch (IabException e) {
                  log.warning("Got an exception getting the purchase details.", e);
                }
              }

              if (isSuccess) {
                try {
                  showStarRenamePopup(purchase);
                } catch (IllegalStateException e) {
                  // this can be called before the activity is resumed, so we just set a
                  // flag that'll cause us to pop up the dialog when the activity is resumed.
                  log.warning(
                      "Got an error trying to show the popup, we'll try again in a second...");
                  starRenamePurchase = purchase;
                }
              }
            }
          });
    } catch (IabException e) {
      log.error("Couldn't get SKU details!", e);
    }
  }

  private void showStarRenamePopup(Purchase purchase) {
    StarRenameDialog dialog = new StarRenameDialog();
    dialog.setPurchaseInfo(purchase);
    dialog.setStar(selectedStar);
    dialog.show(getSupportFragmentManager(), "");
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);

    if (requestCode == SOLAR_SYSTEM_REQUEST && intent != null) {
      boolean wasSectorUpdated =
          intent.getBooleanExtra("au.com.codeka.warworlds.SectorUpdated", false);
      long sectorX = intent.getLongExtra("au.com.codeka.warworlds.SectorX", 0);
      long sectorY = intent.getLongExtra("au.com.codeka.warworlds.SectorY", 0);
      String starKey = intent.getStringExtra("au.com.codeka.warworlds.StarKey");

      if (wasSectorUpdated) {
        SectorManager.i.refreshSector(sectorX, sectorY);
      } else if (starKey != null && mStarfield.getScene() != null) {
        // make sure we re-select the star you had selected before.
        mStarfield.getScene().selectStar(starKey);
      } else if (starKey != null) {
        starKeyToSelect = starKey;
      }
    } else if (requestCode == EMPIRE_REQUEST && intent != null) {
      EmpireActivity.EmpireActivityResult res = EmpireActivity.EmpireActivityResult
          .fromValue(intent.getIntExtra("au.com.codeka.warworlds.Result", 0));

      final long sectorX = intent.getLongExtra("au.com.codeka.warworlds.SectorX", 0);
      final long sectorY = intent.getLongExtra("au.com.codeka.warworlds.SectorY", 0);
      final int starOffsetX = intent.getIntExtra("au.com.codeka.warworlds.StarOffsetX", 0);
      final int starOffsetY = intent.getIntExtra("au.com.codeka.warworlds.StarOffsetY", 0);
      final String starKey = intent.getStringExtra("au.com.codeka.warworlds.StarKey");

      if (res == EmpireActivity.EmpireActivityResult.NavigateToPlanet) {
        final int planetIndex = intent.getIntExtra("au.com.codeka.warworlds.PlanetIndex", 0);

        Star star = StarManager.i.getStar(Integer.parseInt(starKey));
        if (star == null) {
          StarManager.eventBus.register(new Object() {
            @EventHandler
            public void onStarUpdated(Star star) {
              if (star.getKey().equals(starKey)) {
                navigateToPlanet(star.getStarType(), sectorX, sectorY, starKey, starOffsetX,
                    starOffsetY, planetIndex, true);
                StarManager.eventBus.unregister(this);
              }
            }
          });
        } else {
          navigateToPlanet(star.getStarType(), sectorX, sectorY, starKey, starOffsetX, starOffsetY,
              planetIndex, true);
        }
      } else if (res == EmpireActivity.EmpireActivityResult.NavigateToFleet) {
        String fleetKey = intent.getStringExtra("au.com.codeka.warworlds.FleetKey");

        navigateToFleet(starKey, fleetKey);
      }
    }
  }

  private void handleDeselect() {
    selectedStar = null;
    selectedFleet = null;
    selectionDetailsView.deselect();
    hideBottomPane(false);
  }

  public Object eventHandler = new Object() {
    @EventHandler
    public void onStarFetched(Star star) {
      if (selectedStar != null && selectedStar.getID() == star.getID()) {
        selectedStar = star;
        selectionDetailsView.showStar(selectedStar);
      }
    }

    @EventHandler
    public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
      EmpireShieldManager.i.clearTextureCache();

      if (selectedFleet != null) {
        StarfieldActivity.this.getEngine().runOnUpdateThread(new Runnable() {
          @Override
          public void run() {
            // this will cause the selected fleet info to redraw and hence the shield
            StarfieldActivity.this.onFleetSelected(selectedFleet);
          }
        });
      }
    }

    @EventHandler
    public void onStarSelected(final StarfieldSceneManager.StarSelectedEvent event) {
      StarfieldActivity.this.onStarSelected(event.star);
    }

    @EventHandler
    public void onFleetSelected(final StarfieldSceneManager.FleetSelectedEvent event) {
      StarfieldActivity.this.onFleetSelected(event.fleet);
    }

    @EventHandler
    public void onSceneUpdated(final StarfieldSceneManager.SceneUpdatedEvent event) {
      if (starKeyToSelect != null) {
        event.scene.selectStar(starKeyToSelect);
      }
      if (starToSelect != null) {
        event.scene.selectStar(starToSelect.getKey());
      }
      if (fleetToSelect != null) {
        event.scene.selectFleet(fleetToSelect.getKey());
      }
      starToSelect = null;
      fleetToSelect = null;
      starKeyToSelect = null;
    }
  };

  private void onStarSelected(Star star) {
    if (star == null) {
      handleDeselect();
      return;
    }

    if (selectedStar != null && selectedStar.getKey().equals(star.getKey())) {
      selectionDetailsView.showStar(selectedStar);
      return;
    }
    selectedStar = star;
    selectedFleet = null;

    selectionDetailsView.loading();
    showBottomPane();

    // force the star to refresh itself
    StarManager.i.refreshStar(star.getID());
  }

  private void onFleetSelected(final Fleet fleet) {
    if (fleet == null) {
      handleDeselect();
      return;
    }

    selectedStar = null;
    selectedFleet = fleet;

    selectionDetailsView.showFleet(selectedFleet);
    showBottomPane();
  }
}
