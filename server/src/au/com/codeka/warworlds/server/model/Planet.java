package au.com.codeka.warworlds.server.model;

import au.com.codeka.common.model.BasePlanet;

public class Planet extends BasePlanet {
    public Planet() {
    }
    public Planet(Star star, int planetIndex, int planetTypeID, int size,
                  int populationCongeniality, int farmingCongeniality, int miningCongeniality) {
        mStar = star;
        mIndex = planetIndex;
        mPlanetType = sPlanetTypes[planetTypeID];
        mSize = size;
        mPopulationCongeniality = populationCongeniality;
        mFarmingCongeniality = farmingCongeniality;
        mMiningCongeniality = miningCongeniality;
    }
}
