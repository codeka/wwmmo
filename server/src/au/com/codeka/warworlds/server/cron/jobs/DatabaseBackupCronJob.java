package au.com.codeka.warworlds.server.cron.jobs;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.Configuration;
import au.com.codeka.warworlds.server.cron.AbstractCronJob;
import au.com.codeka.warworlds.server.cron.CronJob;

/**
 * Backs up the database. Extra parameter is the directory to put the backups in.
 */
@CronJob(name = "Database Backup", desc = "Backs up the database. Extra parameter is the directory to put backups in.")
public class DatabaseBackupCronJob extends AbstractCronJob {
  private static Log log = new Log("DatabaseBackupCronJob");

  @Override
  public String run(String extra) throws Exception {
    File path = new File(extra);
    if (!path.exists() || !path.isDirectory()) {
      return "Specified path (" + extra + ") does not exist.";
    }
    log.info("Backing up into: %s", path.getAbsolutePath());

    long oldestFileToKeep = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(4);

    File[] files = path.listFiles();
    if (files != null) {
      // Check for old files and delete them to save space.
      for (File file : files) {
        long lastModified = file.lastModified();
        if (lastModified > 0 && lastModified < oldestFileToKeep) {
          log.info(
              "File '%s' is too old (%d < %d), deleting.",
              file.getName(), lastModified, oldestFileToKeep);
          if (!file.delete()) {
            log.warning("Error deleting old file.");
          }
        }
      }
    }

    DateTime today = DateTime.now();
    DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd-HH-mm");
    String fileName = String.format("backup-%s.db", fmt.print(today));
    File file = new File(path, fileName);
    log.info("Writing backup to: %s", file.getAbsolutePath());

    Configuration.DatabaseConfiguration dbConfig = Configuration.i.getDatabaseConfig();

    ArrayList<String> cmd = new ArrayList<>();
    cmd.add("pg_dump");
    cmd.add(String.format("--username=%s", dbConfig.getUsername()));
    cmd.add("--host=localhost");
    cmd.add(String.format("--schema=%s", dbConfig.getSchema()));
    cmd.add("--format=custom");
    cmd.add(String.format("--file=%s", file.getAbsolutePath()));
    cmd.add(Configuration.i.getDatabaseConfig().getDatabase());

    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.environment().put("PGPASSWORD", dbConfig.getPassword());
    pb.redirectErrorStream(true);

    log.info("Executing: %s", String.join(" ", cmd));
    long startTime = System.currentTimeMillis();
    Process p = pb.start();

    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
    String line;
    while ((line = reader.readLine()) != null) {
      log.info(" | %s", line);
    }
    p.waitFor();
    long endTime = System.currentTimeMillis();
    log.info("Database dump complete.");

    return String.format(
        Locale.ENGLISH, "Complete in %dms with code %d", endTime - startTime, p.exitValue());
  }
}
