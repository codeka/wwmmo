package au.com.codeka.warworlds.server.cron.jobs;

import com.google.protobuf.InvalidProtocolBufferException;

import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.cron.AbstractCronJob;
import au.com.codeka.warworlds.server.cron.CronJob;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;

@CronJob(
    name = "Accessibility services gatherer",
    desc = "Looks up our login events and gathers all the accessibility services we've seen")
public class AccessibilityServiceGathererCronJob extends AbstractCronJob {
  private static final Log log = new Log("AccessibilityServiceGathererCronJob");

  @Override
  public String run(String extra) throws Exception {
    HashMap<String, ServiceInfo> services = new HashMap<>();

    String sql = "" +
        "SELECT " +
        "  empire_logins.empire_id, accessibility_service_infos, date " +
        "FROM empire_logins " +
        "WHERE num_accessibility_services > 0 " +
        "ORDER BY empire_logins.date DESC " +
        "LIMIT 1000"; // TODO: instead of limiting by number, take the last N days.
    try (SqlStmt stmt = DB.prepare(sql)) {
      SqlResult res = stmt.select();

      while (res.next()) {
        byte[] blob = res.getBytes(2);
        Messages.AccessibilitySettingsInfo accessibilitySettingsInfo;
        try {
          accessibilitySettingsInfo = Messages.AccessibilitySettingsInfo.parseFrom(blob);
        } catch (InvalidProtocolBufferException e) {
          log.warning("Error parsing accessibility settings info.", e);
          continue;
        }

        int empireId = res.getInt(1);
        DateTime dt = res.getDateTime(3);
        for (Messages.AccessibilitySettingsInfo.AccessibilityService service :
            accessibilitySettingsInfo.getServiceList()) {
          ServiceInfo serviceInfo = services.get(service.getName());
          if (serviceInfo == null) {
            serviceInfo = new ServiceInfo(service.getName());
            services.put(service.getName(), serviceInfo);
          }
          serviceInfo.addEmpire(empireId, dt);
        }
      }
    }

    // Delete everything so we can add it all back again.
    sql = "DELETE FROM accessibility_services";
    try (SqlStmt stmt = DB.prepare(sql)) {
      stmt.update();
    }

    sql = "INSERT INTO accessibility_services (name, empires, first_seen, last_seen) " +
          "VALUES (?, ?, ?, ?)";
    try (SqlStmt stmt = DB.prepare(sql)) {
      for (ServiceInfo serviceInfo : services.values()) {
        stmt.setString(1, serviceInfo.name);
        StringBuilder empires = new StringBuilder();
        for (int empireId : serviceInfo.empires) {
          empires.append(empireId);
          empires.append(" ");
        }
        stmt.setString(2, empires.toString().trim());
        stmt.setDateTime(3, serviceInfo.firstSeen);
        stmt.setDateTime(4, serviceInfo.lastSeen);
        stmt.update();
      }
    }

    return String.format(Locale.ENGLISH, "Found %d unique services.", services.size());
  }

  private static class ServiceInfo {
    private String name;
    private Set<Integer> empires;
    private DateTime firstSeen;
    private DateTime lastSeen;

    public ServiceInfo(String name) {
      this.name = name;
      empires = new HashSet<>();
    }

    public void addEmpire(int empireId, DateTime dt) {
      empires.add(empireId);

      if (firstSeen == null || firstSeen.isAfter(dt)) {
        firstSeen = dt;
      }
      if (lastSeen == null || lastSeen.isBefore(dt)) {
        lastSeen = dt;
      }
    }
  }
}
