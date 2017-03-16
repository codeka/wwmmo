package au.com.codeka.warworlds.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
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

  @Expose private String realmName;
  @Expose private String dataDirectory;
  @Expose private int listenPort;
  @Expose private Integer numStarSimulationThreads;
  @Expose private DatabaseConfiguration database;
  @Expose private SinbinConfiguration sinbin;
  @Expose private LimitsConfiguration limits;

  public Configuration() {
    limits = new LimitsConfiguration();
  }

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

  public LimitsConfiguration getLimits() {
    return limits;
  }

  public static class DatabaseConfiguration {
    @Expose private String server;
    @Expose private int port;
    @Expose private String database;
    @Expose private String schema;
    @Expose private String username;
    @Expose private String password;

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
    @Expose private boolean enabled;
    @Expose private int uniqueEmpireVotes;
    @Expose private int voteTimeSeconds;
    @Expose private int maxVotesPerDay;

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

  public static class LimitsConfiguration {
    @Expose private int maxEmpireNameLength;
    @Expose private int maxAllianceNameLength;
    @Expose private int maxStarNameLength;
    @Expose private double maxEmojiRatio;

    public LimitsConfiguration() {
      maxEmpireNameLength = 40;
      maxAllianceNameLength = 80;
      maxStarNameLength = 40;
      maxEmojiRatio = 0.2;
    }

    public int maxEmpireNameLength() {
      return maxEmpireNameLength;
    }

    public int maxAllianceNameLength() {
      return maxAllianceNameLength;
    }

    public int maxStarNameLength() {
      return maxStarNameLength;
    }

    public double getMaxEmojiRatio() {
      return maxEmojiRatio;
    }
  }
}
