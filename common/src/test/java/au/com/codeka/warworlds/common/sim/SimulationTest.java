package au.com.codeka.warworlds.common.sim;

import com.google.common.collect.Lists;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import au.com.codeka.warworlds.common.proto.BuildRequest;
import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.ColonyFocus;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.EmpireStorage;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;

import static au.com.codeka.warworlds.common.testing.Matchers.closeTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link Simulation}.
 */
@RunWith(JUnit4.class)
public class SimulationTest {
  private final Simulation.LogHandler logHandler = new Simulation.LogHandler() {
    private String starName;

    @Override
    public void setStarName(String starName) {
      this.starName = starName;
    }

    @Override
    public void log(String message) {
      System.out.println(String.format("%s : %s", starName, message));
    }
  };

  /** The time to use as "now", so we're not trying to use the 'real' current time. */
  private static final long NOW_TIME = Simulation.STEP_TIME * 100L;

  /** Tests that a simulation on an idle star does nothing. */
  @Test
  public void testDoNothing() {
    Simulation sim = new Simulation(NOW_TIME, logHandler);

    Star.Builder starBuilder = new Star.Builder()
        .id(1L)
        .name("First");
    sim.simulate(starBuilder);

    assertThat(starBuilder.name, is("First"));
    assertThat(starBuilder.id, is(1L));
    assertThat(starBuilder.last_simulation, is(NOW_TIME));
  }

  /** Tests that we correctly simulate based on the simulation's STEP_TIME. */
  @Test
  public void testSimulateStepTime() {
    Simulation sim = new Simulation(NOW_TIME, false, logHandler);

    Star.Builder starBuilder = new Star.Builder()
        .id(1L)
        .name("Stardust");
    sim.simulate(starBuilder);
    assertThat(starBuilder.last_simulation, is(NOW_TIME));

    sim = new Simulation(NOW_TIME + Simulation.STEP_TIME - 1, false, logHandler);
    sim.simulate(starBuilder);
    assertThat(starBuilder.last_simulation, is(NOW_TIME));

    sim = new Simulation(NOW_TIME + Simulation.STEP_TIME, false, logHandler);
    sim.simulate(starBuilder);
    assertThat(starBuilder.last_simulation, is(NOW_TIME + Simulation.STEP_TIME));
  }

  @Test
  public void testSimulateSimpleColony() {
    Star.Builder starBuilder = new Star.Builder()
        .id(1L)
        .planets(Lists.newArrayList(
            new Planet.Builder()
                .index(0)
                .energy_congeniality(100)
                .farming_congeniality(100)
                .mining_congeniality(100)
                .population_congeniality(1000)
                .planet_type(Planet.PLANET_TYPE.TERRAN)
                .colony(
                    new Colony.Builder()
                        .empire_id(1L)
                        .focus(
                            new ColonyFocus.Builder()
                                .energy(0.25f)
                                .farming(0.25f)
                                .mining(0.25f)
                                .construction(0.25f)
                                .build())
                        .population(100f)
                        .build())
                .build()
        ))
        .empire_stores(Lists.newArrayList(
            new EmpireStorage.Builder()
                .empire_id(1L)
                .max_energy(1000f)
                .max_goods(1000f)
                .max_minerals(1000f)
                .total_energy(100f)
                .total_goods(100f)
                .total_minerals(100f)
                .build()
        ))
        .name("Stardust");

    new Simulation(NOW_TIME, false, logHandler).simulate(starBuilder);
    assertThat(starBuilder.last_simulation, is(NOW_TIME));
    assertThat(starBuilder.planets.get(0).colony.population, closeTo(101.67f, 2));
    assertThat(starBuilder.empire_stores.get(0).empire_id, is(1L));
    // 100 + 4.17 (=100 * focus=0.25 * time_step=0.166) - population_growth=1.67
    assertThat(starBuilder.empire_stores.get(0).total_goods, closeTo(102.50f, 2));
    assertThat(starBuilder.empire_stores.get(0).total_minerals, closeTo(104.17f, 2));
    assertThat(starBuilder.empire_stores.get(0).total_energy, closeTo(104.17f, 2));
  }

