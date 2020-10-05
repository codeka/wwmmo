package au.com.codeka.warworlds.server.cron.jobs;

import com.google.common.base.Strings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.warworlds.server.Configuration;
import au.com.codeka.warworlds.server.StarSimulatorThreadManager;
import au.com.codeka.warworlds.server.cron.AbstractCronJob;
import au.com.codeka.warworlds.server.cron.CronJob;
import au.com.codeka.warworlds.server.ctrl.BaseDataBase;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Empire;

/**
 * Exports all the stars in the database to a .csv file.
 */
@CronJob(name = "Export Stars", desc = "Exports all stars in the database to a .csv file.")
public class ExportStarsCronJob extends AbstractCronJob {
  private static final Log log = new Log("ExportStarsCronJob");

  private HashMap<Integer, Empire> empires = new HashMap<>();
  private HashMap<Integer, Integer> obfuscatedIds = new HashMap<>();

  @Override
  public String run(String extra) throws Exception {
    String fileName = extra;
    if (Strings.isNullOrEmpty(fileName)) {
      fileName = new File(Configuration.i.getDataDirectory(), "export.csv").getAbsolutePath();
    }
    populateEmpires();
    log.info("Exporting stars to: %s", fileName);
    OutputStream outs =  new FileOutputStream(fileName);
    try {
      // Pause simulating stars while we do this...
      StarSimulatorThreadManager.i.pause();

      long n = export(outs);
      return String.format(Locale.ENGLISH, "Exported %d stars to: %s", n, fileName);
    } finally {
      StarSimulatorThreadManager.i.resume();

      outs.close();
    }
  }

  private void populateEmpires() throws Exception {
    SecureRandom rand = new SecureRandom();

    String sql = "SELECT id, obfuscated_id FROM empires";
    try (SqlStmt stmt = DB.prepare(sql)) {
      SqlResult res = stmt.select();
      while (res.next()) {
        int empireId = res.getInt(1);
        Integer obfuscatedId = res.getInt(2);
        if (obfuscatedId == null) {
          obfuscatedId = Math.abs(rand.nextInt());
          saveObfuscaedId(empireId, obfuscatedId);
        }

        obfuscatedIds.put(empireId, obfuscatedId);
      }
    }

    for (Empire empire : new EmpireController().getAllEmpires()) {
      empires.put(empire.getID(), empire);
    }
  }

  private void saveObfuscaedId(int empireId, int obfuscatedId) throws Exception {
    try (SqlStmt stmt = DB.prepare("UPDATE empires SET obfuscated_id = ? WHERE id = ?")) {
      stmt.setInt(1, obfuscatedId);
      stmt.setInt(2, empireId);
      stmt.update();
    }
  }

  private long export(OutputStream outs) throws Exception {
    long numStars = 0;
    PrintStream ps = new PrintStream(outs);
    String sql = "" +
        "SELECT" +
        "  sectors.x AS sector_x,sectors.y AS sector_y,stars.x,stars.y,name,size,star_type," +
        " (" +
        "    SELECT" +
        "      empires.id" +
        "    FROM" +
        "      colonies INNER JOIN empires ON empires.id = colonies.empire_id" +
        "    WHERE colonies.star_id = stars.id" +
        "    GROUP BY empires.id" +
        "    ORDER BY COUNT(*) DESC LIMIT 1) AS empire_id " +
        "FROM stars " +
        "INNER JOIN sectors ON sectors.id = stars.sector_id";
    try (SqlStmt stmt = DB.prepare(sql)) {
      SqlResult res = stmt.select();

      ps.println("x,y,name,size,type,empire_id,empire_name");
      while (res.next()) {
        Empire empire = null;
        int obfuscatedId = 0;
        if (res.getInt(8) != null) {
          empire = empires.get(res.getInt(8));
          obfuscatedId = obfuscatedIds.get(empire.getID());
        }

        ps.println(String.format(Locale.ENGLISH, "%d.%d,%d.%d,%s,%s,%s,%s,%s",
            res.getInt(1), res.getInt(3), res.getInt(2), res.getInt(4),
            escapeValue(res.getString(5)), res.getInt(6),
            getStarTypeName(BaseStar.Type.values()[res.getInt(7)]),
            obfuscatedId,
            escapeValue(empire == null ? "" : empire.getDisplayName())));
        numStars++;
      }
    }

    return numStars;
  }

  private String escapeValue(String value) {
    if (value == null) {
      return "";
    }

    value = value.replace("\r", "").replace("\n", "");
    if (value.indexOf(',') >= 0) {
      if (value.indexOf('"') >= 0) {
        value = value.replace("\"", "\\\"");
      }
      value = "\""+value+"\"";
    }
    return value;
  }

  private String getStarTypeName(BaseStar.Type type) {
    return BaseStar.getStarType(type).getDisplayName();
  }
}
