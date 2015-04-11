package au.com.codeka.warworlds.intel;

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
    String fileName = System.getProperty("au.com.codeka.warworlds.intel.ConfigFile");
    if (fileName == null) {
      // just try whatever in the current directory
      fileName = "config.json";
    }

    Gson gson = new GsonBuilder().create();
    JsonReader jsonReader = new JsonReader(new FileReader(fileName));
    jsonReader.setLenient(true); // allow comments (and a few other things)
    i = gson.fromJson(jsonReader, Configuration.class);
  }

  private String dataDirectory;
  private int listenPort;
  private String csvPath;

  public int getListenPort() {
    return listenPort;
  }

  public String getCsvPath() {
    return csvPath;
  }

  public String getDataDirectory() {
    return dataDirectory;
  }
}
