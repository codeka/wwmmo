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

  public static final String PROD_CLIENT_ID =
      "1021675369049-85dehn126ib087kkc270k0lko6ahv2h7.apps.googleusercontent.com";
  public static final String DEV_CLIENT_ID =
      "1021675369049-kh3j8g9m8ugkrqamllddh3v0coss7gc8.apps.googleusercontent.com";

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
  @Expose private String configDirectory;
  @Expose private int listenPort;
  @Expose private Integer numStarSimulationThreads;
  @Expose private DatabaseConfiguration database;
  @Expose private LimitsConfiguration limits;
  @Expose private ResetsConfig resets;
  @Expose private PatreonConfig patreon;
  @Expose private String requestStatsDirectory;
  @Expose private boolean allowNonProdClientLogins;
  @Expose private ClickerConfig[] clickers;
  @Expose private SafetyNetConfig safetyNet;

  public Configuration() {
    limits = new LimitsConfiguration();
  }

  public String getRealmName() {
    return realmName;
  }

  public File getDataDirectory() {
    return new File(dataDirectory);
  }

  public File getConfigDirectory() { return new File(configDirectory); }

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

  public ResetsConfig getResets() {
    return resets;
  }

  public LimitsConfiguration getLimits() {
    return limits;
  }

  public PatreonConfig getPatreon() {
    return patreon;
  }

  public String getRequestStatsDirectory() {
    return requestStatsDirectory;
  }

  /** If true, we'll allow non-prod clients to log in. Default is false. */
  public boolean getAllowNonProdClientLogins() {
    return allowNonProdClientLogins;
  }

  public ClickerConfig[] getClickers() {
    return clickers;
  }

  public SafetyNetConfig getSafetyNet() {
    return safetyNet;
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

  public static class ResetsConfig {
    @Expose private int resetPeriodDays;
    @Expose private int maxResetsPerPeriod;

    public int getResetPeriodDays() {
      return resetPeriodDays;
    }

    public int getMaxResetsPerPeriod() {
      return maxResetsPerPeriod;
    }
  }

  public static class PatreonConfig {
    @Expose private String clientId;
    @Expose private String clientSecret;
    @Expose private String redirectUri;
    @Expose private String refreshToken;

    public String getClientId() {
      return clientId;
    }

    public String getClientSecret() {
      return clientSecret;
    }

    public String getRedirectUri() {
      return redirectUri;
    }

    public String getRefreshToken() {
      return refreshToken;
    }
  }

  public static class ClickerConfig {
    @Expose private String name;
    @Expose private String appName;

    public String getName() {
      return name;
    }

    public String getAppName() {
      return appName;
    }
  }

  public static class SafetyNetConfig {
    @Expose boolean enabled;
    @Expose int[] exemptedEmpires;

    public boolean isEnabled() {
      return enabled;
    }

    public int[] getExemptedEmpires() {
      return exemptedEmpires;
    }
  }
}
