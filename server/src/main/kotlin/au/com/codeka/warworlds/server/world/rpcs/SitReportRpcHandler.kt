package au.com.codeka.warworlds.server.world.rpcs

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Empire
import au.com.codeka.warworlds.common.proto.RpcPacket
import au.com.codeka.warworlds.server.store.DataStore
import au.com.codeka.warworlds.server.world.WatchableObject

/**
 * Handles requests for situation reports.
 */
class SitReportRpcHandler : RpcHandler {
  companion object {
    private val log = Log("SitReportRpcHandler")
  }

  override fun handle(empire: WatchableObject<Empire>, rpc: RpcPacket): RpcPacket {
    // TODO: handle the case when the star_id is set
    val sitReports =
        DataStore.i.sitReports().getByEmpireId(empire.get().id, 100 /* TODO: handle limit+paging */)

    return RpcPacket.Builder()
        .sit_report_response(RpcPacket.SitReportResponse.Builder()
            .sit_reports(sitReports)
            .build())
        .build()
  }
}
