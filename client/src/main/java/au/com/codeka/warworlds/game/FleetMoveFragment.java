package au.com.codeka.warworlds.game;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import au.com.codeka.BackgroundRunner;
import au.com.codeka.Cash;
import au.com.codeka.common.Log;
import au.com.codeka.common.Vector2;
import au.com.codeka.common.model.BaseSector;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.ShipDesign;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.starfield.scene.StarfieldManager;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.Sector;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.designeffects.EmptySpaceMoverShipEffect;
import au.com.codeka.warworlds.opengl.SceneObject;
import au.com.codeka.warworlds.opengl.Sprite;
import au.com.codeka.warworlds.ui.BaseFragment;

/**
 * This activity is used to select a location for moves. It's a bit annoying that we have to do
 * it like this...
 */
public class FleetMoveFragment extends BaseFragment {
  private static final Log log = new Log("FleetMoveFragment");

  private Star srcStar;
  private Star destStar;
  private Fleet fleet;
  private float estimatedCost;
  private StarfieldManager starfieldManager;
  private Handler handler = new Handler();

  private FleetMoveIndicatorSceneObject fleetMoveIndicatorSceneObject;

  private Star markerStar;
//  private RadiusIndicatorEntity tooCloseIndicatorEntity;

  private View starDetailsView;
  private View instructionsView;
  private TextView starDetailsLeft;
  private TextView starDetailsRight;
  private Button moveBtn;

  private FleetMoveFragmentArgs args;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fleet_move, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    args = FleetMoveFragmentArgs.fromBundle(requireArguments());

    starDetailsView = view.findViewById(R.id.star_details);
    instructionsView = view.findViewById(R.id.instructions);
    starDetailsLeft = view.findViewById(R.id.star_details_left);
    starDetailsRight = view.findViewById(R.id.star_details_right);
    moveBtn = view.findViewById(R.id.move_btn);

    srcStar = StarManager.i.getStar(args.getStarID());
    init();

    Button cancelBtn = view.findViewById(R.id.cancel_btn);
    cancelBtn.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

