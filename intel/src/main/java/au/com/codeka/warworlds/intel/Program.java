package au.com.codeka.warworlds.intel;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.jetty.server.Server;

import java.io.FileReader;
import java.util.PriorityQueue;

import au.com.codeka.common.Log;

/**
 * This program reads the .csv file returned by:
 *
 * https://game.war-worlds.com/realms/beta/stars?export=csv
 *
 * And generates Mercator-style tiles suitable for use with a mapping library like Leaflet
 * (http://leafletjs.com/) to visualize the entire universe.
 */
public class Program {
  private static final Log log = new Log("Program");

  public static void main(String[] args) {
    try {
      Configuration.loadConfig();
      LogImpl.setup();
      Universe.setup();

      log.info("Starting server.");
      int port = Configuration.i.getListenPort();
      Server server = new Server(port);
      server.setHandler(new RequestRouter());
      server.start();
      log.info("Server started on http://localhost:%d/", port);
      server.join();
    } catch (Exception e) {
      log.error("Top-level error caught!", e);
    }
  }
}
