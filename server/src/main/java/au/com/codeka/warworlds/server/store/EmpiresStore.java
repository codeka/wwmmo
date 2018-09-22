package au.com.codeka.warworlds.server.store;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.DeviceInfo;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.server.store.base.BaseStore;
import au.com.codeka.warworlds.server.store.base.QueryResult;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

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

  public List<Empire> search() {
    ArrayList<Empire> empires = new ArrayList<>();
    try (
        QueryResult res = newReader()
            .stmt("SELECT empire FROM empires")
            .query()
        ) {
      while (res.next()) {
        empires.add(Empire.ADAPTER.decode(res.getBytes(0)));
      }
    } catch (Exception e) {
      log.error("Unexpected.", e);
    }
    return empires;
  }

  public void put(long id, Empire empire) {
    try {
      newWriter()
          .stmt("INSERT OR REPLACE INTO empires (id, empire) VALUES (?, ?)")
          .param(0, id)
          .param(1, empire.encode())
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

    return diskVersion;
  }
}