  @Test
  public void testSimulateColonyUnevenFocus() {
    Star.Builder starBuilder = new Star.Builder()
        .id(1L)
        .planets(Lists.newArrayList(
            new Planet.Builder()
                .index(0)
                .energy_congeniality(100)
                .farming_congeniality(100)
                .mining_congeniality(100)
                .population_congeniality(1000)
                .planet_type(Planet.PLANET_TYPE.TERRAN)
                .colony(
                    new Colony.Builder()
                        .empire_id(1L)
                        .focus(
                            new ColonyFocus.Builder()
                                .energy(0.1f)
                                .farming(0.2f)
                                .mining(0.3f)
                                .construction(0.4f)
                                .build())
                        .population(100f)
                        .build())
                .build()
        ))
        .empire_stores(Lists.newArrayList(
            new EmpireStorage.Builder()
                .empire_id(1L)
                .max_energy(1000f)
                .max_goods(1000f)
                .max_minerals(1000f)
                .total_energy(100f)
                .total_goods(100f)
                .total_minerals(100f)
                .build()
        ))
        .name("Stardust");

    new Simulation(NOW_TIME, false, logHandler).simulate(starBuilder);
    assertThat(starBuilder.last_simulation, is(NOW_TIME));
    assertThat(starBuilder.planets.get(0).colony.population, closeTo(101.67f, 2));
    assertThat(starBuilder.empire_stores.get(0).empire_id, is(1L));
    // 100 + 4.17 (=100 * focus=0.2 * time_step=0.166) - population_growth=1.67
    assertThat(starBuilder.empire_stores.get(0).total_goods, closeTo(101.67f, 2));
    assertThat(starBuilder.empire_stores.get(0).total_minerals, closeTo(105.00f, 2));
    assertThat(starBuilder.empire_stores.get(0).total_energy, closeTo(101.67f, 2));
  }

  @Test
  public void testSimulateColonyUnevenCongeniality() {
    Star.Builder starBuilder = new Star.Builder()
        .id(1L)
        .planets(Lists.newArrayList(
            new Planet.Builder()
                .index(0)
                .energy_congeniality(100)
                .farming_congeniality(200)
                .mining_congeniality(300)
                .population_congeniality(1000)
                .planet_type(Planet.PLANET_TYPE.TERRAN)
                .colony(
                    new Colony.Builder()
                        .empire_id(1L)
                        .focus(
                            new ColonyFocus.Builder()
                                .energy(0.25f)
                                .farming(0.25f)
                                .mining(0.25f)
                                .construction(0.25f)
                                .build())
                        .population(100f)
                        .build())
                .build()
        ))
        .empire_stores(Lists.newArrayList(
            new EmpireStorage.Builder()
                .empire_id(1L)
                .max_energy(1000f)
                .max_goods(1000f)
                .max_minerals(1000f)
                .total_energy(100f)
                .total_goods(100f)
                .total_minerals(100f)
                .build()
        ))
        .name("Stardust");

    new Simulation(NOW_TIME, false, logHandler).simulate(starBuilder);
    assertThat(starBuilder.last_simulation, is(NOW_TIME));
    assertThat(starBuilder.planets.get(0).colony.population, closeTo(101.67f, 2));
    assertThat(starBuilder.empire_stores.get(0).empire_id, is(1L));
    // 100 + 4.17 (=100 * focus=0.25 * congeniality=2.0 * time_step=0.166) - population_growth=1.67
    assertThat(starBuilder.empire_stores.get(0).total_goods, closeTo(106.67f, 2));
    assertThat(starBuilder.empire_stores.get(0).total_minerals, closeTo(112.50f, 2));
    assertThat(starBuilder.empire_stores.get(0).total_energy, closeTo(104.17f, 2));
  }

