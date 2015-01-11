package au.com.codeka.warworlds.model.designeffects;

import org.w3c.dom.Element;

public class RadarBuildingEffect extends BuildingEffect {
    private float mRange;

    public RadarBuildingEffect(Element effectElement) {
        super(effectElement);

        mRange = Float.parseFloat(effectElement.getAttribute("range"));
    }

    public float getRange() {
        return mRange;
    }
}
