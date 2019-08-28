package au.com.codeka.warworlds.server.model;

import au.com.codeka.common.model.BasePlanet;

public class Planet extends BasePlanet {
    public Planet() {
    }
    public Planet(Star star, int planetIndex, int planetTypeID, int size,
                  int populationCongeniality, int farmingCongeniality, int miningCongeniality) {
        this.star = star;
        index = planetIndex;
        planetType = sPlanetTypes[planetTypeID];
        this.size = size;
        this.populationCongeniality = populationCongeniality;
        this.farmingCongeniality = farmingCongeniality;
        this.miningCongeniality = miningCongeniality;
    }
}
