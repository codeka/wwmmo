package au.com.codeka.warworlds.server.store

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.DeviceInfo
import au.com.codeka.warworlds.common.proto.Empire
import au.com.codeka.warworlds.server.proto.PatreonInfo
import au.com.codeka.warworlds.server.store.StoreException
import au.com.codeka.warworlds.server.store.base.BaseStore
import java.io.IOException
import java.util.*

/** Storage for empires.  */
class EmpiresStore  /* package */
internal constructor(fileName: String) : BaseStore(fileName) {
  operator fun get(id: Long): Empire? {
    try {
      newReader()
          .stmt("SELECT empire FROM empires WHERE id = ?")
          .param(0, id)
          .query().use { res ->
            if (res.next()) {
              return Empire.ADAPTER.decode(res.getBytes(0))
            }
          }
    } catch (e: Exception) {
      log.error("Unexpected.", e)
    }
    return null
  }

  fun search(query: String?): List<Long> {
    val empireIds = ArrayList<Long>()
    val reader = newReader()
    if (query == null) {
      reader!!.stmt("SELECT id FROM empires")
    } else {
      reader
          .stmt("SELECT id FROM empires WHERE empire_name LIKE ?")
          .param(0, "$query%")
    }
    try {
      reader!!.query().use { res ->
        while (res.next()) {
          empireIds.add(res.getLong(0))
        }
      }
    } catch (e: Exception) {
      log.error("Unexpected.", e)
    }
    return empireIds
  }

  fun put(id: Long, empire: Empire) {
    try {
      newWriter()
          .stmt("INSERT OR REPLACE INTO empires (id, empire, empire_name) VALUES (?, ?, ?)")
          .param(0, id)
          .param(1, empire.encode())
          .param(2, empire.display_name)
          .execute()
    } catch (e: StoreException) {
      log.error("Unexpected.", e)
    }
  }

  /**
   * Saves the given device to the empire store, under the given empire. When we want to msg the
   * empire, these devices are what we'll message.
   */
  fun saveDevice(empire: Empire?, deviceInfo: DeviceInfo) {
    try {
      newWriter()
          .stmt("INSERT OR REPLACE INTO devices (empire_id, device_id, device) VALUES (?, ?, ?)")
          .param(0, empire!!.id)
          .param(1, deviceInfo.device_id)
          .param(2, deviceInfo.encode())
          .execute()
    } catch (e: StoreException) {
      log.error("Unexpected.", e)
    }
  }

  fun getDevicesForEmpire(empireId: Long): List<DeviceInfo> {
    val devices = ArrayList<DeviceInfo>()
    try {
      newReader()
          .stmt("SELECT device FROM devices WHERE empire_id = ?")
          .param(0, empireId)
          .query().use { res ->
            while (res.next()) {
              devices.add(DeviceInfo.ADAPTER.decode(res.getBytes(0)))
            }
          }
    } catch (e: Exception) {
      log.error("Unexpected.", e)
    }
    return devices
  }

  fun savePatreonInfo(empireID: Long, patreonInfo: PatreonInfo) {
    try {
      newWriter()
          .stmt("INSERT OR REPLACE INTO patreon_info (" +
              "empire_id, token_expiry_time, patreon_info) VALUES (?, ?, ?)")
          .param(0, empireID)
          .param(1, patreonInfo.token_expiry_time)
          .param(2, patreonInfo.encode())
          .execute()
    } catch (e: StoreException) {
      log.error("Unexpected.", e)
    }
  }

  /** Gets the [PatreonInfo] for the given empire.  */
  fun getPatreonInfo(empireId: Long): PatreonInfo? {
    return try {
      val res = newReader()
          .stmt("SELECT patreon_info FROM patreon_info WHERE empire_id=?")
          .param(0, empireId)
          .query()
      if (res!!.next()) {
        PatreonInfo.ADAPTER.decode(res.getBytes(0))
      } else null
    } catch (e: Exception) {
      log.error("Unexpected.", e)
      null
    }
  }

  @Throws(StoreException::class)
  override fun onOpen(diskVersion: Int): Int {
    var diskVersion = diskVersion
    if (diskVersion == 0) {
      newWriter()
          .stmt(
              "CREATE TABLE empires (" +
                  "  id INTEGER PRIMARY KEY," +
                  "  empire BLOB)")
          .execute()
      diskVersion++
    }
    if (diskVersion == 1) {
      newWriter()
          .stmt(
              "CREATE TABLE devices (" +
                  "  empire_id INTEGER," +
                  "  device_id STRING," +
                  "  device BLOB)")
          .execute()
      newWriter()
          .stmt("CREATE UNIQUE INDEX IX_devices_empire_device ON devices (empire_id, device_id)")
          .execute()
      diskVersion++
    }
    if (diskVersion == 2) {
      newWriter()
          .stmt(
              "CREATE TABLE patreon_info (" +
                  "  empire_id INTEGER PRIMARY KEY," +
                  "  token_expiry_time INTEGER," +
                  "  patreon_info BLOB)")
          .execute()
      diskVersion++
    }
    if (diskVersion == 3) {
      newWriter()
          .stmt(
              "ALTER TABLE empires ADD COLUMN empire_name STRING")
          .execute()
      newWriter()
          .stmt("CREATE UNIQUE INDEX IX_empires_empire_name ON empires (empire_name)")
          .execute()

      // Update the empires, make sure their names are unique as we go.
      val res = newReader()
          .stmt("SELECT empire FROM empires")
          .query()
      val empires = ArrayList<Empire>()
      while (res.next()) {
        try {
          empires.add(Empire.ADAPTER.decode(res.getBytes(0)))
        } catch (e: IOException) {
          throw StoreException(e)
        }
      }
      val seenNames = HashSet<String>()
      for (i in 0 until empires.size) {
        var empire = empires[i]
        if (empire.display_name.trim { it <= ' ' } == "") {
          empire = empire.newBuilder().display_name("~").build()
        }
        while (seenNames.contains(empire.display_name)) {
          empire = empire.newBuilder().display_name(empire.display_name + "~").build()
        }
        seenNames.add(empire.display_name)
        newWriter()
            .stmt("UPDATE empires SET empire_name=?, empire=? WHERE id=?")
            .param(0, empire.display_name)
            .param(1, empire.encode())
            .param(2, empire.id)
            .execute()
      }
      diskVersion++
    }
    return diskVersion
  }

  companion object {
    private val log = Log("EmpiresStore")
  }
}