package au.com.codeka.warworlds.model.designeffects;

import org.w3c.dom.Element;

import au.com.codeka.common.XmlIterator;

public class EmptySpaceMoverShipEffect extends ShipEffect {
    private float mMinStarDistance;

    public EmptySpaceMoverShipEffect(Element effectElement) {
        super(effectElement);

        mMinStarDistance = 0.0f;

        for (Element starDistanceElement : XmlIterator.childElements(effectElement, "star-distance")) {
            if (starDistanceElement.getAttribute("min") != null) {
                mMinStarDistance = Float.parseFloat(starDistanceElement.getAttribute("min"));
            }
        }
    }

    public float getMinStarDistance() {
        return mMinStarDistance;
    }
}
