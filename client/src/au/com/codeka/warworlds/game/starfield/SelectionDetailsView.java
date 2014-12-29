package au.com.codeka.warworlds.game.starfield;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;

import au.com.codeka.common.model.BaseColony;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ctrl.FleetListSimple;
import au.com.codeka.warworlds.ctrl.PlanetListSimple;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;

/** View that displays details about the currently selected object in the game world. */
public class SelectionDetailsView extends FrameLayout {
  private final View loadingContainer;
  private final View selectedStar;
  private final View selectedFleet;
  private final PlanetListSimple planetList;
  private final FleetListSimple fleetList;

  public SelectionDetailsView(Context context, AttributeSet attrs) {
    super(context, attrs);
    LayoutInflater.from(context).inflate(R.layout.starfield_selection_details, this);

    loadingContainer = findViewById(R.id.loading_container);
    selectedStar = findViewById(R.id.selected_star);
    selectedFleet = findViewById(R.id.selected_fleet);
    planetList = (PlanetListSimple) findViewById(R.id.planet_list);
    fleetList = (FleetListSimple) findViewById(R.id.fleet_list);
  }

  /** Called to set the handlers for when the user selects a planet/fleet from our lists, etc. */
  public void setHandlers(PlanetListSimple.PlanetSelectedHandler planetSelectedHandler,
      FleetListSimple.FleetSelectedHandler fleetSelectedHandler,
      OnClickListener renameClickListener, OnClickListener viewClientListener,
      OnClickListener intelClickListener) {
    planetList.setPlanetSelectedHandler(planetSelectedHandler);
    fleetList.setFleetSelectedHandler(fleetSelectedHandler);
    findViewById(R.id.rename_btn).setOnClickListener(renameClickListener);
    findViewById(R.id.view_btn).setOnClickListener(viewClientListener);
    findViewById(R.id.scout_report_btn).setOnClickListener(intelClickListener);
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
  public void showStar(Star star) {
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

    planetList.setStar(star);
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

  public void showFleet(Fleet fleet) {
    final View selectionLoadingContainer = findViewById(R.id.loading_container);
    final View selectedStarContainer = findViewById(R.id.selected_star);
    final FleetInfoView fleetInfoView = (FleetInfoView) findViewById(R.id.selected_fleet);

    fleetInfoView.setFleet(fleet);
    selectionLoadingContainer.setVisibility(View.GONE);
    selectedStarContainer.setVisibility(View.GONE);
    fleetInfoView.setVisibility(View.VISIBLE);
  }
}
