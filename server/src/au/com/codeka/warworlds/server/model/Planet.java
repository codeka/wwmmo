package au.com.codeka.warworlds.server.model;

import com.google.api.client.util.Objects;

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

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("index", index)
        .add("type", planetType)
        .add("size", size)
        .add("populationCongeniality", populationCongeniality)
        .add("farmingCongeniality", farmingCongeniality)
        .add("miningCongeniality", miningCongeniality)
        .toString();
  }
}
