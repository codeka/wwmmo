package au.com.codeka.warworlds.client.game.sitrep

import android.view.ViewGroup
import au.com.codeka.warworlds.client.game.solarsystem.SolarSystemScreen
import au.com.codeka.warworlds.client.ui.Screen
import au.com.codeka.warworlds.client.ui.ScreenContext
import au.com.codeka.warworlds.client.ui.ShowInfo
import au.com.codeka.warworlds.common.proto.Star

class SitReportScreen : Screen() {
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

  override fun onShow(): ShowInfo? {
    return ShowInfo.builder().view(layout).build()
  }
}