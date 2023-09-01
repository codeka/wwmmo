package au.com.codeka.warworlds.common.sim

import au.com.codeka.warworlds.common.proto.StarModification
import io.kotest.matchers.collections.shouldHaveSize
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

  @Test
  fun `colonize with no ship`() {
    val log = ReportingLogHandler()

    val star = MutableStar.from(makeStar(id=1L, planets = listOf(makePlanet(0), makePlanet(1))))
    val starModifier = makeStarModifier()
    starModifier.modifyStar(star, StarModification(type = StarModification.Type.COLONIZE), logHandler = log)

    log.errors shouldHaveSize 0

    star.id shouldBe 1L
    star.planets.size shouldBe 2

  }

  private fun makeStarModifier(): StarModifier {
    return StarModifier { 1L }
  }
}