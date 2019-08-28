package au.com.codeka.common.model;

import au.com.codeka.common.protobuf.Messages;


public class BasePlanet {
  protected static PlanetType[] sPlanetTypes = {
      new PlanetType.Builder().setIndex(0)
          .setDisplayName("Gas Giant")
          .setInternalName("gasgiant")
          .build(),
      new PlanetType.Builder().setIndex(1)
          .setDisplayName("Radiated")
          .setInternalName("radiated")
          .build(),
      new PlanetType.Builder().setIndex(2)
          .setDisplayName("Inferno")
          .setInternalName("inferno")
          .build(),
      new PlanetType.Builder().setIndex(3)
          .setDisplayName("Asteroids")
          .setInternalName("asteroids")
          .build(),
      new PlanetType.Builder().setIndex(4)
          .setDisplayName("Water")
          .setInternalName("water")
          .build(),
      new PlanetType.Builder().setIndex(5)
          .setDisplayName("Toxic")
          .setInternalName("toxic")
          .build(),
      new PlanetType.Builder().setIndex(6)
          .setDisplayName("Desert")
          .setInternalName("desert")
          .build(),
      new PlanetType.Builder().setIndex(7)
          .setDisplayName("Swamp")
          .setInternalName("swamp")
          .build(),
      new PlanetType.Builder().setIndex(8)
          .setDisplayName("Terran")
          .setInternalName("terran")
          .build()
  };

  protected BaseStar star;
  protected int index;
  protected PlanetType planetType;
  protected int size;
  protected int populationCongeniality;
  protected int farmingCongeniality;
  protected int miningCongeniality;

  public BaseStar getStar() {
    return star;
  }

  public void setStar(BaseStar star) {
    this.star = star;
  }

  public int getIndex() {
    return index;
  }

  public PlanetType getPlanetType() {
    return planetType;
  }

  public int getSize() {
    return size;
  }

  public int getPopulationCongeniality() {
    return populationCongeniality;
  }

  public int getFarmingCongeniality() {
    return farmingCongeniality;
  }

  public void setFarmingCongeniality(int farmingCongeniality) {
    this.farmingCongeniality = farmingCongeniality;
  }

  public int getMiningCongeniality() {
    return miningCongeniality;
  }

  @Override
  public int hashCode() {
    return star.getKey().hashCode() ^ (index * 632548);
  }

  /**
   * Converts the given Planet protocol buffer into a \c Planet.
   */
  public void fromProtocolBuffer(BaseStar star, Messages.Planet pb) {
    this.star = star;
    index = pb.getIndex();
    planetType = sPlanetTypes[pb.getPlanetType().getNumber() - 1];
    size = pb.getSize();
    if (pb.hasPopulationCongeniality()) {
      populationCongeniality = pb.getPopulationCongeniality();
    }
    if (pb.hasFarmingCongeniality()) {
      farmingCongeniality = pb.getFarmingCongeniality();
    }
    if (pb.hasMiningCongeniality()) {
      miningCongeniality = pb.getMiningCongeniality();
    }
  }

  public void toProtocolBuffer(Messages.Planet.Builder pb) {
    pb.setIndex(index);
    pb.setPlanetType(Messages.Planet.PLANET_TYPE.valueOf(planetType.mIndex + 1));
    pb.setSize(size);
    pb.setPopulationCongeniality(populationCongeniality);
    pb.setFarmingCongeniality(farmingCongeniality);
    pb.setMiningCongeniality(miningCongeniality);
  }

  /**
   * Contains a definition of the planet "type". This should be kept largely in sync with
   * the planet types defined in model/sector.py in the server.
   */
  public static class PlanetType {
    private int mIndex;
    private String mDisplayName;
    private String mInternalName;

    public int getIndex() {
      return mIndex;
    }

    public String getDisplayName() {
      return mDisplayName;
    }

    public String getInternalName() {
      return mInternalName;
    }

    public String getBitmapBasePath() {
      return "planets/" + mInternalName;
    }

    public static class Builder {
      private PlanetType mPlanetType;

      public Builder() {
        mPlanetType = new PlanetType();
      }

      public Builder setIndex(int index) {
        mPlanetType.mIndex = index;
        return this;
      }

      public Builder setDisplayName(String displayName) {
        mPlanetType.mDisplayName = displayName;
        return this;
      }

      public Builder setInternalName(String name) {
        mPlanetType.mInternalName = name;
        return this;
      }

      public PlanetType build() {
        return mPlanetType;
      }
    }
  }
}
