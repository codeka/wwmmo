package au.com.codeka.warworlds.server.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.DeviceInfo;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.server.proto.PatreonInfo;
import au.com.codeka.warworlds.server.store.base.BaseStore;
import au.com.codeka.warworlds.server.store.base.QueryResult;
import au.com.codeka.warworlds.server.store.base.StoreReader;

/** Storage for empires. */
public class EmpiresStore extends BaseStore {
  private static final Log log = new Log("EmpiresStore");

  /* package */ EmpiresStore(String fileName) {
    super(fileName);
  }

  @Nullable
  public Empire get(long id) {
    try (
        QueryResult res = newReader()
            .stmt("SELECT empire FROM empires WHERE id = ?")
            .param(0, id)
            .query()
    ) {
      if (res.next()) {
        return Empire.ADAPTER.decode(res.getBytes(0));
      }
    } catch (Exception e) {
      log.error("Unexpected.", e);
    }
    return null;
  }

  public List<Long> search(@Nullable String query) {
    ArrayList<Long> empireIds = new ArrayList<>();

    StoreReader reader = newReader();
    if (query == null) {
      reader.stmt("SELECT id FROM empires");
    } else {
      reader
          .stmt("SELECT id FROM empires WHERE empire_name LIKE ?")
          .param(0, query + "%");
    }

    try (QueryResult res = reader.query()) {
      while (res.next()) {
        empireIds.add(res.getLong(0));
      }
    } catch (Exception e) {
      log.error("Unexpected.", e);
    }
    return empireIds;
  }

  public void put(long id, Empire empire) {
    try {
      newWriter()
          .stmt("INSERT OR REPLACE INTO empires (id, empire, empire_name) VALUES (?, ?, ?)")
          .param(0, id)
          .param(1, empire.encode())
          .param(2, empire.display_name)
          .execute();
    } catch (StoreException e) {
      log.error("Unexpected.", e);
    }
  }

  /**
   * Saves the given device to the empire store, under the given empire. When we want to msg the
   * empire, these devices are what we'll message.
   */
  public void saveDevice(Empire empire, DeviceInfo deviceInfo) {
    try {
      newWriter()
          .stmt("INSERT OR REPLACE INTO devices (empire_id, device_id, device) VALUES (?, ?, ?)")
          .param(0, empire.id)
          .param(1, deviceInfo.device_id)
          .param(2, deviceInfo.encode())
          .execute();
    } catch(StoreException e) {
      log.error("Unexpected.", e);
    }
  }

  public List<DeviceInfo> getDevicesForEmpire(long empireId) {
    ArrayList<DeviceInfo> devices = new ArrayList<>();
    try (
        QueryResult res = newReader()
            .stmt("SELECT device FROM devices WHERE empire_id = ?")
            .param(0, empireId)
            .query()
    ) {
      while (res.next()) {
        devices.add(DeviceInfo.ADAPTER.decode(res.getBytes(0)));
      }
    } catch (Exception e) {
      log.error("Unexpected.", e);
    }
    return devices;
  }

  public void savePatreonInfo(long empireID, PatreonInfo patreonInfo) {
    try {
      newWriter()
          .stmt("INSERT OR REPLACE INTO patreon_info (" +
              "empire_id, token_expiry_time, patreon_info) VALUES (?, ?, ?)")
          .param(0, empireID)
          .param(1, patreonInfo.token_expiry_time)
          .param(2, patreonInfo.encode())
          .execute();
    } catch (StoreException e) {
      log.error("Unexpected.", e);
    }
  }

  /** Gets the {@link PatreonInfo} for the given empire. */
  @Nullable
  public PatreonInfo getPatreonInfo(long empireId) {
    try {
      QueryResult res = newReader()
          .stmt("SELECT patreon_info FROM patreon_info WHERE empire_id=?")
          .param(0, empireId)
          .query();
      if (res.next()) {
        return PatreonInfo.ADAPTER.decode(res.getBytes(0));
      }
      return null;
    } catch (Exception e) {
      log.error("Unexpected.", e);
      return null;
    }
  }

  @Override
  protected int onOpen(int diskVersion) throws StoreException {
    if (diskVersion == 0) {
      newWriter()
          .stmt(
              "CREATE TABLE empires (" +
                  "  id INTEGER PRIMARY KEY," +
                  "  empire BLOB)")
          .execute();
      diskVersion++;
    }
    if (diskVersion == 1) {
      newWriter()
          .stmt(
              "CREATE TABLE devices (" +
                  "  empire_id INTEGER," +
                  "  device_id STRING," +
                  "  device BLOB)")
          .execute();
      newWriter()
          .stmt("CREATE UNIQUE INDEX IX_devices_empire_device ON devices (empire_id, device_id)")
          .execute();
      diskVersion++;
    }
    if (diskVersion == 2) {
      newWriter()
          .stmt(
              "CREATE TABLE patreon_info (" +
                  "  empire_id INTEGER PRIMARY KEY," +
                  "  token_expiry_time INTEGER," +
                  "  patreon_info BLOB)")
          .execute();
      diskVersion++;
    }
    if (diskVersion == 3) {
      newWriter()
          .stmt(
              "ALTER TABLE empires ADD COLUMN empire_name STRING")
          .execute();
      newWriter()
          .stmt("CREATE UNIQUE INDEX IX_empires_empire_name ON empires (empire_name)")
          .execute();

      // Update the empires, make sure their names are unique as we go.
      QueryResult res = newReader()
          .stmt("SELECT empire FROM empires")
          .query();
      ArrayList<Empire> empires = new ArrayList<>();
      while (res.next()) {
        try {
          empires.add(Empire.ADAPTER.decode(res.getBytes(0)));
        } catch (IOException e) {
          throw new StoreException(e);
        }
      }

      HashSet<String> seenNames = new HashSet<>();
      for (Empire empire : empires) {
        if (empire.display_name.trim().equals("")) {
          empire = empire.newBuilder().display_name("~").build();
        }

        while (seenNames.contains(empire.display_name)) {
          empire = empire.newBuilder().display_name(empire.display_name + "~").build();
        }
        seenNames.add(empire.display_name);

        newWriter()
            .stmt("UPDATE empires SET empire_name=?, empire=? WHERE id=?")
            .param(0, empire.display_name)
            .param(1, empire.encode())
            .param(2, empire.id)
            .execute();
      }

      diskVersion++;
    }

    return diskVersion;
  }
}
