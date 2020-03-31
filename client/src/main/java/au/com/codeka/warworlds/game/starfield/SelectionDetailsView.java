package au.com.codeka.warworlds.game.starfield;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.Html;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.joda.time.DateTime;

import java.util.Locale;

import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ctrl.FleetListSimple;
import au.com.codeka.warworlds.ctrl.PlanetListSimple;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.ShieldManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;

/** View that displays details about the currently selected object in the game world. */
public class SelectionDetailsView extends FrameLayout {
  private final View loadingContainer;
  private final View selectedStar;
  private final View selectedFleet;
  private final PlanetListSimple planetList;
  private final FleetListSimple fleetList;
  private Star star;
  private Star destStar;
  private ZoomToStarHandler zoomToStarHandler;

  public SelectionDetailsView(Context context, AttributeSet attrs) {
    super(context, attrs);
    LayoutInflater.from(context).inflate(R.layout.starfield_selection_details, this);

    loadingContainer = findViewById(R.id.loading_container);
    selectedStar = findViewById(R.id.selected_star);
    selectedFleet = findViewById(R.id.selected_fleet);
    planetList = (PlanetListSimple) findViewById(R.id.planet_list);
    fleetList = (FleetListSimple) findViewById(R.id.fleet_list);

    findViewById(R.id.wormhole_locate).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (destStar != null && zoomToStarHandler != null) {
          zoomToStarHandler.onZoomToStar(destStar);
        }
      }
    });
  }

  @Override
  public void onAttachedToWindow() {
    super.onAttachedToWindow();
    StarManager.eventBus.register(eventListener);
    EmpireManager.eventBus.register(eventListener);
    EmpireShieldManager.eventBus.register(eventListener);
  }

  @Override
  public void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    StarManager.eventBus.unregister(eventListener);
    EmpireManager.eventBus.unregister(eventListener);
    EmpireShieldManager.eventBus.unregister(eventListener);
  }

  /** Called to set the handlers for when the user selects a planet/fleet from our lists, etc. */
  public void setHandlers(PlanetListSimple.PlanetSelectedHandler planetSelectedHandler,
      FleetListSimple.FleetSelectedHandler fleetSelectedHandler,
      OnClickListener renameClickListener, OnClickListener viewClientListener,
      OnClickListener intelClickListener, ZoomToStarHandler zoomToStarHandler) {
    planetList.setPlanetSelectedHandler(planetSelectedHandler);
    fleetList.setFleetSelectedHandler(fleetSelectedHandler);
    findViewById(R.id.rename_btn).setOnClickListener(renameClickListener);
    findViewById(R.id.view_btn).setOnClickListener(viewClientListener);
    findViewById(R.id.scout_report_btn).setOnClickListener(intelClickListener);
    this.zoomToStarHandler = zoomToStarHandler;
  }

  /** Called when the user deselects whatever they had selected. Hide everything. */
  public void deselect() {
    loadingContainer.setVisibility(View.GONE);
    selectedStar.setVisibility(View.GONE);
    selectedFleet.setVisibility(View.GONE);
  }

  /** Called when we're loading something. Show the spinner. */
  public void loading() {
    loadingContainer.setVisibility(View.VISIBLE);
    selectedStar.setVisibility(View.GONE);
    selectedFleet.setVisibility(View.GONE);
  }

  /** Called when we're displaying info about the given star. Hide everything else. */
  public void showStar(Star s) {
    this.star = s;
    final View selectionLoadingContainer = findViewById(R.id.loading_container);
    final View selectedStarContainer = findViewById(R.id.selected_star);
    final View selectedFleetContainer = findViewById(R.id.selected_fleet);
    final TextView starName = findViewById(R.id.star_name);
    final TextView starKind = findViewById(R.id.star_kind);
    final ImageView starIcon = findViewById(R.id.star_icon);
    final Button renameButton = findViewById(R.id.rename_btn);

    selectionLoadingContainer.setVisibility(View.GONE);
    selectedStarContainer.setVisibility(View.VISIBLE);
    selectedFleetContainer.setVisibility(View.GONE);

    if (star.getStarType().getType() == Star.Type.Wormhole) {
      planetList.setVisibility(View.GONE);
      findViewById(R.id.wormhole_details).setVisibility(View.VISIBLE);
      refreshWormholeDetails();
    } else {
      findViewById(R.id.wormhole_details).setVisibility(View.GONE);
      planetList.setVisibility(View.VISIBLE);
      planetList.setStar(star);
    }

    fleetList.setStar(star);

    MyEmpire myEmpire = EmpireManager.i.getEmpire();
    int numMyEmpire = 0;
    int numOtherEmpire = 0;
    for (BaseColony colony : star.getColonies()) {
      if (colony.getEmpireKey() == null) {
        continue;
      }
      if (colony.getEmpireKey().equals(myEmpire.getKey())) {
        numMyEmpire++;
      } else {
        numOtherEmpire++;
      }
    }
    if (numMyEmpire > numOtherEmpire) {
      renameButton.setVisibility(View.VISIBLE);
    } else {
      renameButton.setVisibility(View.GONE);
    }

    starName.setText(star.getName());
    starKind.setText(String.format(Locale.ENGLISH, "%s %s", star.getStarType().getShortName(),
        star.getCoordinateString()));
    Sprite starImage = StarImageManager.getInstance().getSprite(star, 80, true);
    starIcon.setImageDrawable(new SpriteDrawable(starImage));
  }

  private void refreshWormholeDetails() {
    if (star.getStarType().getType() != Star.Type.Wormhole) {
      return;
    }

    destStar = null;
    if (star.getWormholeExtra().getDestWormholeID() != 0) {
      destStar = StarManager.i.getStar(star.getWormholeExtra().getDestWormholeID());
    }

    TextView destinationName = (TextView) findViewById(R.id.destination_name);
    if (destStar != null) {
      BaseStar.WormholeExtra wormholeExtra = star.getWormholeExtra();
      DateTime tuneCompleteTime = null;
      if (wormholeExtra.getTuneCompleteTime() != null &&
          wormholeExtra.getTuneCompleteTime().isAfter(DateTime.now())) {
        tuneCompleteTime = wormholeExtra.getTuneCompleteTime();
      }

      String str = String.format(Locale.ENGLISH, "→ %s", destStar.getName());
      if (tuneCompleteTime != null) {
        str = "<font color=\"red\">" + str + "</font>";
      }
      destinationName.setText(Html.fromHtml(str));
      findViewById(R.id.wormhole_locate).setEnabled(true);
    } else {
      findViewById(R.id.wormhole_locate).setEnabled(false);

      if (star.getWormholeExtra().getDestWormholeID() == 0) {
        destinationName.setText(Html.fromHtml("→ <i>None</i>"));
      } else {
        destinationName.setText(Html.fromHtml("→ ..."));
      }
    }

    Empire empire = EmpireManager.i.getEmpire(star.getWormholeExtra().getEmpireID());
    if (empire != null) {
      TextView empireName = (TextView) findViewById(R.id.empire_name);
      empireName.setVisibility(View.VISIBLE);
      empireName.setText(empire.getDisplayName());

      Bitmap bmp = EmpireShieldManager.i.getShield(getContext(), empire);
      ImageView empireIcon = (ImageView) findViewById(R.id.empire_icon);
      empireIcon.setVisibility(View.VISIBLE);
      empireIcon.setImageBitmap(bmp);
    } else {
      findViewById(R.id.empire_name).setVisibility(View.GONE);
      findViewById(R.id.empire_icon).setVisibility(View.GONE);
    }
  }

  public void showFleet(Fleet fleet) {
    final View selectionLoadingContainer = findViewById(R.id.loading_container);
    final View selectedStarContainer = findViewById(R.id.selected_star);
    final FleetInfoView fleetInfoView = (FleetInfoView) findViewById(R.id.selected_fleet);

    fleetInfoView.setFleet(fleet);
    selectionLoadingContainer.setVisibility(View.GONE);
    selectedStarContainer.setVisibility(View.GONE);
    fleetInfoView.setVisibility(View.VISIBLE);
  }

  private final Object eventListener = new Object() {
    @EventHandler
    public void onStarUpdated(Star s) {
      if (star == null) {
        return;
      }
      if (s.getKey().equals(star.getKey())) {
        star = s;
      }
      refreshWormholeDetails();
    }

    @EventHandler
    public void onEmpireUpdated(Empire empire) {
      if (star == null) {
        return;
      }
      refreshWormholeDetails();
    }

    @EventHandler
    public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
      if (star == null) {
        return;
      }
      refreshWormholeDetails();
    }
  };

  public interface ZoomToStarHandler {
    public void onZoomToStar(Star star);
  }
}
