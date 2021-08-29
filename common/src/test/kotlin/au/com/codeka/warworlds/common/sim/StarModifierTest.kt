package au.com.codeka.warworlds.common.sim

import au.com.codeka.warworlds.common.proto.StarModification
import org.junit.jupiter.api.Test

/** Tests for [StarModifier]. */
class StarModifierTest {
  @Test
  fun `do nothing`() {
    var star = MutableStar.from(makeStar(id=1L))
    val starModifier = makeStarModifier()
    starModifier.modifyStar(star, StarModification(type = StarModification.MODIFICATION_TYPE.))
  }

  private fun makeStarModifier(): StarModifier {
    return StarModifier { 1L }
  }
}