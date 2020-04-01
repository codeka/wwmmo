package au.com.codeka.warworlds.server.cron.jobs;

import com.google.common.base.Strings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.warworlds.server.Configuration;
import au.com.codeka.warworlds.server.cron.AbstractCronJob;
import au.com.codeka.warworlds.server.cron.CronJob;
import au.com.codeka.warworlds.server.ctrl.BaseDataBase;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;

/**
 * Exports all the stars in the database to a .csv file.
 */
@CronJob(name = "Export Stars", desc = "Exports all stars in the database to a .csv file.")
public class ExportStarsCronJob extends AbstractCronJob {

  private static final Log log = new Log("ExportStarsCronJob");

  @Override
  public String run(String extra) throws Exception {
    String fileName = extra;
    if (Strings.isNullOrEmpty(fileName)) {
      fileName = new File(Configuration.i.getDataDirectory(), "export.csv").getAbsolutePath();
    }
    log.info("Exporting stars to: %s", fileName);
    OutputStream outs =  new FileOutputStream(fileName);
    try {
      long n = export(outs);
      return String.format(Locale.ENGLISH, "Exported %d stars to: %s", n, fileName);
    } finally {
      outs.close();
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
        "      empires.name" +
        "    FROM" +
        "      colonies INNER JOIN empires ON empires.id = colonies.empire_id" +
        "    WHERE colonies.star_id = stars.id" +
        "    GROUP BY empire_id, empires.name" +
        "    ORDER BY COUNT(*) DESC LIMIT 1) AS empire_name " +
        "FROM stars " +
        "INNER JOIN sectors ON sectors.id = stars.sector_id";
    try (SqlStmt stmt = DB.prepare(sql)) {
      SqlResult res = stmt.select();

      ps.println("x,y,name,size,type,empire_name");
      while (res.next()) {
        ps.println(String.format(Locale.ENGLISH, "%d.%d,%d.%d,%s,%s,%s,%s",
            res.getInt(1), res.getInt(3), res.getInt(2), res.getInt(4),
            escapeValue(res.getString(5)), res.getInt(6),
            getStarTypeName(BaseStar.Type.values()[res.getInt(7)]),
            escapeValue(res.getString(8))));
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
