package au.com.codeka.warworlds.client.game.build

import au.com.codeka.warworlds.common.proto.Star

/** Interface implemented by the various bottom panes. */
interface BottomPaneContentView {
  fun refresh(star: Star?)
}