    Button moveBtn = view.findViewById(R.id.move_btn);
    moveBtn.setOnClickListener(v -> {
      if (destStar != null) {
        onMoveClick();
      }
    });
  }

  /**
   * Called at various points during startup. We'll do nothing until all of the pre-requisites are
   * met (i.e. attached to the activity, loaded the star, etc). Then we'll set ourselves up.
   */
  public void init() {
    if (fleet != null) {
      // If the fleet's non-null that means we've finished initializing.
      return;
    }

    log.debug("init()");
    if (starfieldManager == null) {
      // Not attached to the activity yet.
      log.debug("Not attached to main activity.");
    }

    if (srcStar == null) {
      // Source star not loaded yet.
      log.debug("Source star not loaded yet.");
      return;
    }

    // We're done!
    starfieldManager.warpTo(srcStar);
    fleet = (Fleet) srcStar.getFleet(args.getFleetID());
    fleetMoveIndicatorSceneObject =
        new FleetMoveIndicatorSceneObject(starfieldManager, fleet);
    SceneObject sceneObject = starfieldManager.getStarSceneObject(srcStar.getID());
    if (sceneObject != null) {
      sceneObject.addChild(fleetMoveIndicatorSceneObject);
    }
    // Start off hidden, until you select a star.
    refreshSelection();
  }

  @Override
  public void onAttach(@NonNull @NotNull Context context) {
    super.onAttach(context);
    starfieldManager = requireMainActivity().getStarfieldManager();
    init();

    starfieldManager.addTapListener(tapListener);
  }

  @Override
  public void onDetach() {
    super.onDetach();

    starfieldManager.removeTapListener(tapListener);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (fleetMoveIndicatorSceneObject != null) {
      SceneObject sceneObject = fleetMoveIndicatorSceneObject.getParent();
      if (sceneObject != null) {
        sceneObject.removeChild(fleetMoveIndicatorSceneObject);
      }
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    StarManager.eventBus.register(eventHandler);
  }

  @Override
  public void onStop() {
    super.onStop();
    StarManager.eventBus.unregister(eventHandler);

    // if we have a marker star, make sure we remove it first
    if (markerStar != null) {
      Sector s = SectorManager.i.getSector(markerStar.getSectorX(), markerStar.getSectorY());
      if (s != null) {
        s.getStars().remove(markerStar);
      }
    }
  }

  public Object eventHandler = new Object() {
    @EventHandler
    public void onStarUpdated(Star star) {
      if (star.getID() == args.getStarID()) {
        if (srcStar == null) {
          srcStar = star;
          init();
        }

        refreshSelection();
      }
    }

    /*
        @EventHandler
        public void onStarSelected(final StarfieldSceneManager.StarSelectedEvent event) {
          if (event.star == null) {
            destStar = null;
            refreshSelection();
            return;
          }

          if (srcStar != null && event.star.getKey().equals(srcStar.getKey())) {
            // if src & dest are the same, just forget about it
            destStar = null;
          } else {
            destStar = event.star;
          }

          refreshSelection();
        }

        @EventHandler
        public void onSpaceTap(final StarfieldSceneManager.SpaceTapEvent event) {
          // if the fleet you're moving has the 'empty space mover' effect, it means you can move it
          // to regions of empty space.
          if (!fleet.getDesign().hasEffect("empty-space-mover")) {
            return;
          }

          // when moving to a region of empty space, we need to place a special "marker" star
          // at the destination (since everything else we do assume you're moving to a star)
          if (markerStar != null) {
            Sector s = SectorManager.i.getSector(markerStar.getSectorX(), markerStar.getSectorY());
            if (s != null) {
              s.getStars().remove(markerStar);
            }
          }
          markerStar = new Star(BaseStar.getStarType(Star.Type.Marker), "Marker", 20, event.sectorX,
              event.sectorY, event.offsetX, event.offsetY, null);
          Sector s = SectorManager.i.getSector(event.sectorX, event.sectorY);
          if (s != null) {
            s.getStars().add(markerStar);
          }
          SectorManager.eventBus.publish(s);

          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              StarfieldScene scene = starfield.getScene();
              if (scene != null) {
                scene.selectStar(markerStar.getKey());
              }
            }
          });
        }
      };

      @Override
      protected int getLayoutID() {
        return R.layout.fleet_move;
      }
    */
  };

  private void refreshSelection() {
    if (srcStar == null) {
      return;
    }

    Vector2 srcPoint = starfieldManager.calculatePosition(srcStar);
    if (destStar == null) {
      instructionsView.setVisibility(View.VISIBLE);
      starDetailsView.setVisibility(View.GONE);
      moveBtn.setEnabled(false);

      fleetMoveIndicatorSceneObject.reset(srcPoint, null);
    } else {
      instructionsView.setVisibility(View.GONE);
      starDetailsView.setVisibility(View.VISIBLE);
      moveBtn.setEnabled(true);

      Vector2 destPoint = starfieldManager.calculatePosition(destStar);
      fleetMoveIndicatorSceneObject.reset(srcPoint, destPoint);

      float distanceInParsecs = Sector.distanceInParsecs(srcStar, destStar);
      ShipDesign design =
          (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, fleet.getDesignID());

    String leftDetails = String
        .format(Locale.ENGLISH, "<b>Star:</b> %s<br /><b>Distance:</b> %.2f pc",
            destStar.getName(), distanceInParsecs);
    starDetailsLeft.setText(Html.fromHtml(leftDetails));

    float timeInHours = distanceInParsecs / design.getSpeedInParsecPerHour();
    int hrs = (int) Math.floor(timeInHours);
    int mins = (int) Math.floor((timeInHours - hrs) * 60.0f);

    estimatedCost = design.getFuelCost(distanceInParsecs, fleet.getNumShips());
    String cash = Cash.format(estimatedCost);

    String fontOpen = "";
    String fontClose = "";
    if (estimatedCost > EmpireManager.i.getEmpire().getCash()) {
      fontOpen = "<font color=\"#ff0000\">";
      fontClose = "</font>";
    }

    String rightDetails = String
        .format(Locale.ENGLISH, "<b>ETA:</b> %d hrs, %d mins<br />%s<b>Cost:</b> %s%s", hrs, mins,
            fontOpen, cash, fontClose);
    starDetailsRight.setText(Html.fromHtml(rightDetails));

    // if it's the marker star, make sure it's not too close to existing stars
    if (destStar == markerStar) {
      EmptySpaceMoverShipEffect effect =
          fleet.getDesign().getEffect(EmptySpaceMoverShipEffect.class);
      float minDistance = effect.getMinStarDistance();
      if (minDistance > 0) {
        float distanceToClosestStar = findClosestStar(markerStar);
        if (distanceToClosestStar < minDistance) {
//          if (tooCloseIndicatorEntity == null || tooCloseIndicatorEntity.isDisposed()) {
//            tooCloseIndicatorEntity = new RadiusIndicatorEntity(this);
//            tooCloseIndicatorEntity.setScale(minDistance * Sector.PIXELS_PER_PARSEC * 2.0f);
//          }
//          if (!markerStar.getAttachedEntities().contains(tooCloseIndicatorEntity)) {
//            markerStar.getAttachedEntities().add(tooCloseIndicatorEntity);
//            starfield.queueRefreshScene();
          }
        }
      }
    }
  }

  /**
   * Searches for, and returns the distance to, the clostest star to the given star.
   */
  private float findClosestStar(Star toStar) {
    float minDistance = -1.0f;
    for (Star star : SectorManager.i.getAllVisibleStars()) {
      if (star == toStar || star.getKey().equals(toStar.getKey())) {
        continue;
      }
      float distance = Sector.distanceInParsecs(star, toStar);
      if (distance < minDistance || minDistance < 0.0f) {
        minDistance = distance;
      }
    }
    return minDistance;
  }

  private void onMoveClick() {
    if (destStar == null) {
      return;
    }

    moveBtn.setEnabled(false);

    EmpireManager.i.getEmpire().addCash(-estimatedCost);

    new BackgroundRunner<Boolean>() {
      private String mErrorMessage;

      @Override
      protected Boolean doInBackground() {
        String url = String.format("stars/%s/fleets/%s/orders", fleet.getStarKey(), fleet.getKey());
        Messages.FleetOrder.Builder builder =
            Messages.FleetOrder.newBuilder().setOrder(Messages.FleetOrder.FLEET_ORDER.MOVE);
        if (markerStar != null) {
          builder.setSectorX(markerStar.getSectorX());
          builder.setSectorY(markerStar.getSectorY());
          builder.setOffsetX(markerStar.getOffsetX());
          builder.setOffsetY(markerStar.getOffsetY());
        } else {
          builder.setStarKey(destStar.getKey());
        }
        try {
          return ApiClient.postProtoBuf(url, builder.build());
        } catch (ApiException e) {
          mErrorMessage = e.getServerErrorMessage();
          return false;
        }
      }

      @Override
      protected void onComplete(Boolean success) {
        if (!success) {
          StyledDialog dialog =
              new StyledDialog.Builder(requireContext()).setTitle("Could not move fleet")
                  .setMessage(mErrorMessage
                      == null ? "Unable to move fleet, reason unknown." : mErrorMessage)
                  .setPositiveButton("OK", null).create();
          dialog.show();
          moveBtn.setEnabled(true);
        } else {
          // the star this fleet is attached to needs to be refreshed...
          StarManager.i.refreshStar(Integer.parseInt(fleet.getStarKey()));

          // the empire needs to be updated, too, since we'll have subtracted
          // the cost of this move from your cash
          EmpireManager.i.refreshEmpire(fleet.getEmpireID());

          NavHostFragment.findNavController(FleetMoveFragment.this).popBackStack();
        }
      }
    }.execute();
  }

  private final StarfieldManager.TapListener tapListener = new StarfieldManager.TapListener() {
    @Override
    public void onStarTapped(@Nullable Star star) {
      destStar = star;
      refreshSelection();
    }

    @Override
    public void onFleetTapped(@Nullable Star star, @Nullable Fleet fleet) {
      // Nothing to do.
    }
  };

  private class FleetMoveIndicatorSceneObject extends SceneObject {
    private final StarfieldManager starfieldManager;
    private final Sprite sprite;
    private final Vector2 pos;

    private Vector2 src;
    private Vector2 dest;
    private float rotation;
    private float fraction;

    public FleetMoveIndicatorSceneObject(StarfieldManager starfieldManager, Fleet fleet) {
      super(starfieldManager.getScene().getDimensionResolver());
      this.starfieldManager = starfieldManager;
      this.sprite = starfieldManager.createFleetSprite(fleet);
      addChild(sprite);
      this.pos = new Vector2();

      setDrawRunnable(frameTime -> {
        if (src == null) {
          return;
        }
        if (dest == null) {
          setTranslation(0.0f, 0.0f);
          return;
        }

        fraction += frameTime;
        if (fraction > 0.9f) {
          fraction = 0.1f;
        }
        Vector2.lerp(src, dest, fraction, pos);
        setTranslation((float)(pos.x - src.x), -(float)(pos.y - src.y));
        sprite.setRotation(rotation, 0, 0, 1);
      });
    }

    public void reset(Vector2 src, Vector2 dest) {
      this.src = src;
      this.dest = dest;

      if (src != null && dest != null) {
        Vector2 dir = BaseSector.directionBetween(srcStar, destStar);
        dir.normalize();
        rotation = Vector2.angleBetween(dir, new Vector2(0, -1));
      } else {
        rotation = 0f;
      }
    }
  }
}