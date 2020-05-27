package au.com.codeka.warworlds.common.sim

import au.com.codeka.warworlds.common.proto.Designs

/** Holder class that just holds all the design definitions.  */
object DesignDefinitions {
  /** The list of all designs in the game.  */
  lateinit var designs: Designs

  fun init(designs: Designs) {
    this.designs = designs
  }
}