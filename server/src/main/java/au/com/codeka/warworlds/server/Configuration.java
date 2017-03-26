package au.com.codeka.warworlds.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.annotations.Expose;
import com.google.gson.stream.JsonReader;

import java.io.FileReader;
import java.io.IOException;

import au.com.codeka.warworlds.common.Log;

/**
 * The server's configuration parameters are read from a json object on startup and parsed into
 * this object via Gson.
 */
public class Configuration {
  public static final Configuration i = new Configuration();
  private static final Log log = new Log("Configuration");

  @Expose
  private int listenPort;

  private Configuration() {
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
}
