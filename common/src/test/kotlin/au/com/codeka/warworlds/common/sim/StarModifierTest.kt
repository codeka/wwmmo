package au.com.codeka.warworlds.common.sim

import au.com.codeka.warworlds.common.proto.StarModification
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** Tests for [StarModifier]. */
class StarModifierTest {
  @Test
  fun `do nothing`() {
    val star = MutableStar.from(makeStar(id=1L))
    val starModifier = makeStarModifier()
    starModifier.modifyStar(star, StarModification(type = StarModification.Type.EMPTY_NATIVE))
    star.id shouldBe 1L
    star.planets.size shouldBe 0
  }

  private fun makeStarModifier(): StarModifier {
    return StarModifier { 1L }
  }
}