package au.com.codeka.warworlds.server.monitor;

import org.joda.time.DateTime;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.BackgroundRunner;
import au.com.codeka.warworlds.server.Session;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;

/**
 * Contains a mapping of empires to the IP address(es) they're coming from.
 * <p/>
 * Periodically, we will write this mapping out to the database and clear out the cached values.
 */
public class EmpireIpAddressMonitor extends Monitor {
  private static final Log log = new Log("EmpireIpAddressMonitor");
  private final Map<Integer, ArrayList<MapEntry>> entries = new TreeMap<>();
  private DateTime lastSaveTime = DateTime.now();

  @Override
  public void onBeginRequest(Session session, HttpServletRequest request,
      HttpServletResponse response) {
    if (session == null) {
      // Not authenticated, don't care.
      return;
    }

    int empireID = session.getEmpireID();
    String ipAddress = request.getHeader("X-Real-IP");
    if (ipAddress == null) {
      ipAddress = request.getRemoteAddr();
    }

    synchronized (entries) {
      ArrayList<MapEntry> empireEntries = entries.get(empireID);
      if (empireEntries == null) {
        empireEntries = new ArrayList<>();
        entries.put(empireID, empireEntries);
      }

      boolean found = false;
      for (MapEntry entry : empireEntries) {
        if (entry.ipAddress.equals(ipAddress)) {
          entry.lastSeen = DateTime.now();
          found = true;
          break;
        }
      }
      if (!found) {
        empireEntries.add(new MapEntry(empireID, ipAddress, DateTime.now()));
      }
    }

    if (lastSaveTime.isBefore(DateTime.now().minusMinutes(5))) {
      lastSaveTime = DateTime.now();

      // If we haven't saved for five minutes, save now.
      Map<Integer, ArrayList<MapEntry>> copy;
      synchronized (entries) {
        copy = new TreeMap<>(entries);
        entries.clear();
      }

      saveEntries(copy);
    }
  }

  // static to be sure we don't accidentally access any class state in a separate thread...
  private static void saveEntries(final Map<Integer, ArrayList<MapEntry>> entries) {
    new BackgroundRunner() {
      @Override
      protected void doInBackground() {
        String sql = "UPDATE empire_ips SET last_seen = ? WHERE empire_id = ? AND ip_address = ?;"
            + " INSERT INTO empire_ips (empire_id, ip_address, last_seen)"
            + " SELECT ?, ?, ?"
            + " WHERE NOT EXISTS (SELECT 1 FROM empire_ips WHERE empire_id = ? AND ip_address = ?)";

        try (SqlStmt stmt = DB.prepare(sql)) {
          for (ArrayList<MapEntry> mapEntry : entries.values()) {
            for (MapEntry singleEntry : mapEntry) {
              saveEntry(stmt, singleEntry);
            }
          }
        } catch (Exception e) {
          log.error("Exception caught updating IP address map.", e);
        }
      }
    }.execute();
  }

  /** Saves a single entry to the database. */
  private static void saveEntry(SqlStmt stmt, MapEntry entry) throws SQLException {
    stmt.setDateTime(1, entry.lastSeen);
    stmt.setInt(2, entry.empireID);
    stmt.setString(3, entry.ipAddress);
    stmt.setInt(4, entry.empireID);
    stmt.setString(5, entry.ipAddress);
    stmt.setDateTime(6, entry.lastSeen);
    stmt.setInt(7, entry.empireID);
    stmt.setString(8, entry.ipAddress);
    stmt.update();
  }

  private static class MapEntry {
    public int empireID;
    public String ipAddress;
    public DateTime lastSeen;

    public MapEntry(int empireID, String ipAddress, DateTime lastSeen) {
      this.empireID = empireID;
      this.ipAddress = ipAddress;
      this.lastSeen = lastSeen;
    }
  }
}