  @Test
  public void testBuildColonyShipEnoughEverything() {
    Star.Builder starBuilder = new Star.Builder()
        .id(1L)
        .planets(Lists.newArrayList(
            new Planet.Builder()
                .index(0)
                .energy_congeniality(100)
                .farming_congeniality(200)
                .mining_congeniality(300)
                .population_congeniality(1200)
                .planet_type(Planet.PLANET_TYPE.TERRAN)
                .colony(
                    new Colony.Builder()
                        .empire_id(1L)
                        .focus(
                            new ColonyFocus.Builder()
                                .energy(0.0f)
                                .farming(0.0f)
                                .mining(0.0f)
                                .construction(1.0f)
                                .build())
                        .population(1200f)
                        .build_requests(Lists.newArrayList(
                            new BuildRequest.Builder()
                                .count(1)
                                .design_type(Design.DesignType.COLONY_SHIP)
                                .id(1L)
                                .progress(0.0f)
                                .start_time(NOW_TIME)
                                .build()
                        ))
                        .build())
                .build()
        ))
        .empire_stores(Lists.newArrayList(
            new EmpireStorage.Builder()
                .empire_id(1L)
                .max_energy(1000f)
                .max_goods(1000f)
                .max_minerals(1000f)
                .total_energy(100f)
                .total_goods(100f)
                .total_minerals(120f)
                .build()
        ))
        .name("Stardust");

    new Simulation(NOW_TIME, false, logHandler).simulate(starBuilder);
    assertThat(starBuilder.last_simulation, is(NOW_TIME));
    // We're 120% over quota so we should finish in 1/1.2 of a time step.
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).end_time,
        is(NOW_TIME + (long)((1 / 1.2) * Simulation.STEP_TIME)));

    starBuilder = new Star.Builder()
        .id(1L)
        .planets(Lists.newArrayList(
            new Planet.Builder()
                .index(0)
                .energy_congeniality(100)
                .farming_congeniality(200)
                .mining_congeniality(300)
                .population_congeniality(1000)
                .planet_type(Planet.PLANET_TYPE.TERRAN)
                .colony(
                    new Colony.Builder()
                        .empire_id(1L)
                        .focus(
                            new ColonyFocus.Builder()
                                .energy(0.0f)
                                .farming(0.0f)
                                .mining(0.0f)
                                .construction(1.0f)
                                .build())
                        .population(1000f)
                        .build_requests(Lists.newArrayList(
                            new BuildRequest.Builder()
                                .count(1)
                                .design_type(Design.DesignType.COLONY_SHIP)
                                .id(1L)
                                .progress(0.0f)
                                .start_time(NOW_TIME)
                                .build()
                        ))
                        .build())
                .build()
        ))
        .empire_stores(Lists.newArrayList(
            new EmpireStorage.Builder()
                .empire_id(1L)
                .max_energy(1000f)
                .max_goods(1000f)
                .max_minerals(1000f)
                .total_energy(100f)
                .total_goods(100f)
                .total_minerals(120f)
                .build()
        ))
        .name("Stardust");

    new Simulation(NOW_TIME, false, logHandler).simulate(starBuilder);
    assertThat(starBuilder.last_simulation, is(NOW_TIME));
    // We're 100% over quota on population so we should finish exactly at the end of the step.
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).end_time,
        is(NOW_TIME + Simulation.STEP_TIME));

    starBuilder = new Star.Builder()
        .id(1L)
        .planets(Lists.newArrayList(
            new Planet.Builder()
                .index(0)
                .energy_congeniality(100)
                .farming_congeniality(200)
                .mining_congeniality(300)
                .population_congeniality(1000)
                .planet_type(Planet.PLANET_TYPE.TERRAN)
                .colony(
                    new Colony.Builder()
                        .empire_id(1L)
                        .focus(
                            new ColonyFocus.Builder()
                                .energy(0.0f)
                                .farming(0.0f)
                                .mining(0.0f)
                                .construction(1.0f)
                                .build())
                        .population(1000f)
                        .build_requests(Lists.newArrayList(
                            new BuildRequest.Builder()
                                .count(1)
                                .design_type(Design.DesignType.COLONY_SHIP)
                                .id(1L)
                                .progress(0.0f)
                                .start_time(NOW_TIME)
                                .build()
                        ))
                        .build())
                .build()
        ))
        .empire_stores(Lists.newArrayList(
            new EmpireStorage.Builder()
                .empire_id(1L)
                .max_energy(1000f)
                .max_goods(1000f)
                .max_minerals(1000f)
                .total_energy(100f)
                .total_goods(100f)
                .total_minerals(100f)
                .build()
        ))
        .name("Stardust");

    new Simulation(NOW_TIME, false, logHandler).simulate(starBuilder);
    assertThat(starBuilder.last_simulation, is(NOW_TIME));
    // We're 100% over quota on minerals so we should finish exactly at the end of the step.
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).end_time,
        is(NOW_TIME + Simulation.STEP_TIME));
  }

  @Test
  public void testBuildColonyShipInsufficientPopulation() {
    Star.Builder starBuilder = new Star.Builder()
        .id(1L)
        .planets(Lists.newArrayList(
            new Planet.Builder()
                .index(0)
                .energy_congeniality(100)
                .farming_congeniality(200)
                .mining_congeniality(300)
                .population_congeniality(1000)
                .planet_type(Planet.PLANET_TYPE.TERRAN)
                .colony(
                    new Colony.Builder()
                        .empire_id(1L)
                        .focus(
                            new ColonyFocus.Builder()
                                .energy(0.0f)
                                .farming(0.9f)
                                .mining(0.0f)
                                .construction(0.1f)
                                .build())
                        .population(1000f)
                        .build_requests(Lists.newArrayList(
                            new BuildRequest.Builder()
                                .count(1)
                                .design_type(Design.DesignType.COLONY_SHIP)
                                .id(1L)
                                .progress(0.0f)
                                .start_time(NOW_TIME)
                                .build()
                        ))
                        .build())
                .build()
        ))
        .empire_stores(Lists.newArrayList(
            new EmpireStorage.Builder()
                .empire_id(1L)
                .max_energy(1000f)
                .max_goods(1000f)
                .max_minerals(1000f)
                .total_energy(100f)
                .total_goods(100f)
                .total_minerals(120f)
                .build()
        ))
        .name("Stardust");

    new Simulation(NOW_TIME, false, logHandler).simulate(starBuilder);
    assertThat(starBuilder.last_simulation, is(NOW_TIME));
    // We only have 10% of the required population, so it should take 10 steps to complete.
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).end_time,
        is(NOW_TIME + (10 * Simulation.STEP_TIME)));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).minerals_efficiency,
        closeTo(0.92f, 2));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).population_efficiency,
        closeTo(0.08f, 2));

    // And it should continue taking 10 steps as we continue simulating.
    new Simulation(NOW_TIME + Simulation.STEP_TIME, false, logHandler).simulate(starBuilder);
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).end_time,
        is(NOW_TIME + (10 * Simulation.STEP_TIME)));

    // Note as we progress, the efficiency of the population improves as we need less and less
    // population relative to when we started, and we use up the remaining minerals.
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).minerals_efficiency,
        closeTo(0.92f, 2));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).population_efficiency,
        closeTo(0.08f, 2));

    new Simulation(NOW_TIME + (2 * Simulation.STEP_TIME), false, logHandler).simulate(starBuilder);
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).end_time,
        is(NOW_TIME + (10 * Simulation.STEP_TIME)));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).minerals_efficiency,
        closeTo(0.91f, 2));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).population_efficiency,
        closeTo(0.09f, 2));

    new Simulation(NOW_TIME + (3 * Simulation.STEP_TIME), false, logHandler).simulate(starBuilder);
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).end_time,
        is(NOW_TIME + (10 * Simulation.STEP_TIME)));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).minerals_efficiency,
        closeTo(0.90f, 2));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).population_efficiency,
        closeTo(0.10f, 2));

    new Simulation(NOW_TIME + (4 * Simulation.STEP_TIME), false, logHandler).simulate(starBuilder);
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).end_time,
        is(NOW_TIME + (10 * Simulation.STEP_TIME)));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).minerals_efficiency,
        closeTo(0.89f, 2));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).population_efficiency,
        closeTo(0.11f, 2));

    new Simulation(NOW_TIME + (5 * Simulation.STEP_TIME), false, logHandler).simulate(starBuilder);
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).end_time,
        is(NOW_TIME + (10 * Simulation.STEP_TIME)));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).minerals_efficiency,
        closeTo(0.88f, 2));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).population_efficiency,
        closeTo(0.12f, 2));

    new Simulation(NOW_TIME + (6 * Simulation.STEP_TIME), false, logHandler).simulate(starBuilder);
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).end_time,
        is(NOW_TIME + (10 * Simulation.STEP_TIME)));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).minerals_efficiency,
        closeTo(0.87f, 2));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).population_efficiency,
        closeTo(0.13f, 2));

    new Simulation(NOW_TIME + (7 * Simulation.STEP_TIME), false, logHandler).simulate(starBuilder);
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).end_time,
        is(NOW_TIME + (10 * Simulation.STEP_TIME)));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).minerals_efficiency,
        closeTo(0.86f, 2));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).population_efficiency,
        closeTo(0.14f, 2));

    new Simulation(NOW_TIME + (8 * Simulation.STEP_TIME), false, logHandler).simulate(starBuilder);
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).end_time,
        is(NOW_TIME + (10 * Simulation.STEP_TIME)));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).minerals_efficiency,
        closeTo(0.84f, 2));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).population_efficiency,
        closeTo(0.16f, 2));

    new Simulation(NOW_TIME + (9 * Simulation.STEP_TIME), false, logHandler).simulate(starBuilder);
    // Now we start getting some rounding errors
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).end_time / 100000.f,
        closeTo((NOW_TIME + (10 * Simulation.STEP_TIME)) / 100000.f, 2));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).minerals_efficiency,
        closeTo(0.84f, 2));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).population_efficiency,
        closeTo(0.16f, 2));

    new Simulation(NOW_TIME + (10 * Simulation.STEP_TIME), false, logHandler).simulate(starBuilder);
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).end_time / 100000.f,
        closeTo((NOW_TIME + (10 * Simulation.STEP_TIME)) / 100000.f, 2));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).progress, closeTo(1.0f, 2));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).minerals_efficiency,
        closeTo(0.84f, 2));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).population_efficiency,
        closeTo(0.16f, 2));
  }

  @Test
  public void testBuildColonyShipInsufficientMinerals() {
    Star.Builder starBuilder = new Star.Builder()
        .id(1L)
        .planets(Lists.newArrayList(
            new Planet.Builder()
                .index(0)
                .energy_congeniality(100)
                .farming_congeniality(200)
                .mining_congeniality(300)
                .population_congeniality(1000)
                .planet_type(Planet.PLANET_TYPE.TERRAN)
                .colony(
                    new Colony.Builder()
                        .empire_id(1L)
                        .focus(
                            new ColonyFocus.Builder()
                                .energy(0.0f)
                                .farming(0.0f)
                                .mining(0.01f)
                                .construction(0.99f)
                                .build())
                        .population(1000f)
                        .build_requests(Lists.newArrayList(
                            new BuildRequest.Builder()
                                .count(1)
                                .design_type(Design.DesignType.COLONY_SHIP)
                                .id(1L)
                                .progress(0.0f)
                                .start_time(NOW_TIME)
                                .build()
                        ))
                        .build())
                .build()
        ))
        .empire_stores(Lists.newArrayList(
            new EmpireStorage.Builder()
                .empire_id(1L)
                .max_energy(1000f)
                .max_goods(1000f)
                .max_minerals(1000f)
                .total_energy(100f)
                .total_goods(100f)
                .total_minerals(0f)
                .build()
        ))
        .name("Stardust");

    new Simulation(NOW_TIME, false, logHandler).simulate(starBuilder);
    assertThat(starBuilder.last_simulation, is(NOW_TIME));
    // We only have 10 minerals here, so it should take 10 steps to complete.
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).end_time,
        is(NOW_TIME + Math.round(10 * Simulation.STEP_TIME)));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).minerals_efficiency,
        closeTo(0.09f, 2));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).population_efficiency,
        closeTo(0.91f, 2));

    new Simulation(NOW_TIME + Simulation.STEP_TIME, false, logHandler).simulate(starBuilder);
    // Now we only have 5 minerals here, so it should take 19 steps to complete.
    assertThat(starBuilder.last_simulation, is(NOW_TIME + Simulation.STEP_TIME));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).end_time,
        is(NOW_TIME + (19 * Simulation.STEP_TIME)));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).minerals_efficiency,
        closeTo(0.05f, 2));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).population_efficiency,
        closeTo(0.95f, 2));

    new Simulation(NOW_TIME + (10 * Simulation.STEP_TIME), false, logHandler).simulate(starBuilder);
    assertThat(starBuilder.last_simulation, is(NOW_TIME + (10 * Simulation.STEP_TIME)));
    // We've used up our store of minerals, now it'll take much longer because we're only making 5
    // minerals per turn. (note, we round it because there's going to be some rounding errors)
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).end_time / 100000.0f,
        closeTo((NOW_TIME + (19 * Simulation.STEP_TIME)) / 100000.0f, 3));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).minerals_efficiency,
        closeTo(0.05f, 2));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).population_efficiency,
        closeTo(0.95f, 2));

    long now = NOW_TIME + (19 * Simulation.STEP_TIME);
    new Simulation(now, false, logHandler).simulate(starBuilder);
    assertThat(starBuilder.last_simulation, is(now));
    // Should be finished now. (note, we round it because there's going to be some rounding errors)
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).end_time / 100000.0f,
        closeTo((NOW_TIME + (19 * Simulation.STEP_TIME)) / 100000.0f, 3));
    assertThat(
        BuildHelper.getBuildProgress(starBuilder.planets.get(0).colony.build_requests.get(0), now),
        closeTo(1.0f, 2));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).minerals_efficiency,
        closeTo(0.05f, 2));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).population_efficiency,
        closeTo(0.95f, 2));
  }

  @Test
  public void testBuildScoutPartialStep() {
    Star.Builder starBuilder = new Star.Builder()
        .id(1L)
        .planets(Lists.newArrayList(
            new Planet.Builder()
                .index(0)
                .energy_congeniality(100)
                .farming_congeniality(200)
                .mining_congeniality(300)
                .population_congeniality(1000)
                .planet_type(Planet.PLANET_TYPE.TERRAN)
                .colony(
                    new Colony.Builder()
                        .empire_id(1L)
                        .focus(
                            new ColonyFocus.Builder()
                                .energy(0.0f)
                                .farming(0.0f)
                                .mining(0.0f)
                                .construction(1.0f)
                                .build())
                        .population(1000f)
                        .build_requests(Lists.newArrayList(
                            new BuildRequest.Builder()
                                .count(1)
                                .design_type(Design.DesignType.SCOUT)
                                .id(1L)
                                .progress(0.0f)
                                .start_time(NOW_TIME + (Simulation.STEP_TIME/4))
                                .build()
                        ))
                        .build())
                .build()
        ))
        .empire_stores(Lists.newArrayList(
            new EmpireStorage.Builder()
                .empire_id(1L)
                .max_energy(1000f)
                .max_goods(1000f)
                .max_minerals(1000f)
                .total_energy(100f)
                .total_goods(100f)
                .total_minerals(100f)
                .build()
        ))
        .name("Stardust");

    new Simulation(NOW_TIME, false, logHandler).simulate(starBuilder);
    assertThat(starBuilder.last_simulation, is(NOW_TIME));
    // We started 1/4 of the way through the step, and we should finish after 1/200th of a step
    // (because we have enough minerals and 200x more population than needed)
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).end_time,
        is(NOW_TIME + (Simulation.STEP_TIME / 4) + (Simulation.STEP_TIME / 200)));
    // But we're not ACTUALLY finished yet, because actually we've only just started...
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).progress,
        closeTo(0.0f, 2));

    long now = NOW_TIME + (Simulation.STEP_TIME / 4L) + (Simulation.STEP_TIME / 400L);
    new Simulation(now, false, logHandler).simulate(starBuilder);
    // Now we're half-way through
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).end_time,
        is(NOW_TIME + (Simulation.STEP_TIME / 4) + (Simulation.STEP_TIME / 200)));
    assertThat(
        BuildHelper.getBuildProgress(starBuilder.planets.get(0).colony.build_requests.get(0), now),
        closeTo(0.5f, 2));

    now = NOW_TIME + (Simulation.STEP_TIME / 4) + (Simulation.STEP_TIME / 200);
    new Simulation(now, false, logHandler).simulate(starBuilder);
    // NOW we should be finished.
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).end_time,
        is(NOW_TIME + (Simulation.STEP_TIME / 4) + (Simulation.STEP_TIME / 200)));
    assertThat(
        BuildHelper.getBuildProgress(starBuilder.planets.get(0).colony.build_requests.get(0), now),
        closeTo(1.0f, 2));
  }

  /** If you have more minerals than you need, you can't build faster. Unlike population. */
  @Test
  public void testExtraMineralsDoesntAffectBuildTime() {
    Star.Builder starBuilder = new Star.Builder()
        .id(1L)
        .planets(Lists.newArrayList(
            new Planet.Builder()
                .index(0)
                .energy_congeniality(100)
                .farming_congeniality(200)
                .mining_congeniality(300)
                .population_congeniality(2000)
                .planet_type(Planet.PLANET_TYPE.TERRAN)
                .colony(
                    new Colony.Builder()
                        .empire_id(1L)
                        .focus(
                            new ColonyFocus.Builder()
                                .energy(0.0f)
                                .farming(0.0f)
                                .mining(0.0f)
                                .construction(1.0f)
                                .build())
                        .population(2000f)
                        .build_requests(Lists.newArrayList(
                            new BuildRequest.Builder()
                                .count(1)
                                .design_type(Design.DesignType.COLONY_SHIP)
                                .id(1L)
                                .progress(0.0f)
                                .start_time(NOW_TIME)
                                .build()
                        ))
                        .build())
                .build()
        ))
        .empire_stores(Lists.newArrayList(
            new EmpireStorage.Builder()
                .empire_id(1L)
                .max_energy(1000f)
                .max_goods(1000f)
                .max_minerals(2000f)
                .total_energy(100f)
                .total_goods(100f)
                .total_minerals(100f)
                .build()
        ))
        .name("Stardust");

    long now = NOW_TIME;
    new Simulation(now, false, logHandler).simulate(starBuilder);
    assertThat(starBuilder.last_simulation, is(now));
    // Because we haven't started building yet, minerals should not be being used.
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).end_time,
        is(NOW_TIME + (Simulation.STEP_TIME / 2)));

    // Doubling the mineral count won't affect the end time.
    starBuilder.empire_stores.set(
        0, starBuilder.empire_stores.get(0).newBuilder().total_minerals(200f).build());
    new Simulation(now, false, logHandler).simulate(starBuilder);
    assertThat(starBuilder.last_simulation, is(now));
    assertThat(
        starBuilder.planets.get(0).colony.build_requests.get(0).end_time,
        is(NOW_TIME + (Simulation.STEP_TIME / 2)));
  }

  @Test
  public void testMineralUsage() {
    Star.Builder starBuilder = new Star.Builder()
        .id(1L)
        .planets(Lists.newArrayList(
            new Planet.Builder()
                .index(0)
                .energy_congeniality(100)
                .farming_congeniality(200)
                .mining_congeniality(300)
                .population_congeniality(1000)
                .planet_type(Planet.PLANET_TYPE.TERRAN)
                .colony(
                    new Colony.Builder()
                        .empire_id(1L)
                        .focus(
                            new ColonyFocus.Builder()
                                .energy(0.0f)
                                .farming(0.8f)
                                .mining(0.1f)
                                .construction(0.1f)
                                .build())
                        .population(1000f)
                        .build_requests(Lists.newArrayList(
                            new BuildRequest.Builder()
                                .count(1)
                                .design_type(Design.DesignType.SCOUT)
                                .id(1L)
                                .progress(0.0f)
                                .start_time(NOW_TIME + (Simulation.STEP_TIME/4))
                                .build()
                        ))
                        .build())
                .build()
        ))
        .empire_stores(Lists.newArrayList(
            new EmpireStorage.Builder()
                .empire_id(1L)
                .max_energy(1000f)
                .max_goods(1000f)
                .max_minerals(1000f)
                .total_energy(100f)
                .total_goods(100f)
                .total_minerals(100f)
                .build()
        ))
        .name("Stardust");

    long now = NOW_TIME;
    new Simulation(now, false, logHandler).simulate(starBuilder);
    assertThat(starBuilder.last_simulation, is(now));
    // Because we haven't started building yet, minerals should not be being used.
    assertThat(starBuilder.empire_stores.get(0).total_minerals, closeTo(150f, 2));
    assertThat(
        StarHelper.getDeltaMineralsPerHour(starBuilder.build(), 1L, now),
        closeTo(300.0f, 2));
    assertThat(
        ColonyHelper.getDeltaMineralsPerHour(starBuilder.planets.get(0).colony, now),
        closeTo(300.0f, 2));

    now = NOW_TIME + (Simulation.STEP_TIME / 4L) + (Simulation.STEP_TIME / 40L);
    new Simulation(now, false, logHandler).simulate(starBuilder);
    assertThat(starBuilder.last_simulation, is(NOW_TIME));
    // We're half-way through, so we should be using those minerals.
    assertThat(starBuilder.empire_stores.get(0).total_minerals, closeTo(150f, 2));
    assertThat(
        StarHelper.getDeltaMineralsPerHour(starBuilder.build(), 1L, now),
        closeTo(293.33f, 2));
    assertThat(
        ColonyHelper.getDeltaMineralsPerHour(starBuilder.planets.get(0).colony, now),
        closeTo(293.33f, 2));

    now = NOW_TIME + (Simulation.STEP_TIME / 4) + (Simulation.STEP_TIME / 20);
    new Simulation(now, false, logHandler).simulate(starBuilder);
    assertThat(starBuilder.last_simulation, is(NOW_TIME));
    // Now we're done, we're no longer using the minerals.
    assertThat(starBuilder.empire_stores.get(0).total_minerals, closeTo(150f, 2));
    assertThat(
        StarHelper.getDeltaMineralsPerHour(starBuilder.build(), 1L, now),
        closeTo(300.0f, 2));
    assertThat(
        ColonyHelper.getDeltaMineralsPerHour(starBuilder.planets.get(0).colony, now),
        closeTo(300.0f, 2));
  }
}
