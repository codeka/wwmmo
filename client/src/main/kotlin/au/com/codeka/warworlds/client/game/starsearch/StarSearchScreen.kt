package au.com.codeka.warworlds.client.game.starsearch

import android.view.ViewGroup
import au.com.codeka.warworlds.client.game.solarsystem.SolarSystemScreen
import au.com.codeka.warworlds.client.ui.Screen
import au.com.codeka.warworlds.client.ui.ScreenContext
import au.com.codeka.warworlds.client.ui.ShowInfo
import au.com.codeka.warworlds.client.ui.ShowInfo.Companion.builder
import au.com.codeka.warworlds.common.proto.Star

class StarSearchScreen : Screen() {
  private lateinit var layout: StarSearchLayout

  override fun onCreate(context: ScreenContext, container: ViewGroup) {
    super.onCreate(context, container)
    layout = StarSearchLayout(
        context.activity,
        object : StarSearchLayout.Callback {
          override fun onStarClick(star: Star?) {
            context.pushScreen(SolarSystemScreen(star!!, -1))
          }
        })
  }

  override fun onShow(): ShowInfo? {
    return builder().view(layout).build()
  }
}