package au.com.codeka.warworlds.server.world.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.PointCloud;
import au.com.codeka.warworlds.common.Vector2;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Sector;
import au.com.codeka.warworlds.common.proto.SectorCoord;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.store.SectorsStore;
import au.com.codeka.warworlds.server.world.SectorManager;

/**
 * The sector generator is used to expand the universe, and generate bunch of sectors when we run
 * out for new players (or when a player creeps up on the edge of the universe).
 */
public class SectorGenerator {
  private static final Log log = new Log("SectorGenerator");

  private Random random;

  private static final double STAR_DENSITY = 0.18;
  private static final double STAR_RANDOMNESS = 0.11;

  /**
   * This is used to choose a star type at a given point in the map. The order is based on the order
   * of Star.CLASSIFICATION:
   *
   * Blue, White, Orange, Red, Neutron, Black Hole
   */
  private static final int[] STAR_TYPE_BONUSES = {30, 40, 50, 40, 30, 0, 0};

  /**
   * Bonuses for generating the number of planets around a star. It's impossible to have 2 or less,
   * 3 is quite unlikely.
   */
  private static final int[] PLANET_COUNT_BONUSES = {-9999, -9999, 0, 10, 20, 10, 5, 0};

  /**
   * Planet type bonuses. The bonuses for each entry need to be added to get the "final" bonus
   */
  private static final int[][] PLANET_TYPE_SLOT_BONUSES = {
      // #GasGiant #Radiated #Inferno #Asteroids #Water #Toxic #Desert #Swamp #Terran
      { -20,        10,       20,     -20,       -20,    0,     10,      0,   -10}, // close to sun
      { -10,         0,       10,     -20,         0,    0,      0,      0,   -10},
      {   0,       -10,      -10,       0,         0,    0,      0,      0,    20},
      {  10,       -10,      -20,       0,        10,    0,    -10,     10,    25},
      {  20,       -20,      -30,     -10,        10,    0,    -20,     10,    30},
      {  20,       -20,      -40,     -10,         0,    0,    -30,      0,     5},
      {  30,       -20,      -40,     -10,         0,    0,    -30,      0,     0}    // far from sun
  };
  private static final int[][] PLANET_TYPE_STAR_BONUSES = {
      // #GasGiant #Radiated #Inferno #Asteroids #Water #Toxic #Desert #Swamp #Terran
      { -10,        0,        0,      -10,        10,   -10,     0,     10,    40},  // blue
      { -10,       -5,      -10,      -10,        20,   -10,     0,     20,    50},  // white
      { -10,       -5,      -20,      -10,        30,   -10,     0,     30,    60},  // yellow
      { -20,      -15,      -30,      -10,        30,    -5,     0,     40,    70},  // orange
      { -20,      -15,      -40,      -10,        20,    -5,     0,     40,    80},  // red
      { -30,       20,       10,      -10,       -10,     0,   -10,    -10,   -30},  // neutron
      { -30,       30,       20,      -10,       -20,     0,   -10,    -10,   -30},  // black hole
  };

  // Planet population is calculated based on the size of the planet (usually, the bigger
  // the planet, the higher the potential population) but also the following bonuses are
  // applied.
  private static final double[] PLANET_POPULATION_BONUSES = {
      // #GasGiant #Radiated #Inferno #Asteroids #Water #Toxic #Desert #Swamp #Terran
          0.4,      0.4,      0.4,     0.0,       1.1,   0.6,   0.9,    0.9,   1.5
  };
  private static final double[] PLANET_FARMING_BONUSES = {
      // #GasGiant #Radiated #Inferno #Asteroids #Water #Toxic #Desert #Swamp #Terran
          0.4,      0.2,      0.2,     0.0,       1.4,   0.4,   0.6,    1.0,   1.2
  };
  private static final double[] PLANET_MINING_BONUSES = {
      // #GasGiant #Radiated #Inferno #Asteroids #Water #Toxic #Desert #Swamp #Terran
          0.8,      1.5,      1.0,     2.5,       0.3,   0.4,   0.6,    0.6,   0.8
  };
  private static final double[] PLANET_ENERGY_BONUSES = {
      // #GasGiant #Radiated #Inferno #Asteroids #Water #Toxic #Desert #Swamp #Terran
          0.8,      2.0,      2.5,     0.1,       0.8,   1.0,   0.3,    0.5,   1.0
  };

  public Sector generate(long x, long y) {
    log.info("Generating sector (%d, %d)...", x, y);
    random = new Random((x * 73649274L) ^ y ^ System.currentTimeMillis());

    SectorCoord coord = new SectorCoord.Builder().x(x).y(y).build();
    ArrayList<Vector2> points = new PointCloud.PoissonGenerator()
        .generate(STAR_DENSITY, STAR_RANDOMNESS, random);

    ArrayList<Star> stars = new ArrayList<>();
    for (Vector2 point : points) {
      stars.add(generateStar(coord, point));
    }

    Sector sector = new Sector.Builder()
        .x(x)
        .y(y)
        .stars(stars)
        .build();
    DataStore.i.sectors().createSector(sector);

    return sector;
  }

