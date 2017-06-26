package au.com.codeka.warworlds.server.cron;

import java.util.HashMap;
import java.util.Map;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.Configuration;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.utils.NameValidator;

/**
 * Goes through the names of things and validates/fixes them. This isn't actually needed to be
 * run on a schedule, but it's useful to run when things change to make sure the names are
 * valid.
 */
public class FixNamesCronJob extends CronJob {
  private static final Log log = new Log("FindAltAccountsCronJob");

  @Override
  public void run(String extra) throws Exception {
    // Go through all of the stars first.
    Map<Integer, String> newNames = new HashMap<>();
    try (SqlStmt stmt = DB.prepare("SELECT id, name FROM stars")) {
      SqlResult res = stmt.select();
      while (res.next()) {
        int id = res.getInt(1);
        String name = res.getString(2);
        String validatedName =
            NameValidator.validateTruncate(name, Configuration.i.getLimits().maxStarNameLength());
        if (!name.equals(validatedName)) {
          log.info("Updating star #%d to \"%s\"", id, validatedName);
          newNames.put(id, validatedName);
        }
      }
    }

    try (SqlStmt stmt = DB.prepare("UPDATE stars SET name = ? WHERE id = ?")) {
      for (Integer id : newNames.keySet()) {
        stmt.setString(1, newNames.get(id));
        stmt.setInt(2, id);
        stmt.update();
      }
    }

    // Next, go through all of the empires.
    newNames.clear();
    try (SqlStmt stmt = DB.prepare("SELECT id, name FROM empires")) {
      SqlResult res = stmt.select();
      while (res.next()) {
        int id = res.getInt(1);
        String name = res.getString(2);
        String validatedName =
            NameValidator.validateTruncate(name, Configuration.i.getLimits().maxEmpireNameLength());
        if (!name.equals(validatedName)) {
          log.info("Updating empire #%d to \"%s\"", id, validatedName);
          newNames.put(id, validatedName);
        }
      }
    }

    try (SqlStmt stmt = DB.prepare("UPDATE empires SET name = ? WHERE id = ?")) {
      for (Integer id : newNames.keySet()) {
        stmt.setString(1, newNames.get(id));
        stmt.setInt(2, id);
        stmt.update();
      }
    }


    // Next, go through all of the alliances.
    newNames.clear();
    try (SqlStmt stmt = DB.prepare("SELECT id, name FROM alliances")) {
      SqlResult res = stmt.select();
      while (res.next()) {
        int id = res.getInt(1);
        String name = res.getString(2);
        String validatedName =
            NameValidator.validateTruncate(name, Configuration.i.getLimits().maxAllianceNameLength());
        if (!name.equals(validatedName)) {
          log.info("Updating alliance #%d to \"%s\"", id, validatedName);
          newNames.put(id, validatedName);
        }
      }
    }

    try (SqlStmt stmt = DB.prepare("UPDATE alliances SET name = ? WHERE id = ?")) {
      for (Integer id : newNames.keySet()) {
        stmt.setString(1, newNames.get(id));
        stmt.setInt(2, id);
        stmt.update();
      }
    }
  }
}
