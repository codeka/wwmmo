package au.com.codeka.warworlds.server.admin.handlers;

import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.world.StarManager;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;

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

    ArrayList<Star> stars = new ArrayList<>();
    for (Long starId : DataStore.i.stars().getStarsForEmpire(empire.id)) {
      stars.add(StarManager.i.getStar(starId).get());
    }

    render("empires/details.html", ImmutableMap.<String, Object>builder()
        .put("empire", empire)
        .put("stars", stars)
        .build());
  }
}
