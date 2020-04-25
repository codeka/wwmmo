package au.com.codeka.warworlds.server.handlers.admin;

import com.google.protobuf.util.JsonFormat;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.model.Empire;
import au.com.codeka.warworlds.server.monitor.RequestStatMonitor;

public class AdminEmpireDetailsHandler extends AdminHandler {
  private static final Log log = new Log("AdminEmpireDetailsHandler");

  @Override
  protected void get() throws RequestException {
    if (!isAdmin()) {
      return;
    }
    TreeMap<String, Object> data = new TreeMap<>();

    int empireID = Integer.parseInt(getUrlParameter("empireid"));
    data.put("empire_id", empireID);

    Empire empire = new EmpireController().getEmpire(empireID);
    data.put("empire", empire);

    ArrayList<Messages.RequestStatHour> stats = RequestStatMonitor.i.getLastHours(48);
    ArrayList<HashMap<Object, Object>> requestGraphData = new ArrayList<>();
    for (int i = 0; i < stats.size(); i++) {
      Messages.RequestStatHour stat = stats.get(i);
      HashMap<Object, Object> graphEntry = new HashMap<>();
      int year = stat.getDay() / 10000;
      int month = (stat.getDay() - (year * 10000)) / 100;
      int day = stat.getDay() - (year * 10000) - (month * 100);
      int hour = stat.getHour();

      graphEntry.put("date", new DateTime(year, month + 1, day, hour, 0, 0));
      graphEntry.put("total", stat.getTotalRequests());
      graphEntry.put("empireRequests", 0);
      graphEntry.put("empireNotRateLimited", 0);
      graphEntry.put("empireSoftRateLimited", 0);
      graphEntry.put("empireHardRateLimited", 0);
      for (Messages.RequestStatEmpireHour empireHour : stat.getEmpireList()) {
        if (empireHour.getEmpireId() == empireID) {
          graphEntry.put("empireRequests", empireHour.getTotalRequests());
          graphEntry.put("empireNotRateLimited", empireHour.getTotalNotRateLimited());
          graphEntry.put("empireSoftRateLimited", empireHour.getTotalSoftRateLimited());
          graphEntry.put("empireHardRateLimited", empireHour.getTotalHardRateLimited());
        }
      }
      requestGraphData.add(graphEntry);
    }
    data.put("request_graph", requestGraphData);

    render("admin/empire/ajax-empire-details.html", data);
  }
}
