package au.com.codeka.warworlds.model.designeffects;

import org.w3c.dom.Element;

public class WormholeDisruptorBuildingEffect extends BuildingEffect {
  private float range;

  public WormholeDisruptorBuildingEffect(Element effectElement) {
    super(effectElement);

    range = Float.parseFloat(effectElement.getAttribute("range"));
  }

  public float getRange() {
    return range;
  }
}
