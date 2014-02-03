package au.com.codeka.warworlds.server.ctrl;

import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.ResultSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.model.BaseStar;
import au.com.codeka.warworlds.server.data.SqlStmt;

public class StarExportController {
    private static final Logger log = LoggerFactory.getLogger(StarExportController.class);
    private DataBase db;

    public StarExportController() {
        db = new DataBase();
    }

    public void export(OutputStream outs) {
        PrintStream ps = new PrintStream(outs);
        try {
            String sql = "SELECT sectors.x AS sector_x, sectors.y AS sector_y, stars.x, stars.y, name, size, star_type, (" +
                                "SELECT empires.name FROM colonies INNER JOIN empires ON empires.id = colonies.empire_id" +
                               " WHERE colonies.star_id = stars.id GROUP BY empire_id ORDER BY COUNT(*) DESC LIMIT 1) AS empire_name" +
                         " FROM stars INNER JOIN sectors ON sectors.id = stars.sector_id";
            try (SqlStmt stmt = db.prepare(sql)) {
                ResultSet rs = stmt.select();

                ps.println("x,y,name,size,type,empire_name");
                while (rs.next()) {
                    ps.println(String.format("%d.%d,%d.%d,%s,%s,%s,%s",
                            rs.getInt(1), rs.getInt(3), rs.getInt(2), rs.getInt(4),
                            escapeValue(rs.getString(5)), rs.getInt(6), getStarTypeName(BaseStar.Type.values()[rs.getInt(7)]),
                            escapeValue(rs.getString(8))));
                }
            }
        } catch (Exception e) {
            ps.println("----ERROR OCCURED, PLEASE REPORT BUGS TO dean@war-worlds.com----");
            log.error("Error exporting stars.", e);
        }
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

    private static class DataBase extends BaseDataBase {
        public DataBase() {
            super();
        }
    }
}
