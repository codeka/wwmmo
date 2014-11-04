package au.com.codeka.warworlds.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

/**
 * The server's configuration parameters are read from a json object on startup and parsed into
 * this object via Gson.
 */
public class Configuration {
  public static Configuration i;

  /** Loads the {@link Configuration} from the given file and sets it to {@code Configuration.i}. */
  public static void loadConfig() throws FileNotFoundException {
    String fileName = System.getProperty("au.com.codeka.warworlds.server.ConfigFile");
    if (fileName == null) {
      // just try whatever in the current directory
      fileName = "config.json";
    }

    Gson gson = new GsonBuilder().create();
    JsonReader jsonReader = new JsonReader(new FileReader(fileName));
    jsonReader.setLenient(true); // allow comments (and a few other things)
    i = gson.fromJson(jsonReader, Configuration.class);
  }

  private String realmName;
  private String dataDirectory;
  private int listenPort;
  private Integer numStarSimulationThreads;
  private DatabaseConfiguration database;
  private SinbinConfiguration sinbin;

  public String getRealmName() {
    return realmName;
  }

  public File getDataDirectory() {
    return new File(dataDirectory);
  }

  public int getListenPort() {
    return listenPort;
  }

  public int getNumStarSimulationThreads() {
    if (numStarSimulationThreads == null) {
      return 1;
    }
    return numStarSimulationThreads.intValue();
  }

  public DatabaseConfiguration getDatabaseConfig() {
    return database;
  }

  public SinbinConfiguration getSinbinConfig() {
    return sinbin;
  }

  public static class DatabaseConfiguration {
    private String server;
    private int port;
    private String database;
    private String schema;
    private String username;
    private String password;

    public String getServer() {
      return server;
    }

    public int getPort() {
      return port;
    }

    public String getDatabase() {
      return database;
    }

    public String getSchema() {
      return schema;
    }

    public String getUsername() {
      return username;
    }

    public String getPassword() {
      return password;
    }
  }

  public static class SinbinConfiguration {
    private boolean enabled;
    private int uniqueEmpireVotes;
    private int voteTimeSeconds;
    private int maxVotesPerDay;

    public boolean isEnabled() {
      return enabled;
    }

    public int getUniqueEmpireVotes() {
      return uniqueEmpireVotes;
    }

    public int getVoteTimeSeconds() {
      return voteTimeSeconds;
    }

    public int getMaxVotesPerDay() {
      return maxVotesPerDay;
    }
  }
}
