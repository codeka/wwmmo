package au.com.codeka.warworlds.server;

/**
 * The names of various system properties that be used to configure War Worlds server.
 * <p>
 * You would typically use these like so:
 * <code>
 * String basePath = System.getProperty(SystemProperties.BASE_PATH);
 */
public class SystemProperties {
  /** The "base path" where all the files are installed. This is guaranteed to be set. */
  public static final String BASE_PATH = "au.com.codeka.warworlds.server.basePath";

  /** Port number to listen for HTTP connections on. Default is 8080. */
  public static final String LISTEN_PORT = "au.com.codeka.warworlds.server.listenPort";

  /** If this property is set, {@link StarSimulationThread} will not be started. */
  public static final String DISABLE_STAR_SIMULATION_THREAD
      = "au.com.codeka.warworlds.server.disableStarSimulationThread";
}
