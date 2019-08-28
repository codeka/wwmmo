package au.com.codeka.warworlds.server.designeffects;

import org.w3c.dom.Element;

import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.model.BuildingEffect;
import au.com.codeka.warworlds.server.model.Colony;

public class PopulationBoostBuildingEffect extends BuildingEffect {
  private float boost;
  private float minimumBoost;

  @Override
  public void load(Element effectElem) {
    boost = Float.parseFloat(effectElem.getAttribute("boost"));
    minimumBoost = Float.parseFloat(effectElem.getAttribute("min"));
  }

  @Override
  public void apply(BaseStar star, BaseColony baseColony, BaseBuilding building) {
    Colony colony = (Colony) baseColony;
    float extraPopulation = colony.getMaxPopulation() * boost;
    if (extraPopulation < minimumBoost) {
      extraPopulation = minimumBoost;
    }
    colony.setMaxPopulation(colony.getMaxPopulation() + extraPopulation);
  }
}
