package au.com.codeka.warworlds.client.game.build

import au.com.codeka.warworlds.common.proto.Colony
import au.com.codeka.warworlds.common.proto.Star

/**
 * Interface implemented by the build tabs ([BuildingsView], [ShipsView]) etc.
 */
interface TabContentView {
  fun refresh(star: Star, colony: Colony)
}