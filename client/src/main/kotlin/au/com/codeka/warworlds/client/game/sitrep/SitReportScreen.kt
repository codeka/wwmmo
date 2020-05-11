package au.com.codeka.warworlds.client.game.sitrep

import android.view.ViewGroup
import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.client.game.solarsystem.SolarSystemScreen
import au.com.codeka.warworlds.client.ui.Screen
import au.com.codeka.warworlds.client.ui.ScreenContext
import au.com.codeka.warworlds.client.ui.ShowInfo
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.RpcPacket
import au.com.codeka.warworlds.common.proto.SituationReport
import au.com.codeka.warworlds.common.proto.Star

class SitReportScreen : Screen() {
  companion object {
    private val log = Log("SitReportScreen")
  }

  private lateinit var layout: SitReportLayout

  override fun onCreate(context: ScreenContext, container: ViewGroup) {
    super.onCreate(context, container)
    layout = SitReportLayout(
        context.activity,
        object : SitReportLayout.Callback {
          override fun onStarClick(star: Star?) {
            context.pushScreen(SolarSystemScreen(star!!, -1))
          }
        })
  }

  fun refresh(sitReports: List<SituationReport>) {
    log.info("populating response: ${sitReports.size} reports")
    // TODO: implement me!
  }

  override fun onShow(): ShowInfo? {
    App.taskRunner.runTask(Runnable {
      val resp = App.server.sendRpc(RpcPacket.Builder()
          .sit_report_request(RpcPacket.SitReportRequest.Builder()
              .build())
          .build())

      App.taskRunner.runTask(Runnable {
        refresh(resp.sit_report_response.sit_reports)
      }, Threads.UI)
    }, Threads.BACKGROUND)

    return ShowInfo.builder().view(layout).build()
  }
}