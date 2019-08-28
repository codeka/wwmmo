package au.com.codeka.warworlds.server.designeffects;

import org.w3c.dom.Element;

import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.model.BuildingEffect;

public class RadarBuildingEffect extends BuildingEffect {
  private float range;

  @Override
  public void load(Element effectElem) {
    range = Float.parseFloat(effectElem.getAttribute("range"));
  }

  @Override
  public void apply(BaseStar star, BaseColony baseColony, BaseBuilding building) {
  }

  public float getRange() {
    return range;
  }
}
