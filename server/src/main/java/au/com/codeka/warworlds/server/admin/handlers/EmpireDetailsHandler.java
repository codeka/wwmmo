package au.com.codeka.warworlds.server.admin.handlers;

import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;

import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.server.admin.RequestException;
import au.com.codeka.warworlds.server.store.DataStore;

/**
 * Handler for /admin/empires/xxx which shows details about the empire with id xxx.
 */
public class EmpireDetailsHandler extends AdminHandler {
  @Override
  public void get() throws RequestException {
    long id = Long.parseLong(getUrlParameter("id"));
    Empire empire = DataStore.i.empires().get(id);

    Iterable<Star> stars =
        DataStore.i.starEmpireSecondaryStore().getStarsForEmpire(null, empire.id);

    render("empires/details.html", ImmutableMap.<String, Object>builder()
        .put("empire", empire)
        .put("stars", stars)
        .build());
  }
}
