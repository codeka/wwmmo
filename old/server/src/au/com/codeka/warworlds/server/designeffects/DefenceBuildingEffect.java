package au.com.codeka.warworlds.server.designeffects;

import org.w3c.dom.Element;

import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.model.BuildingEffect;
import au.com.codeka.warworlds.server.model.Colony;

public class DefenceBuildingEffect extends BuildingEffect {
    private float mBonus;

    @Override
    public void load(Element effectElem) {
        mBonus = Float.parseFloat(effectElem.getAttribute("bonus"));
    }

    @Override
    public void apply(BaseStar star, BaseColony baseColony, BaseBuilding building) {
        Colony colony = (Colony) baseColony;
        colony.setDefenceBoost(colony.getDefenceBoost() + mBonus);
    }
}
