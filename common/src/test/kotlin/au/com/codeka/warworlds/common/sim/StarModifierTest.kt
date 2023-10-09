package au.com.codeka.warworlds.common.sim

import au.com.codeka.warworlds.common.proto.Design
import au.com.codeka.warworlds.common.proto.StarModification
import io.kotest.matchers.shouldBe
import kotlin.test.assertFailsWith
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
    val star = MutableStar.from(makeStar(id=1L, planets = listOf(makePlanet(0), makePlanet(1))))
    val starModifier = makeStarModifier()

    val exception = assertFailsWith<ErrorWhileSimulatingException>(
      block = {
        starModifier.modifyStar(
          star, StarModification(
            type = StarModification.Type.COLONIZE, empire_id = 1L, planet_index = 1))
      })
    exception.message shouldBe "no colonyship, cannot colonize"
  }

  @Test
  fun colonize() {
    val star = MutableStar.from(makeStar(
      id=1L, planets = listOf(makePlanet(0), makePlanet(1)),
      fleets = listOf(makeFleet(1, design_type = Design.DesignType.COLONY_SHIP, num_ships = 1.0f))))
    val starModifier = makeStarModifier()

    starModifier.modifyStar(
      star, StarModification(
        type = StarModification.Type.COLONIZE, empire_id = 1L, planet_index = 1))
  }

  private fun makeStarModifier(): StarModifier {
    return StarModifier({ 1L }, ExceptionLogHandler())
  }
}
