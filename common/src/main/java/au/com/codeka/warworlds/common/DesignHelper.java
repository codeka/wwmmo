package au.com.codeka.warworlds.common;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Designs;

/**
 * Helper class for working with ship and building designs.
 */
public class DesignHelper {

  public static String getDesignName(Design design, boolean plural) {
    return design.display_name + (plural ? "s" : "");
  }

  /** Gets the {@link Design} with the given identifier. */
  public static Design getDesign(String id) {
    for (Design design : designs.designs) {
      if (design.id.equals(id)) {
        return design;
      }
    }

    throw new IllegalStateException("No design with id=" + id + " found.");
  }

  /** The list of all designs in the game. */
  public static final Designs designs = new Designs.Builder().designs(Lists.newArrayList(
      new Design.Builder()
          .id("colony")
          .design_kind(Design.DesignKind.SHIP)
          .display_name("Colony Ship")
          .description(
              "<p>The colony ship is what you'll need to colonize remote planets. They'll carry 100"
              + " of your colony's population to a brave new world.</p>"
              + " <p>Colony ships are single-use. The ship is destroyed once a planet is"
              + " colonized.</p>")
          .dependencies(Lists.newArrayList(
              new Design.Dependency.Builder().id("shipyard").level(1).build()))
          .build_cost(new Design.BuildCost.Builder()
              .minerals(1000)
              .population(10000)
              .max_count(100)
              .build())
          .base_attack(1.0f)
          .base_defence(1.0f)
          .combat_priority(100)
          .effect(Lists.newArrayList(
              new Design.Effect.Builder().name("fighter").build()))
          .fuel_cost_per_px(35.0f)
          .image_url("colony.png")
          .speed_px_per_hour(32.0f)
          .upgrades(Lists.newArrayList(
              new Design.Upgrade.Builder()
                  .id("cryogenics")
                  .display_name("Cryogenic Chamber")
                  .description(
                      "<p>The cryogenic chamber means colonists are put to sleep during the voyage"
                      + " to the next planet. This allows for a larger number of colonists to be"
                      + " deployed, increasing the number of initial colonists from 100 to"
                      + " 400.</p>")
                  .build_cost(new Design.BuildCost.Builder()
                      .minerals(2000)
                      .population(20000)
                      .build())
                  .dependencies(Lists.newArrayList(
                      new Design.Dependency.Builder().id("shipyard").level(1).build()))
                  .image_url("cryogenics.png")
                  .build()
          ))
          .build(),

      new Design.Builder()
          .id("scout")
          .design_kind(Design.DesignKind.SHIP)
          .display_name("Scout")
          .description(
              "<p>Scouts are small, fast ships with no attack capability (and not much in the way"
              + " of defensive capabilities, either). What they <em>are</em> good at, though, is"
              + " getting in and out of enemy star-systems and reporting back what they found.</p>")
          .dependencies(Lists.newArrayList(
              new Design.Dependency.Builder().id("shipyard").level(1).build()))
          .build_cost(new Design.BuildCost.Builder()
              .minerals(20)
              .population(50)
              .max_count(10000)
              .build())
          .base_attack(1.0f)
          .base_defence(1.0f)
          .combat_priority(100)
          .effect(Lists.newArrayList(
              new Design.Effect.Builder().name("scout").build(),
              new Design.Effect.Builder().name("fighter").build()))
          .fuel_cost_per_px(4.0f)
          .image_url("scout.png")
          .speed_px_per_hour(1024.0f)
          .upgrades(Lists.newArrayList(
              new Design.Upgrade.Builder()
                  .id("cloak")
                  .display_name("Cloak")
                  .description(
                      "<p>Adding a cloaking device to a scout makes it invisible to enemy radar."
                      + " Additionally, if you set the scout's stance to \"Neutral\" or"
                      + " \"Passive\", it will remain <em>undetected</em> around enemy stars.</p>")
                  .build_cost(new Design.BuildCost.Builder()
                      .minerals(20)
                      .population(50)
                      .build())
                  .dependencies(Lists.newArrayList(
                      new Design.Dependency.Builder().id("shipyard").level(1).build()))
                  .image_url("cloak.png")
                  .build()
          ))
          .build(),

      new Design.Builder()
          .id("fighter")
          .design_kind(Design.DesignKind.SHIP)
          .display_name("Fighter")
          .description(
              "<p>Your basic fighter has average speed, low-grade weapons and low-grade shields."
              + " They make up for a general lack of firepower by being incredibly cheap to produce"
              + " and fuel-efficient, allowing you to overwhelm your enemy with numbers.</p>")
          .dependencies(Lists.newArrayList(
              new Design.Dependency.Builder().id("shipyard").level(1).build()))
          .build_cost(new Design.BuildCost.Builder()
              .minerals(50)
              .population(100)
              .max_count(50000)
              .build())
          .base_attack(5.0f)
          .base_defence(15.0f)
          .combat_priority(10)
          .effect(Lists.newArrayList(
              new Design.Effect.Builder().name("fighter").build()))
          .fuel_cost_per_px(16.0f)
          .image_url("fighter.png")
          .speed_px_per_hour(128.0f)
          .upgrades(Lists.newArrayList(
              new Design.Upgrade.Builder()
                  .id("boost")
                  .display_name("Boost")
                  .description(
                      "<p>Boost will halve the remaining travel-time for an in-flight fleet of"
                      + "fighters.</p>")
                  .build_cost(new Design.BuildCost.Builder()
                      .minerals(50)
                      .population(100)
                      .build())
                  .dependencies(Lists.newArrayList(
                      new Design.Dependency.Builder().id("shipyard").level(1).build()))
                  .image_url("boost.png")
                  .build()
          ))
          .build(),

      new Design.Builder()
          .id("troopcarrier")
          .design_kind(Design.DesignKind.SHIP)
          .display_name("Troop Carrier")
          .description(
              "<p>The Troop Carrier carries ground troops which you can deploy to capture an enemy"
              + " colony.</p>")
          .dependencies(Lists.newArrayList(
              new Design.Dependency.Builder().id("shipyard").level(1).build()))
          .build_cost(new Design.BuildCost.Builder()
              .minerals(50)
              .population(100)
              .max_count(50000)
              .build())
          .base_attack(1.0f)
          .base_defence(1.0f)
          .combat_priority(50)
          .effect(Lists.newArrayList(
              new Design.Effect.Builder().name("troopcarrier").build(),
              new Design.Effect.Builder().name("fighter").build()))
          .fuel_cost_per_px(16.0f)
          .image_url("troopcarrier.png")
          .speed_px_per_hour(128.0f)
          .build(),

      new Design.Builder()
          .id("wormhole-generator")
          .design_kind(Design.DesignKind.SHIP)
          .display_name("Wormhole Generator")
          .description(
              "<p>The Wormhole Generator will - quite literally - generate a wormhole. Once the"
              + " ship is built, select a location on the starfield to deploy, and the ship will"
              + " generate a wormhole which you can then use to instantly transport ships"
              + " throughout your empire.</p>")
          .dependencies(Lists.newArrayList(
              new Design.Dependency.Builder().id("shipyard").level(2).build()))
          .build_cost(new Design.BuildCost.Builder()
              .minerals(98000)
              .population(50000)
              .max_count(1)
              .build())
          .base_attack(1.0f)
          .base_defence(1.0f)
          .combat_priority(100)
          .effect(Lists.newArrayList(
              new Design.Effect.Builder().name("empty-space-mover").build(),
              new Design.Effect.Builder().name("fighter").build(),
              new Design.Effect.Builder().name("wormhole-generator").build()))
          .fuel_cost_per_px(512.0f)
          .image_url("wormhole-generator.png")
          .speed_px_per_hour(8.0f)
          .build()
  )).build();
}
