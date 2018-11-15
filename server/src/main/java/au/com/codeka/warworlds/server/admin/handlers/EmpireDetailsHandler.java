package au.com.codeka.warworlds.server.admin.handlers;

import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.List;

import au.com.codeka.warworlds.common.proto.DeviceInfo;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Notification;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.world.NotificationManager;
import au.com.codeka.warworlds.server.world.StarManager;

/**
 * Handler for /admin/empires/xxx which shows details about the empire with id xxx.
 */
public class EmpireDetailsHandler extends AdminHandler {
  @Override
  public void get() throws RequestException {
    long id = Long.parseLong(getUrlParameter("id"));
    Empire empire = DataStore.i.empires().get(id);
    if (empire == null) {
      throw new RequestException(404);
    }


    complete(empire, ImmutableMap.builder());
  }

  @Override
  public void post() throws RequestException {
    long id = Long.parseLong(getUrlParameter("id"));
    Empire empire = DataStore.i.empires().get(id);
    if (empire == null) {
      throw new RequestException(404);
    }

    String msg = getRequest().getParameter("msg");
    if (msg.length() == 0) {
      complete(empire, ImmutableMap.<String, Object>builder()
          .put("error", "You need to specify a message."));
      return;
    }

    // TODO: send it
    NotificationManager.i.sendNotification(empire, new Notification.Builder()
        .debug_message(msg)
        .build());

    redirect("/admin/empires/" + id);
  }

  private void complete(Empire empire, ImmutableMap.Builder<String, Object> mapBuilder)
      throws RequestException {
    ArrayList<Star> stars = new ArrayList<>();
    for (Long starId : DataStore.i.stars().getStarsForEmpire(empire.id)) {
      stars.add(StarManager.i.getStar(starId).get());
    }

    List<DeviceInfo> devices = DataStore.i.empires().getDevicesForEmpire(empire.id);
    render("empires/details.html", mapBuilder
        .put("empire", empire)
        .put("stars", stars)
        .put("devices", devices)
        .build());
  }
}
