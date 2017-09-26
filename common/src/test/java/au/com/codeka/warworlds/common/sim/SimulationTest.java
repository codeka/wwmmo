package au.com.codeka.warworlds.common.sim;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import au.com.codeka.warworlds.common.proto.Star;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

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
}
