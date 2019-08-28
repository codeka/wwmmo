package au.com.codeka.warworlds.server.designeffects;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.*;
import org.w3c.dom.Element;

public class GoodsBoostBuildingEffect extends BuildingEffect {
  private static final Log log = new Log("GoodsBoostBuildingEffect");
  private float boost;
  private float minimumBoost;

  @Override
  public void load(Element effectElem) {
    boost = Float.parseFloat(effectElem.getAttribute("boost"));
    minimumBoost = Float.parseFloat(effectElem.getAttribute("minBoost"));
  }

  @Override
  public void apply(BaseStar star, BaseColony baseColony, BaseBuilding building) {
    int planetIndex = baseColony.getPlanetIndex() - 1;
    BasePlanet planet = star.getPlanets()[planetIndex];

    float extraGoods = planet.getFarmingCongeniality() * boost;
    if (extraGoods < minimumBoost) {
      extraGoods = minimumBoost;
    }
    planet.setFarmingCongeniality(planet.getFarmingCongeniality() + (int) extraGoods);
  }
}
