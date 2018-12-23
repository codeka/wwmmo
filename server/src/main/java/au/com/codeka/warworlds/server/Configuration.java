package au.com.codeka.warworlds.server;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonElement;
import com.google.gson.annotations.Expose;
import com.google.gson.stream.JsonReader;

import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Log;

/**
 * The server's configuration parameters are read from a json object on startup and parsed into
 * this object via Gson.
 */
public class Configuration {
  public static final Configuration i = new Configuration();
  private static final Log log = new Log("Configuration");

  private static final Collection<String> FIREBASE_SCOPES =
      Lists.newArrayList("https://www.googleapis.com/auth/firebase.messaging");

  @Expose private String baseUrl;
  @Expose private int listenPort;
  @Expose private SmtpConfig smtp;
  @Expose private JsonElement firebase;
  @Expose private PatreonConfig patreon;
  @Expose private LimitsConfig limits;

  @Nullable
  private GoogleCredentials firebaseCredentials;

  private Configuration() {
    smtp = new SmtpConfig();
  }

  /** Loads the {@link Configuration} from the given file and sets it to {@code Configuration.i}. */
  public void load() throws IOException {
    String fileName = System.getProperty("ConfigFile");
    if (fileName == null) {
      // just try whatever in the current directory
      fileName = "config.json";
    }

    log.info("Loading config from: %s", fileName);
    Gson gson = new GsonBuilder()
        .registerTypeAdapter(Configuration.class, (InstanceCreator<Configuration>) type -> i)
        .create();
    JsonReader jsonReader = new JsonReader(new FileReader(fileName));
    jsonReader.setLenient(true); // allow comments (and a few other things)
    gson.fromJson(jsonReader, Configuration.class);
  }

  public int getListenPort() {
    return listenPort;
  }

  public SmtpConfig getSmtp() {
    return smtp;
  }

  public PatreonConfig getPatreon() {
    return patreon;
  }

  public LimitsConfig getLimits() {
    return limits;
  }

  public GoogleCredentials getFirebaseCredentials() {
    try {
      if (firebaseCredentials == null) {
        try {
          firebaseCredentials = GoogleCredentials.fromStream(
              new ByteArrayInputStream(firebase.toString().getBytes("utf-8")))
              .createScoped(FIREBASE_SCOPES);
        } catch (UnsupportedEncodingException e) {
          // Should never happen.
        }
      }

      firebaseCredentials.refreshIfExpired();
    } catch (IOException e) {
      throw new RuntimeException("Should never happen.", e);
    }
    return firebaseCredentials;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public static class SmtpConfig {
    @Expose private String host;
    @Expose private int port;
    @Expose private String username;
    @Expose private String password;
    @Expose private String senderAddr;

    public SmtpConfig() {
      host = "smtp-relay.gmail.com";
      port = 587;
      senderAddr = "noreply@war-worlds.com";
    }

    public String getHost() {
      return host;
    }

    public int getPort() {
      return port;
    }

    public String getUserName() {
      return username;
    }

    public String getPassword() {
      return password;
    }

    public String getSenderAddr() {
      return senderAddr;
    }
  }

  public static class PatreonConfig {
    @Expose private String clientId;
    @Expose private String clientSecret;
    @Expose private String redirectUri;

    public String getClientId() {
      return clientId;
    }

    public String getClientSecret() {
      return clientSecret;
    }

    public String getRedirectUri() {
      return redirectUri;
    }
  }

  public static class LimitsConfig {
    @Expose private int maxEmpireNameLength;

    public int getMaxEmpireNameLength() {
      return maxEmpireNameLength;
    }
  }
}
