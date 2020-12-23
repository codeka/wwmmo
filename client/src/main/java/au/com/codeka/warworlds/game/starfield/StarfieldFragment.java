package au.com.codeka.warworlds.game.starfield;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import com.android.billingclient.api.Purchase;
import com.google.common.collect.Lists;

import java.util.Locale;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.BaseEmpire;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.model.BaseStar.StarType;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.ctrl.InfobarView;
import au.com.codeka.warworlds.ctrl.MiniChatView;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.ScoutReportDialog;
import au.com.codeka.warworlds.game.StarRenameDialog;
import au.com.codeka.warworlds.game.chat.ChatFragmentArgs;
import au.com.codeka.warworlds.game.empire.EmpireFragmentArgs;
import au.com.codeka.warworlds.game.starfield.scene.StarfieldManager;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.PurchaseManager;
import au.com.codeka.warworlds.model.ShieldManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.ui.BaseFragment;

/**
 * The {@link StarfieldFragment} is the "home" screen of the game, and displays the
 * starfield where you scroll around and interact with stars, etc.
 */
public class StarfieldFragment extends BaseFragment {
  private static final Log log = new Log("StarfieldActivity");
  private Star selectedStar;
  private Fleet selectedFleet;
  private Star homeStar;
  private View bottomPane;
  private Button allianceBtn;
  private SelectionDetailsView selectionDetailsView;

  private Purchase starRenamePurchase;

  @Nullable private StarfieldFragmentArgs args;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.starfield, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    if (getArguments() != null) {
      args = StarfieldFragmentArgs.fromBundle(requireArguments());
    }

    bottomPane = view.findViewById(R.id.bottom_pane);
    selectionDetailsView = view.findViewById(R.id.selection_details);

    if (isPortrait()) {
      InfobarView infobarView = view.findViewById(R.id.infobar);
      infobarView.hideEmpireName();
    }

    MiniChatView miniChatView = view.findViewById(R.id.mini_chat);
    miniChatView.setup(conversationID -> {
      ChatFragmentArgs args = new ChatFragmentArgs.Builder()
          .setConversationID(conversationID)
          .build();
      NavHostFragment.findNavController(StarfieldFragment.this).navigate(
          R.id.chatFragment, args.toBundle());
    });

    // "Rename" button
    // "View" button
    // "Intel" button
    selectionDetailsView.setHandlers(
        planet -> navigateToPlanet(selectedStar, planet, false),
        fleet -> {
          if (selectedStar == null) {
            return; //??
          }

          openEmpireActivityAtFleet(selectedStar, fleet);
        },
        v -> onRenameClick(), v -> {
          if (selectedStar == null) {
            return;
          }

          navigateToPlanet(selectedStar, null, false);
        }, v -> {
          if (selectedStar != null) {
            ScoutReportDialog dialog = new ScoutReportDialog();
            dialog.setStar(selectedStar);
            dialog.show(getChildFragmentManager(), "");
          }
        }, star -> {
          StarfieldManager starfieldManager = requireMainActivity().getStarfieldManager();
          starfieldManager.warpTo(star);
        });

    view.findViewById(R.id.empire_btn).setOnClickListener(v -> openEmpireActivity());
    view.findViewById(R.id.sitrep_btn).setOnClickListener(v -> openSitrepActivity());

    allianceBtn = view.findViewById(R.id.alliance_btn);
    allianceBtn.setOnClickListener(v -> onAllianceClick());

