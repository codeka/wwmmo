package au.com.codeka.warworlds.server.store

import au.com.codeka.warworlds.common.proto.Account
import au.com.codeka.warworlds.common.proto.DeviceInfo
import au.com.codeka.warworlds.common.proto.LoginRequest
import au.com.codeka.warworlds.server.proto.DailyStat
import au.com.codeka.warworlds.server.proto.LoginEvent
import au.com.codeka.warworlds.server.store.base.BaseStore
import org.joda.time.DateTime
import java.util.*

/**
 * Store for various stats that we want to keep track of.
 */
class StatsStore internal constructor(fileName: String) : BaseStore(fileName) {
  /** Store the given [LoginRequest]  */
  fun addLoginEvent(loginRequest: LoginRequest?, account: Account) {
    val now = System.currentTimeMillis()
    newWriter()
        .stmt("INSERT INTO login_events (" +
            "timestamp, day, empire_id, device_id, email_addr, device_info) " +
            "VALUES (?, ?, ?, ?, ?, ?)")
        .param(0, now)
        .param(1, StatsHelper.timestampToDay(now))
        .param(2, if (account.empire_id == null) 0 else account.empire_id)
        .param(3, loginRequest!!.device_info.device_id)
        .param(4, account.email)
        .param(5, loginRequest.device_info.encode())
        .execute()
  }

  /** Gets the most recent `num` login events.  */
  fun getRecentLogins(num: Int): List<LoginEvent> {
    val loginEvents: ArrayList<LoginEvent> = ArrayList<LoginEvent>()
    newReader()
        .stmt("SELECT timestamp, day, empire_id, email_addr, device_info FROM login_events ORDER BY timestamp DESC")
        .query().use { res ->
          while (res.next()) {
            loginEvents.add(LoginEvent.Builder()
                .timestamp(res.getLong(0))
                .day(res.getInt(1))
                .empire_id(res.getLong(2))
                .email_addr(res.getStringOrNull(3))
                .device_info(DeviceInfo.ADAPTER.decode(res.getBytes(4)))
                .build())
            if (loginEvents.size >= num) {
              break
            }
          }
        }
    return loginEvents
  }

  /** Get the [DailyStat] for the last `num` days.  */
  fun getDailyStats(num: Int): Map<Int, DailyStat> {
    val dt = DateTime.now().minusDays(num + 7) // 7 more to calculate the 7da correctly
    var currDay = StatsHelper.dateTimeToDay(dt)
    val lastEmpires = ArrayList<MutableSet<Long>>()
    lastEmpires.add(HashSet())
    val dailyStats: HashMap<Int, DailyStat> = HashMap<Int, DailyStat>()
    newReader()
        .stmt("SELECT day, empire_id FROM login_events WHERE day >= ? ORDER BY day ASC")
        .param(0, currDay)
        .query().use { res ->
          while (res.next()) {
            val day = res.getInt(0)
            val empireId = res.getLong(1)
            if (day == currDay) {
              lastEmpires[0].add(empireId)
            } else {
              appendStats(dailyStats, currDay, lastEmpires)
              currDay = day
              lastEmpires.add(0, HashSet())
              lastEmpires[0].add(empireId)
            }
          }
          appendStats(dailyStats, currDay, lastEmpires)
        }
    return dailyStats
  }

  private fun appendStats(
      dailyStats: MutableMap<Int, DailyStat>,
      day: Int,
      lastEmpires: ArrayList<MutableSet<Long>>) {
    val sevenda: MutableSet<Long> = HashSet()
    var i = 0
    while (i < 7 && i < lastEmpires.size) {
      sevenda.addAll(lastEmpires[i])
      i++
    }
    dailyStats[day] = DailyStat.Builder()
        .day(day)
        .oneda(lastEmpires[0].size)
        .sevenda(sevenda.size)
        .signups(0) // TODO: populate
        .build()
  }

  override fun onOpen(diskVersion: Int): Int {
    var version = diskVersion
    if (version == 0) {
      newWriter()
          .stmt(
              "CREATE TABLE login_events (" +
                  "  timestamp INTEGER," +
                  "  day INTEGER," +
                  "  empire_id INTEGER," +
                  "  device_id STRING," +
                  "  email_addr STRING," +
                  "  device_info BLOB)")
          .execute()
      newWriter()
          .stmt("CREATE INDEX IX_login_events_day ON login_events (day)")
          .execute()
      newWriter()
          .stmt(
              "CREATE TABLE create_empire_events (" +
                  "  timestamp INTEGER," +
                  "  day INTEGER," +
                  "  empire_id INTEGER)")
          .execute()
      newWriter()
          .stmt("CREATE INDEX IX_create_empire_events_day ON create_empire_events (day)")
          .execute()
      version++
    }
    return version
  }
}