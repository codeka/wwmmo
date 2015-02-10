package au.com.codeka.warworlds.server.ctrl;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.OutputStream;
import java.io.OutputStreamWriter;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;

public class StarExportController {
  private static final Log log = new Log("StarExportController");
  private DataBase db;

  public StarExportController() {
    db = new DataBase();
  }

  public void export(OutputStream outs) {
    String[] header = new String[] {"x", "y", "name", "size", "type", "empire_name"};
    try (CSVPrinter printer = CSVFormat.RFC4180.withHeader(header)
        .print(new OutputStreamWriter(outs))) {
      String sql =
          "SELECT sectors.x AS sector_x, sectors.y AS sector_y, stars.x, stars.y, name, size,"
              + " star_type, (SELECT empires.name FROM colonies INNER JOIN empires"
              + " ON empires.id = colonies.empire_id WHERE colonies.star_id = stars.id"
              + " GROUP BY empire_id, empires.name ORDER BY COUNT(*) DESC LIMIT 1) AS empire_name"
              + " FROM stars INNER JOIN sectors ON sectors.id = stars.sector_id";
      try (SqlStmt stmt = db.prepare(sql)) {
        SqlResult res = stmt.select();

        while (res.next()) {
          printer.printRecord(
              String.format("%d.%d", res.getInt(1), res.getInt(3)),
              String.format("%d.%d", res.getInt(2), res.getInt(4)),
              res.getString(5),
              Integer.toString(res.getInt(6)),
              getStarTypeName(BaseStar.Type.values()[res.getInt(7)]),
              res.getString(8));
        }
      }
    } catch (Exception e) {
      log.error("Error exporting stars.", e);
    }
  }

  private String getStarTypeName(BaseStar.Type type) {
    return BaseStar.getStarType(type).getDisplayName();
  }

  private static class DataBase extends BaseDataBase {
    public DataBase() {
      super();
    }
  }
}
