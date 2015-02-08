package au.com.codeka.warworlds.intel.generator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Scanner;

/**
 * This program reads the .csv file returned by:
 *
 * https://game.war-worlds.com/realms/beta/stars?export=csv
 *
 * And generates Mercator-style tiles suitable for use with a mapping library like Leaflet
 * (http://leafletjs.com/) to visualize the entire universe.
 */
public class Program {
  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Usage: java -jar generator.jar \"path-to-stars.csv\"");
      System.exit(1);
    }
    String fileName = args[0];

    // We do an initial pass through the file to get the world's bounding box.
    Scanner scanner = new Scanner(fileName);
    scanner.
    try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
      String line;
      while ((line = br.readLine()) != null) {

      }
    } catch (Exception e) {
      System.out.println("Error reading file, aborting.");
      System.exit(1);
    }
  }
}
