package au.com.codeka.warworlds.server.designeffects;

import org.w3c.dom.Element;

import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.model.BuildingEffect;
import au.com.codeka.warworlds.server.model.Colony;

public class PopulationBoostBuildingEffect extends BuildingEffect {
    private float mBoost;
    private float mMinimumBoost;

    @Override
    public void load(Element effectElem) {
        mBoost = Float.parseFloat(effectElem.getAttribute("boost"));
        mMinimumBoost = Float.parseFloat(effectElem.getAttribute("min"));
    }

    @Override
    public void apply(BaseStar star, BaseColony baseColony, BaseBuilding building) {
        Colony colony = (Colony) baseColony;
        float extraPopulation = colony.getMaxPopulation() * mBoost;
        if (extraPopulation < mMinimumBoost) {
            extraPopulation = mMinimumBoost;
        }
        colony.setMaxPopulation(colony.getMaxPopulation() + extraPopulation);
    }
}
