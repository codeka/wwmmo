package au.com.codeka.warworlds.server.store

import au.com.codeka.warworlds.common.proto.SituationReport
import au.com.codeka.warworlds.server.store.base.BaseStore

class SitReportsStore internal constructor(fileName: String) : BaseStore(fileName) {
  fun save(sitReports: Collection<SituationReport>) {
    val writer = newWriter()
        .stmt("INSERT INTO sit_reports (empire_id, star_id, report_time, sit_report)" +
             " VALUES (?, ?, ?, ?)")
    for(sitReport in sitReports) {
      writer.param(0, sitReport.empire_id)
      writer.param(1, sitReport.star_id)
      writer.param(2, sitReport.report_time)
      writer.param(3, sitReport.encode())
      writer.execute()
    }
  }

  fun getByEmpireId(empireId: Long, limit: Int): List<SituationReport> {
    newReader()
        .stmt(
            "SELECT sit_report " +
            "FROM sit_reports " +
            "WHERE empire_id = ? " +
            "ORDER BY report_time " +
            "DESC LIMIT ?")
        .param(0, empireId)
        .param(1, limit)
        .query().use { res ->
          val sitReports = ArrayList<SituationReport>()
          while (res.next()) {
            sitReports.add(SituationReport.ADAPTER.decode(res.getBytes(0)))
          }
          return sitReports
        }
  }

  override fun onOpen(diskVersion: Int): Int {
    var version = diskVersion
    if (version == 0) {
      newWriter()
          .stmt(
              "CREATE TABLE sit_reports (" +
                  "  empire_id INTEGER," +
                  "  star_id INTEGER," +
                  "  report_time INTEGER," +
                  "  sit_report BLOB)")
          .execute()
      version++
    }

    return version
  }
}