  /** Expands the universe by (at least) one sector. */
  public void expandUniverse() {
    expandUniverse(50);
  }

  private void expandUniverse(int numToGenerate) {
    log.debug("Expanding universe by %d", numToGenerate);

    List<SectorCoord> coords =
        DataStore.i.sectors().findSectorsByState(SectorsStore.SectorState.New, numToGenerate);
    for (SectorCoord coord : coords) {
      generate(coord.x, coord.y);
      numToGenerate --;
    }

    if (numToGenerate > 0) {
      DataStore.i.sectors().expandUniverse();
      expandUniverse(numToGenerate);
    }
  }

  private Star generateStar(SectorCoord sectorCoord, Vector2 point) {
    Star.CLASSIFICATION classification = Star.CLASSIFICATION.fromValue(select(STAR_TYPE_BONUSES));
    ArrayList<Planet> planets = generatePlanets(classification);

    return new Star.Builder()
        .id(DataStore.i.seq().nextIdentifier())
        .classification(classification)
        .name(new NameGenerator().generate(random))
        .offset_x((int) ((SectorManager.SECTOR_SIZE - 64) * point.x) + 32)
        .offset_y((int) ((SectorManager.SECTOR_SIZE - 64) * point.y) + 32)
        .planets(planets)
        .sector_x(sectorCoord.x)
        .sector_y(sectorCoord.y)
        .size(random.nextInt(8) + 16)
        .build();
  }

  private ArrayList<Planet> generatePlanets(Star.CLASSIFICATION classification) {
    int numPlanets = 0;
    while (numPlanets < 2) {
      numPlanets = select(PLANET_COUNT_BONUSES);
    }

    ArrayList<Planet> planets = new ArrayList<>();
    for (int planetIndex = 0; planetIndex < numPlanets; planetIndex++) {
      int[] bonuses = new int[PLANET_TYPE_SLOT_BONUSES[0].length];
      for (int i = 0; i < bonuses.length; i++) {
        bonuses[i] = PLANET_TYPE_SLOT_BONUSES[planetIndex][i] +
            PLANET_TYPE_STAR_BONUSES[classification.ordinal()][i];
      }
      int planetType = select(bonuses);

      double populationMultiplier = PLANET_POPULATION_BONUSES[planetType];
      double farmingMultiplier = PLANET_FARMING_BONUSES[planetType];
      double miningMultiplier = PLANET_MINING_BONUSES[planetType];
      double energyMultipler = PLANET_ENERGY_BONUSES[planetType];

      planets.add(new Planet.Builder()
          .index(planetIndex)
          .planet_type(Planet.PLANET_TYPE.fromValue(planetType + 1))
          .population_congeniality((int) (normalRandom(1000) * populationMultiplier))
          .farming_congeniality((int) (normalRandom(100) * farmingMultiplier))
          .mining_congeniality((int) (normalRandom(100) * miningMultiplier))
          .energy_congeniality((int) (normalRandom(100) * energyMultipler))
          .build());
    }

    return planets;
  }

  /**
   * Selects an index from a list of bonuses.
   *
   * <p>For example, if you pass in [0,0,0,0], then all four indices are equally likely and
   * we will return a value in the range [0,4) with equal probability. If you pass in something
   * like [0,0,30] then the third item has a "bonus" of 30 and is hence 2 is a far more likely
   * result than 0 or 1.
   */
  private int select(int[] bonuses) {
    int[] values = new int[bonuses.length];
    int total = 0;

    for (int i = 0; i < bonuses.length; i++) {
      int bonus = bonuses[i];
      int n = bonus + normalRandom(100);
      if (n > 0) {
        total += n;
        values[i] = n;
      } else {
        values[i] = 0;
      }
    }

    int randValue = random.nextInt(total);
    for (int i = 0; i < values.length; i++) {
      randValue -= values[i];
      if (randValue <= 0) {
        return i;
      }
    }

    throw new RuntimeException("Should not get here!");
  }

  /**
   * Generates a random number that has an approximate normal distribution around the midpoint.
   *
   * <p>For example, if maxValue=100 then you'll most get values around 50 and only occasionally 0
   * or 100. Depending on the number of rounds, the tighter the distribution around the midpoint.
   */
  private int normalRandom(int max) {
    final int rounds = 5;

    int n = 0;
    int step = max / rounds;
    for (int i = 0; i < rounds; i++) {
      n += random.nextInt(step - 1);
    }

    return n;
  }
}