    hideBottomPane(true);
  }

  @Override
  public void onResume() {
    super.onResume();

    MyEmpire myEmpire = EmpireManager.i.getEmpire();
    if (myEmpire == null) {
      return;
    }

    BaseStar homeStar = myEmpire.getHomeStar();
    if (homeStar != null && (
        StarfieldFragment.this.homeStar == null || !StarfieldFragment.this.homeStar.getKey()
        .equals(homeStar.getKey()))) {
      StarfieldFragment.this.homeStar = (Star) homeStar;
    }

    if (args != null && args.getStarCoord() != null) {
      requireMainActivity().getStarfieldManager().warpTo(args.getStarCoord());
    }

    // Now that we've processed the arguments, don't do it again.
    setArguments(null);
    args = null;

    if (starRenamePurchase != null) {
      showStarRenamePopup(starRenamePurchase);
      starRenamePurchase = null;
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    StarManager.eventBus.register(eventHandler);
    ShieldManager.eventBus.register(eventHandler);
    requireMainActivity().getStarfieldManager().addTapListener(tapListener);
  }

  @Override
  public void onStop() {
    super.onStop();
    requireMainActivity().getStarfieldManager().removeTapListener(tapListener);
    StarManager.eventBus.unregister(eventHandler);
    ShieldManager.eventBus.unregister(eventHandler);
  }

  public void openEmpireActivityAtFleet(Star star, Fleet fleet) {
    EmpireFragmentArgs args = new EmpireFragmentArgs.Builder()
        .setStarID(star.getID())
        .setFleetID(fleet.getID())
        .build();
    NavHostFragment.findNavController(this).navigate(R.id.empireFragment, args.toBundle());
  }

  public void openEmpireActivity() {
    NavHostFragment.findNavController(this).navigate(R.id.empireFragment);
  }

  public void openSitrepActivity() {
    NavHostFragment.findNavController(this).navigate(R.id.sitrepFragment);
  }

  public void onAllianceClick() {
    NavHostFragment.findNavController(this).navigate(R.id.allianceFragment);
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
        bottomPane.getLayoutParams().width =
            initialWidth + (int) ((pxWidth - initialWidth) * interpolatedTime);
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
        bottomPane.getLayoutParams().height =
            initialHeight + (int) ((pxHeight - initialHeight) * interpolatedTime);
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
  public void navigateToPlanet(Star star, @Nullable Planet planet, boolean scrollView) {
    navigateToPlanet(star.getStarType(), star.getSectorX(), star.getSectorY(), star.getID(),
        star.getOffsetX(), star.getOffsetY(), planet == null ? -1 : planet.getIndex(), scrollView);
  }

  private void navigateToPlanet(StarType starType, long sectorX, long sectorY, int starID,
      int starOffsetX, int starOffsetY, int planetIndex, boolean scrollView) {
    if (scrollView) {
      StarfieldManager starfieldManager = requireMainActivity().getStarfieldManager();
      starfieldManager.warpTo(sectorX, sectorY, starOffsetX, starOffsetY);
    }

    if (starType.getType() == Star.Type.Wormhole) {
      NavHostFragment.findNavController(this).navigate(
          StarfieldFragmentDirections.actionStarfieldFragmentToWormholeFragment(starID));
    } else {
      NavHostFragment.findNavController(this).navigate(
          StarfieldFragmentDirections.actionStarfieldFragmentToSolarSystemFragment(starID)
              .setPlanetIndex(planetIndex));
    }
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
    StarfieldManager starfieldManager = requireMainActivity().getStarfieldManager();
    starfieldManager.warpTo(star);
    starfieldManager.setSelectedFleet(star, (Fleet) fleet);
  }

  public void onRenameClick() {
    // If you're an Empire-level patron, skip the cost stuff, it's free for you!
    if (EmpireManager.i.getEmpire().getPatreonLevel() == BaseEmpire.PatreonLevel.EMPIRE) {
      doRenameStar();
      return;
    }

    PurchaseManager.i.querySkus(Lists.newArrayList("star_rename"), (billingResult, skuDetails) -> {
      if (skuDetails == null) {
        // TODO: handle error
        return;
      }

      new StyledDialog.Builder(requireContext()).setMessage(String.format(Locale.ENGLISH,
          "Renaming stars costs %s. If you wish to continue, you'll be directed " +
              "to the Play Store where you can purchase a one-time code to rename this " +
              "star. Are you sure you want to continue?", skuDetails.get(0).getPrice()))
          .setTitle("Rename Star").setNegativeButton("Cancel", null)
          .setPositiveButton("Rename", (dialog, which) -> {
            doRenameStar();
            dialog.dismiss();
          }).create().show();
    });
  }

  public void doRenameStar() {
    if (selectedStar == null) {
      return;
    }

    PurchaseManager.i
        .launchPurchaseFlow(requireActivity(), "star_rename", (purchase) -> {
          if (selectedStar == null) {
            return;
          }

          try {
            showStarRenamePopup(purchase);
          } catch (IllegalStateException e) {
            // this can be called before the activity is resumed, so we just set a
            // flag that'll cause us to pop up the dialog when the activity is resumed.
            log.warning(
                "Got an error trying to show the popup, we'll try again in a second...");
            starRenamePurchase = purchase;
          }
        });
  }

  private void showStarRenamePopup(Purchase purchase) {
    StarRenameDialog dialog = new StarRenameDialog();
    dialog.setPurchaseInfo(purchase);
    dialog.setStar(selectedStar);
    dialog.show(getChildFragmentManager(), "");
  }

  /*

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
      } else if (starKey != null && starfield.getScene() != null) {
        // make sure we re-select the star you had selected before.
        starfield.getScene().selectStar(starKey);
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
*/
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

      if (selectedFleet != null) {/*
        StarfieldActivity.this.getEngine().runOnUpdateThread(new Runnable() {
          @Override
          public void run() {
            // this will cause the selected fleet info to redraw and hence the shield
            StarfieldActivity.this.onFleetSelected(selectedFleet);
          }
        });*/
      }
    }
  };

  private final StarfieldManager.TapListener tapListener = new StarfieldManager.TapListener() {
    @Override
    public void onStarTapped(@Nullable Star star) {
      onStarSelected(star);
    }

    @Override
    public void onFleetTapped(@Nullable Star star, @Nullable Fleet fleet) {
      onFleetSelected(fleet);
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
