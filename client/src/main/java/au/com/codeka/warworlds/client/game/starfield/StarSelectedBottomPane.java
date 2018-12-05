package au.com.codeka.warworlds.client.game.starfield;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.squareup.picasso.Picasso;

import java.util.Locale;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.ctrl.PlanetListSimple;
import au.com.codeka.warworlds.client.game.fleets.FleetListSimple;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.game.world.ImageHelper;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.sim.StarHelper;

/**
 * The bottom pane when you have a star selected.
 */
public class StarSelectedBottomPane extends FrameLayout {
  public interface Callback {
    void onStarClicked(Star star, @Nullable Planet planet);
    void onFleetClicked(Star star, Fleet fleet);
  }

  private final PlanetListSimple planetList;
  private final FleetListSimple fleetList;
  private final TextView starName;
  private final TextView starKind;
  private final ImageView starIcon;
  private final Button renameButton;

  private Star star;

  public StarSelectedBottomPane(Context context, Star star, Callback callback) {
    super(context, null);

    inflate(context, R.layout.starfield_bottom_pane_star, this);
    findViewById(R.id.view_btn).setOnClickListener((v) -> callback.onStarClicked(this.star, null));

    this.star = star;
    planetList = findViewById(R.id.planet_list);
    fleetList = findViewById(R.id.fleet_list);
    starName = findViewById(R.id.star_name);
    starKind = findViewById(R.id.star_kind);
    starIcon = findViewById(R.id.star_icon);
    renameButton = findViewById(R.id.rename_btn);

    planetList.setPlanetSelectedHandler(planet -> callback.onStarClicked(this.star, planet));

    fleetList.setFleetSelectedHandler(fleet -> callback.onFleetClicked(this.star, fleet));

    if (isInEditMode()) {
      return;
    }

    refresh();
  }

  @Override
  public void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (isInEditMode()) {
      return;
    }

    App.i.getEventBus().register(eventListener);
  }

  @Override
  public void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    if (isInEditMode()) {
      return;
    }

    App.i.getEventBus().unregister(eventListener);
  }

  private void refresh() {
    if (star.classification == Star.CLASSIFICATION.WORMHOLE) {
      planetList.setVisibility(View.GONE);
      findViewById(R.id.wormhole_details).setVisibility(View.VISIBLE);
//      refreshWormholeDetails();
    } else {
      findViewById(R.id.wormhole_details).setVisibility(View.GONE);
      planetList.setVisibility(View.VISIBLE);
      planetList.setStar(star);
    }

    fleetList.setStar(star);

    Empire myEmpire = Preconditions.checkNotNull(EmpireManager.i.getMyEmpire());
    int numMyEmpire = 0;
    int numOtherEmpire = 0;
    for (Planet planet : star.planets) {
      if (planet.colony == null || planet.colony.empire_id == null) {
        continue;
      }
      if (planet.colony.empire_id.equals(myEmpire.id)) {
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

    starName.setText(star.name);
    starKind.setText(String.format(Locale.ENGLISH, "%s %s", star.classification,
        StarHelper.getCoordinateString(star)));
    Picasso.get()
        .load(ImageHelper.getStarImageUrl(getContext(), star, 40, 40))
        .into(starIcon);
  }

  private final Object eventListener = new Object() {
    @EventHandler
    public void onStarUpdated(Star s) {
      if (star == null) {
        return;
      }
      if (s.id.equals(star.id)) {
        star = s;
      }
      refresh();
//      refreshWormholeDetails();
    }

    @EventHandler
    public void onEmpireUpdated(Empire empire) {
      if (star == null) {
        return;
      }
      refresh();
//      refreshWormholeDetails();
    }
  };
}
