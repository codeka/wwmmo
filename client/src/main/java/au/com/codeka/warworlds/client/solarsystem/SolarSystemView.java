package au.com.codeka.warworlds.client.solarsystem;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.squareup.picasso.Picasso;

import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator;
import au.com.codeka.warworlds.client.world.ImageHelper;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.Vector2;
import au.com.codeka.warworlds.common.proto.Building;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * The view which displays the star and planets.
 */
public class SolarSystemView extends RelativeLayout {
  private static final Log log = new Log("SolarSystemView");
  private Star star;
  private PlanetInfo[] planetInfos;

  public SolarSystemView(Context context, AttributeSet attrs) {
    super(context, attrs);
    ViewBackgroundGenerator.setBackground(this);
  }

  public void setStar(Star star) {
    this.star = star;
    planetInfos = new PlanetInfo[star.planets.size()];
    for (int i = 0; i < star.planets.size(); i++) {
      PlanetInfo planetInfo = new PlanetInfo();
      planetInfo.planet = star.planets.get(i);
      planetInfo.centre = new Vector2(0, 0);
      planetInfo.distanceFromSun = 0.0f;
      planetInfos[i] = planetInfo;
    }

    placePlanets();
    //redraw();
  }

  private void placePlanets() {
    if (planetInfos == null) {
      return;
    }
    int width = getWidth();
    log.info("width == %d", width);
    if (width == 0) {
      this.post(new Runnable() {
        @Override
        public void run() {
          placePlanets();
        }
      });
      return;
    }

    width -= (int)(60 * getContext().getResources().getDisplayMetrics().density);
    float planetStart = 150 * getContext().getResources().getDisplayMetrics().density;
    float distanceBetweenPlanets = width - planetStart;
    distanceBetweenPlanets /= planetInfos.length;

    for (int i = 0; i < planetInfos.length; i++) {
      PlanetInfo planetInfo = planetInfos[i];

      float distanceFromSun =
          planetStart + (distanceBetweenPlanets * i) + (distanceBetweenPlanets / 2.0f);
      float x = distanceFromSun;
      float y = 0;

      float angle = (0.5f/(planetInfos.length));
      angle = (float) ((angle*(planetInfos.length - i - 1)*Math.PI) + angle*Math.PI);

      Vector2 centre = new Vector2(x, y);
      centre.rotate(angle);

      planetInfo.centre = centre;
      planetInfo.distanceFromSun = distanceFromSun;
      planetInfo.imageView = new ImageView(getContext());

      RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
          (int)(50 * getContext().getResources().getDisplayMetrics().density),
          (int)(50 * getContext().getResources().getDisplayMetrics().density));
      lp.topMargin = (int) centre.y;
      lp.leftMargin = (int) centre.x;
      planetInfo.imageView.setLayoutParams(lp);
      addView(planetInfo.imageView);

      Picasso.with(getContext())
          .load(ImageHelper.getPlanetImageUrl(getContext(), star, i, 50, 50))
          .into(planetInfo.imageView);

      if (planetInfo.planet.colony != null) {
        for (Building building : planetInfo.planet.colony.buildings) {
        //  BuildingDesign design = (BuildingDesign) DesignManager.i.getDesign(DesignKind.BUILDING, building.getDesignID());
        //  if (design.showInSolarSystem()) {
        //    planetInfo.buildings.add((Building) building);
        //  }
        }
      }

      //if (!planetInfo.buildings.isEmpty()) {
      //  Collections.sort(planetInfo.buildings, mBuildingDesignComparator);
      //}
    }

    updateSelection();
  }

  private void updateSelection() {
    //if (mSelectedPlanet != null && mSelectionView != null) {
    //  mSelectionView.setVisibility(View.VISIBLE);

    //  RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mSelectionView.getLayoutParams();
    //  params.width = (int) ((((mSelectedPlanet.planet.getSize() - 10.0) / 8.0) + 4.0) * 10.0) + (int) (40 * getPixelScale());
    //  params.height = params.width;
    //  params.leftMargin = (int) (getLeft() + mSelectedPlanet.centre.x - (params.width / 2));
    //  params.topMargin = (int) (getTop() + mSelectedPlanet.centre.y - (params.height / 2));
    //  mSelectionView.setLayoutParams(params);
    //}
  }

  /** This class contains info about the planets we need to render and interact with. */
  private static class PlanetInfo {
    public Planet planet;
    public Vector2 centre;
    public float distanceFromSun;
    public ImageView imageView;
  }
}
