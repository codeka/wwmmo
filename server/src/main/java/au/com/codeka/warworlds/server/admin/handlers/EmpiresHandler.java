package au.com.codeka.warworlds.server.admin.handlers;

import com.google.common.collect.ImmutableMap;
import com.sleepycat.je.utilint.Pair;

import java.util.ArrayList;

import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.server.admin.RequestException;
import au.com.codeka.warworlds.server.store.BaseStore;
import au.com.codeka.warworlds.server.store.DataStore;

/**
 * Handler for /admin/empires which lists all empires in the database.
 */
public class EmpiresHandler extends AdminHandler {
  @Override
  public void get() throws RequestException {
    BaseStore<Long, Empire>.StoreCursor cursor = DataStore.i.empires().search();
    Pair<Long, Empire> pair;
    ArrayList<Empire> empires = new ArrayList<>();
    while ((pair = cursor.next()) != null) {
      empires.add(pair.second());
    }

    render("empires/index.html", ImmutableMap.<String, Object>builder()
        .put("empires", empires)
        .build());
  }
}
