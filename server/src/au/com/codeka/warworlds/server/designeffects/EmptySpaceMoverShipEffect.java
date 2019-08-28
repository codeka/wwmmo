package au.com.codeka.warworlds.server.designeffects;

import org.w3c.dom.Element;

import au.com.codeka.common.XmlIterator;
import au.com.codeka.common.model.ShipEffect;

public class EmptySpaceMoverShipEffect extends ShipEffect {
  private float minStarDistance;

  @Override
  public void load(Element effectElem) {
    for (Element childElement : XmlIterator.childElements(effectElem, "star-distance")) {
      minStarDistance = Float.parseFloat(childElement.getAttribute("min"));
    }
  }

  public float getMinStarDistance() {
    return minStarDistance;
  }
}
