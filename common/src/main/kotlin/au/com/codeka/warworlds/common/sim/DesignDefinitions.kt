package au.com.codeka.warworlds.common.sim

import au.com.codeka.warworlds.common.proto.Design
import au.com.codeka.warworlds.common.proto.Designs
import com.google.common.collect.Lists

/** Holder class that just holds all the design definitions.  */
object DesignDefinitions {
  /** The list of all designs in the game.  */
  val designs: Designs = Designs.Builder().designs(Lists.newArrayList(
      Design.Builder()
          .type(Design.DesignType.COLONY_SHIP)
          .design_kind(Design.DesignKind.SHIP)
          .display_name("Colony Ship")
          .description(
              "<p>The colony ship is what you'll need to colonize remote planets. They'll carry 100"
                  + " of your colony's population to a brave new world.</p>"
                  + " <p>Colony ships are single-use. The ship is destroyed once a planet is"
                  + " colonized.</p>")
          .dependencies(Lists.newArrayList(
              Design.Dependency.Builder().type(Design.DesignType.SHIPYARD).level(1).build()))
          .build_cost(Design.BuildCost.Builder()
              .minerals(100f)
              .population(10f)
              .max_count(100)
              .build())
          .base_attack(1.0f)
          .base_defence(1.0f)
          .combat_priority(100)
          .effect(Lists.newArrayList(
              Design.Effect.Builder().type(Design.EffectType.FIGHTER_SHIP).build()))
          .fuel_cost_per_px(2.5f)
          .fuel_size(700)
          .image_url("colony.png")
          .speed_px_per_hour(256.0f)
          .upgrades(Lists.newArrayList(
              Design.Upgrade.Builder()
                  .type(Design.UpgradeType.CRYOGENICS)
                  .display_name("Cryogenic Chamber")
                  .description(
                      "<p>The cryogenic chamber means colonists are put to sleep during the voyage"
                          + " to the next planet. This allows for a larger number of colonists to"
                          + " be deployed, increasing the number of initial colonists from 100 to"
                          + " 400.</p>")
                  .build_cost(Design.BuildCost.Builder()
                      .minerals(2000f)
                      .population(20f)
                      .build())
                  .dependencies(Lists.newArrayList(
                      Design.Dependency.Builder()
                          .type(Design.DesignType.SHIPYARD)
                          .level(1)
                          .build()))
                  .image_url("cryogenics.png")
                  .build()
          ))
          .build(),
      Design.Builder()
          .type(Design.DesignType.SCOUT)
          .design_kind(Design.DesignKind.SHIP)
          .display_name("Scout")
          .description(
              "<p>Scouts are small, fast ships with no attack capability (and not much in the way"
                  + " of defensive capabilities, either). What they <em>are</em> good at, though,"
                  + " is getting in and out of enemy star-systems and reporting back what they"
                  + " found.</p>")
          .dependencies(Lists.newArrayList(
              Design.Dependency.Builder().type(Design.DesignType.SHIPYARD).level(1).build()))
          .build_cost(Design.BuildCost.Builder()
              .minerals(2f)
              .population(1f)
              .max_count(10000)
              .build())
          .base_attack(0.1f)
          .base_defence(0.1f)
          .combat_priority(100)
          .effect(Lists.newArrayList(
              Design.Effect.Builder().type(Design.EffectType.SCOUT_SHIP).build(),
              Design.Effect.Builder().type(Design.EffectType.FIGHTER_SHIP).build()))
          .fuel_cost_per_px(0.02f)
          .fuel_size(100)
          .image_url("scout.png")
          .speed_px_per_hour(8192.0f)
          .upgrades(Lists.newArrayList(
              Design.Upgrade.Builder()
                  .type(Design.UpgradeType.CLOAK)
                  .display_name("Cloak")
                  .description(
                      "<p>Adding a cloaking device to a scout makes it invisible to enemy radar."
                          + " Additionally, if you set the scout's stance to \"Neutral\" or"
                          + " \"Passive\", it will remain <em>undetected</em> around enemy"
                          + " stars.</p>")
                  .build_cost(Design.BuildCost.Builder()
                      .minerals(20f)
                      .population(1f)
                      .build())
                  .dependencies(Lists.newArrayList(
                      Design.Dependency.Builder()
                          .type(Design.DesignType.SHIPYARD)
                          .level(1)
                          .build()))
                  .image_url("cloak.png")
                  .build()
          ))
          .build(),
      Design.Builder()
          .type(Design.DesignType.FIGHTER)
          .design_kind(Design.DesignKind.SHIP)
          .display_name("Fighter")
          .description(
              "<p>Your basic fighter has average speed, low-grade weapons and low-grade shields."
                  + " They make up for a general lack of firepower by being incredibly cheap to"
                  + " produce and fuel-efficient, allowing you to overwhelm your enemy with"
                  + " numbers.</p>")
          .dependencies(Lists.newArrayList(
              Design.Dependency.Builder().type(Design.DesignType.SHIPYARD).level(1).build()))
          .build_cost(Design.BuildCost.Builder()
              .minerals(5f)
              .population(0.1f)
              .max_count(50000)
              .build())
          .base_attack(5.0f)
          .base_defence(15.0f)
          .combat_priority(10)
          .effect(Lists.newArrayList(
              Design.Effect.Builder().type(Design.EffectType.FIGHTER_SHIP).build()))
          .fuel_cost_per_px(0.005f)
          .fuel_size(2)
          .image_url("fighter.png")
          .speed_px_per_hour(1536.0f)
          .upgrades(Lists.newArrayList(
              Design.Upgrade.Builder()
                  .type(Design.UpgradeType.BOOST)
                  .display_name("Boost")
                  .description("<p>Boost will halve the remaining travel-time for an in-flight"
                      + " fleet of fighters.</p>")
                  .build_cost(Design.BuildCost.Builder()
                      .minerals(50f)
                      .population(0.1f)
                      .build())
                  .dependencies(Lists.newArrayList(
                      Design.Dependency.Builder()
                          .type(Design.DesignType.SHIPYARD)
                          .level(1)
                          .build()))
                  .image_url("boost.png")
                  .build()
          ))
          .build(),
      Design.Builder()
          .type(Design.DesignType.TROOP_CARRIER)
          .design_kind(Design.DesignKind.SHIP)
          .display_name("Troop Carrier")
          .description("<p>The Troop Carrier carries ground troops which you can deploy to capture"
              + " an enemy colony.</p>")
          .dependencies(Lists.newArrayList(
              Design.Dependency.Builder().type(Design.DesignType.SHIPYARD).level(1).build()))
          .build_cost(Design.BuildCost.Builder()
              .minerals(5f)
              .population(0.1f)
              .max_count(50000)
              .build())
          .base_attack(1.0f)
          .base_defence(1.0f)
          .combat_priority(50)
          .effect(Lists.newArrayList(
              Design.Effect.Builder().type(Design.EffectType.FIGHTER_SHIP).build()))
          .fuel_cost_per_px(0.005f)
          .fuel_size(2)
          .image_url("troopcarrier.png")
          .speed_px_per_hour(1200.0f)
          .build(),
      Design.Builder()
          .type(Design.DesignType.TANKER)
          .design_kind(Design.DesignKind.SHIP)
          .display_name("Tanker")
          .description(
              "<p>The Tanker will transport a large amount of energy from one star to another,"
                  + " where you might have waiting ships but no infrastructure set up to refuel"
                  + " them.</p>")
          .dependencies(Lists.newArrayList(
              Design.Dependency.Builder().type(Design.DesignType.SHIPYARD).level(1).build()))
          .build_cost(Design.BuildCost.Builder()
              .minerals(50f)
              .population(2f)
              .max_count(500)
              .build())
          .base_attack(0.1f)
          .base_defence(1.0f)
          .combat_priority(50)
          .effect(Lists.newArrayList(
              Design.Effect.Builder().type(Design.EffectType.ENERGY_TRANSPORT).build()))
          .fuel_cost_per_px(0.005f)
          .fuel_size(250)
          .image_url("tanker.png")
          .speed_px_per_hour(1432.0f)
          .build(),
      Design.Builder()
          .type(Design.DesignType.WORMHOLE_GENERATOR)
          .design_kind(Design.DesignKind.SHIP)
          .display_name("Wormhole Generator")
          .description(
              "<p>The Wormhole Generator will - quite literally - generate a wormhole. Once the"
                  + " ship is built, select a location on the starfield to deploy, and the ship"
                  + " will generate a wormhole which you can then use to instantly transport ships"
                  + " throughout your empire.</p>")
          .dependencies(Lists.newArrayList(
              Design.Dependency.Builder().type(Design.DesignType.SHIPYARD).level(2).build()))
          .build_cost(Design.BuildCost.Builder()
              .minerals(9800f)
              .population(5000f)
              .max_count(1)
              .build())
          .base_attack(1.0f)
          .base_defence(1.0f)
          .combat_priority(100)
          .effect(Lists.newArrayList(
              Design.Effect.Builder().type(Design.EffectType.EMPTY_SPACE_MOVER).build(),
              Design.Effect.Builder().type(Design.EffectType.FIGHTER_SHIP).build()))
          .fuel_cost_per_px(51.2f)
          .fuel_size(450)
          .image_url("wormhole-generator.png")
          .speed_px_per_hour(128.0f)
          .build(),

      //--------------------------------------------------------------------------------------------
      Design.Builder()
          .type(Design.DesignType.SHIPYARD)
          .design_kind(Design.DesignKind.BUILDING)
          .display_name("Shipyard")
          .description(
              "<p>The shipyard allows you to build ships.</p>"
                  + " <p>You can have only one shipyard on your planet, but it's efficiency and the"
                  + " range of ships available to you can be upgraded with the building of"
                  + " supporting buildings.</p>")
          .build_cost(Design.BuildCost.Builder()
              .minerals(100f)
              .population(25f)
              .build())
          .image_url("shipyard.png")
          .max_per_colony(1)
          .upgrades(Lists.newArrayList(
              Design.Upgrade.Builder()
                  .build_cost(Design.BuildCost.Builder()
                      .minerals(500f)
                      .population(500f)
                      .build())
                  .dependencies(Lists.newArrayList(
                      Design.Dependency.Builder()
                          .type(Design.DesignType.RESEARCH)
                          .level(1)
                          .build()))
                  .build()
          ))
          .show_in_solar_system(true)
          .build(),
      Design.Builder()
          .type(Design.DesignType.SILO)
          .design_kind(Design.DesignKind.BUILDING)
          .display_name("Silo")
          .description(
              "<p>The Silo allows you to store farming produce and minerals indefinitely. The"
                  + " higher the silo level, the more you can store. Silos store produce for all"
                  + " colonies in this starsystem and you can build as many as you like.</p>"
                  + "<p>A level 1 Silo adds 100 storage for minerals and goods. A level 2 Silo adds"
                  + " 210, level 3 adds 330, a level 4 Silo adds 460, and a level 5 Silo adds"
                  + " 600.</p>")
          .build_cost(Design.BuildCost.Builder()
              .minerals(100f)
              .population(25f)
              .build())
          .image_url("silo.png")
          .effect(Lists.newArrayList(
              Design.Effect.Builder()
                  .type(Design.EffectType.STORAGE)
                  .goods(100)
                  .minerals(1000)
                  .energy(1000)
                  .build()
          ))
          .upgrades(Lists.newArrayList(
              Design.Upgrade.Builder()
                  .build_cost(Design.BuildCost.Builder()
                      .minerals(200f)
                      .population(200f)
                      .build())
                  .effects(Lists.newArrayList(
                      Design.Effect.Builder()
                          .type(Design.EffectType.STORAGE)
                          .goods(210)
                          .minerals(2100)
                          .energy(2100)
                          .build()
                  ))
                  .build(),
              Design.Upgrade.Builder()
                  .build_cost(Design.BuildCost.Builder()
                      .minerals(200f)
                      .population(200f)
                      .build())
                  .effects(Lists.newArrayList(
                      Design.Effect.Builder()
                          .type(Design.EffectType.STORAGE)
                          .goods(330)
                          .minerals(3300)
                          .energy(3300)
                          .build()
                  ))
                  .build(),
              Design.Upgrade.Builder()
                  .build_cost(Design.BuildCost.Builder()
                      .minerals(200f)
                      .population(200f)
                      .build())
                  .effects(Lists.newArrayList(
                      Design.Effect.Builder()
                          .type(Design.EffectType.STORAGE)
                          .goods(460)
                          .minerals(4600)
                          .energy(4600)
                          .build()
                  ))
                  .build(),
              Design.Upgrade.Builder()
                  .build_cost(Design.BuildCost.Builder()
                      .minerals(200f)
                      .population(200f)
                      .build())
                  .effects(Lists.newArrayList(
                      Design.Effect.Builder()
                          .type(Design.EffectType.STORAGE)
                          .goods(600)
                          .minerals(6000)
                          .energy(6000)
                          .build()
                  ))
                  .build()
          ))
          .show_in_solar_system(true)
          .build(),
      Design.Builder()
          .type(Design.DesignType.RESEARCH)
          .design_kind(Design.DesignKind.BUILDING)
          .display_name("Research Laboratory")
          .description("<p>Allows you to build more advanced ships and buildings. The higher the"
              + " level of this building, the more advanced research becomes available.</p>")
          .build_cost(Design.BuildCost.Builder()
              .minerals(200f)
              .population(250f)
              .build())
          .image_url("research.png")
          .upgrades(Lists.newArrayList(
              Design.Upgrade.Builder()
                  .build_cost(Design.BuildCost.Builder()
                      .minerals(1500f)
                      .population(2000f)
                      .build())
                  .build()
          ))
          .show_in_solar_system(false)
          .max_per_colony(1)
          .build(),
      Design.Builder()
          .type(Design.DesignType.GROUND_SHIELD)
          .design_kind(Design.DesignKind.BUILDING)
          .display_name("Ground Shield")
          .description(
              "<p>The ground shield provide additional defence when an enemy lands a troop carrier."
                  + " Your colony's base defence will be increased by 0.25 for each level of Ground"
                  + " Shield.</p>")
          .build_cost(Design.BuildCost.Builder()
              .minerals(50f)
              .population(100f)
              .build())
          .image_url("groundshield.png")
          .max_per_colony(1)
          .effect(Lists.newArrayList(
              Design.Effect.Builder().type(Design.EffectType.DEFENCE).bonus(0.25f).build()
          ))
          .upgrades(Lists.newArrayList(
              Design.Upgrade.Builder()
                  .build_cost(Design.BuildCost.Builder()
                      .minerals(50f)
                      .population(100f)
                      .build())
                  .effects(Lists.newArrayList(
                      Design.Effect.Builder()
                          .type(Design.EffectType.DEFENCE)
                          .bonus(0.5f)
                          .build()
                  ))
                  .dependencies(Lists.newArrayList(
                      Design.Dependency.Builder()
                          .type(Design.DesignType.RESEARCH)
                          .level(1)
                          .build()))
                  .build(),
              Design.Upgrade.Builder()
                  .build_cost(Design.BuildCost.Builder()
                      .minerals(50f)
                      .population(100f)
                      .build())
                  .effects(Lists.newArrayList(
                      Design.Effect.Builder()
                          .type(Design.EffectType.DEFENCE)
                          .bonus(0.75f)
                          .build()
                  ))
                  .dependencies(Lists.newArrayList(
                      Design.Dependency.Builder()
                          .type(Design.DesignType.RESEARCH)
                          .level(1)
                          .build()))
                  .build(),
              Design.Upgrade.Builder()
                  .build_cost(Design.BuildCost.Builder()
                      .minerals(50f)
                      .population(100f)
                      .build())
                  .effects(Lists.newArrayList(
                      Design.Effect.Builder()
                          .type(Design.EffectType.DEFENCE)
                          .bonus(1.0f)
                          .build()
                  ))
                  .dependencies(Lists.newArrayList(
                      Design.Dependency.Builder()
                          .type(Design.DesignType.RESEARCH)
                          .level(1)
                          .build()))
                  .build(),
              Design.Upgrade.Builder()
                  .build_cost(Design.BuildCost.Builder()
                      .minerals(50f)
                      .population(100f)
                      .build())
                  .effects(Lists.newArrayList(
                      Design.Effect.Builder()
                          .type(Design.EffectType.DEFENCE)
                          .bonus(1.25f)
                          .build()
                  ))
                  .dependencies(Lists.newArrayList(
                      Design.Dependency.Builder()
                          .type(Design.DesignType.RESEARCH)
                          .level(1)
                          .build()))
                  .build()
          ))
          .show_in_solar_system(true)
          .build(),
      Design.Builder()
          .type(Design.DesignType.BIOSPHERE)
          .design_kind(Design.DesignKind.BUILDING)
          .display_name("Biosphere")
          .description(
              "<p>The Biosphere increases a planet's maximum population. Without a biosphere, a"
                  + " planet's maximum population is approximately equal to it's population"
                  + " congeniality. But each level of biosphere adds 100 or 15% (whichever is"
                  + " greater).</p>")
          .build_cost(Design.BuildCost.Builder()
              .minerals(200f)
              .population(150f)
              .build())
          .max_per_colony(1)
          .image_url("biosphere.png")
          .effect(Lists.newArrayList(
              Design.Effect.Builder()
                  .type(Design.EffectType.POPULATION_BOOST)
                  .bonus(0.15f)
                  .minimum(100)
                  .build()
          ))
          .upgrades(Lists.newArrayList(
              Design.Upgrade.Builder()
                  .build_cost(Design.BuildCost.Builder()
                      .minerals(50f)
                      .population(100f)
                      .build())
                  .effects(Lists.newArrayList(
                      Design.Effect.Builder()
                          .type(Design.EffectType.POPULATION_BOOST)
                          .bonus(0.3f)
                          .minimum(200)
                          .build()
                  ))
                  .build(),
              Design.Upgrade.Builder()
                  .build_cost(Design.BuildCost.Builder()
                      .minerals(50f)
                      .population(100f)
                      .build())
                  .effects(Lists.newArrayList(
                      Design.Effect.Builder()
                          .type(Design.EffectType.POPULATION_BOOST)
                          .bonus(0.45f)
                          .minimum(300)
                          .build()
                  ))
                  .build(),
              Design.Upgrade.Builder()
                  .build_cost(Design.BuildCost.Builder()
                      .minerals(50f)
                      .population(100f)
                      .build())
                  .effects(Lists.newArrayList(
                      Design.Effect.Builder()
                          .type(Design.EffectType.POPULATION_BOOST)
                          .bonus(0.6f)
                          .minimum(400)
                          .build()
                  ))
                  .build()
          ))
          .show_in_solar_system(true)
          .build(),
      Design.Builder()
          .type(Design.DesignType.HQ)
          .design_kind(Design.DesignKind.BUILDING)
          .display_name("Galactic HQ")
          .description(
              "<p>Serves as the \"headquarters\" of your Empire. You can only have one HQ across"
                  + " all of your planets and stars.</p>"
                  + "<p>Once you build a Galactic HQ, a beacon will appear on the map screen that"
                  + " always points back to your HQ so that you don't get lost navigating the"
                  + " starfield.</p>")
          .build_cost(Design.BuildCost.Builder()
              .minerals(100000f)
              .population(50000f)
              .build())
          .image_url("hq.png")
          .max_per_empire(1)
          .show_in_solar_system(true)
          .build(),
      Design.Builder()
          .type(Design.DesignType.RADAR)
          .design_kind(Design.DesignKind.BUILDING)
          .display_name("Sensor Array")
          .description(
              "<p>The Sensor Array lets you detect enemy ships (not from native colonies, though)"
                  + " up to 20 parsecs away for 1st level, 30 for 2nd level, 40 for 3rd level and"
                  + " so on. There's no point building more than on per star, though nothing will"
                  + " stop you if you try.</p>"
                  + "<p>A level 5 Sensor Array will send you a notification when it detects enemy"
                  + " ships inbound for your planet.</p>")
          .build_cost(Design.BuildCost.Builder()
              .minerals(100f)
              .population(200f)
              .build())
          .max_per_colony(1)
          .image_url("radar.png")
          .effect(Lists.newArrayList(
              Design.Effect.Builder().type(Design.EffectType.RADAR_SCAN).range(20.0f).build()
          ))
          .upgrades(Lists.newArrayList(
              Design.Upgrade.Builder()
                  .build_cost(Design.BuildCost.Builder()
                      .minerals(100f)
                      .population(200f)
                      .build())
                  .effects(Lists.newArrayList(
                      Design.Effect.Builder()
                          .type(Design.EffectType.RADAR_SCAN)
                          .range(30.0f)
                          .build()
                  ))
                  .build(),
              Design.Upgrade.Builder()
                  .build_cost(Design.BuildCost.Builder()
                      .minerals(100f)
                      .population(200f)
                      .build())
                  .effects(Lists.newArrayList(
                      Design.Effect.Builder()
                          .type(Design.EffectType.RADAR_SCAN)
                          .range(40.0f)
                          .build()
                  ))
                  .build(),
              Design.Upgrade.Builder()
                  .build_cost(Design.BuildCost.Builder()
                      .minerals(100f)
                      .population(200f)
                      .build())
                  .effects(Lists.newArrayList(
                      Design.Effect.Builder()
                          .type(Design.EffectType.RADAR_SCAN)
                          .range(50.0f)
                          .build()
                  ))
                  .dependencies(Lists.newArrayList(
                      Design.Dependency.Builder()
                          .type(Design.DesignType.RESEARCH)
                          .level(1)
                          .build()))
                  .build(),
              Design.Upgrade.Builder()
                  .build_cost(Design.BuildCost.Builder()
                      .minerals(100f)
                      .population(200f)
                      .build())
                  .effects(Lists.newArrayList(
                      Design.Effect.Builder()
                          .type(Design.EffectType.RADAR_SCAN)
                          .range(60.0f)
                          .build(),
                      Design.Effect.Builder()
                          .type(Design.EffectType.RADAR_ALERT)
                          .range(60.0f)
                          .build()
                  ))
                  .dependencies(Lists.newArrayList(
                      Design.Dependency.Builder()
                          .type(Design.DesignType.RESEARCH)
                          .level(1)
                          .build()))
                  .build()
          ))
          .show_in_solar_system(true)
          .build(),
      Design.Builder()
          .type(Design.DesignType.WORMHOLE_DISRUPTOR)
          .design_kind(Design.DesignKind.BUILDING)
          .display_name("Wormhole Disruptor")
          .description(
              "<p>A Wormhole Disruptor is an offensive building used to attack nearby"
                  + " wormholes.</p>"
                  + "<p>Each upgrade of the Wormhole Disruptor will extend it's range, but keep in"
                  + " mind the range will always be quite limited, so be sure to only build a"
                  + " Disruptor on the closest star to the wormhole you can.</p>")
          .build_cost(Design.BuildCost.Builder()
              .minerals(148000f)
              .population(1000f)
              .build())
          .max_per_colony(1)
          .image_url("wormhole-disruptor.png")
          .effect(Lists.newArrayList(
              Design.Effect.Builder()
                  .type(Design.EffectType.WORMHOLE_DISRUPT)
                  .range(9.5f)
                  .build()
          ))
          .dependencies(Lists.newArrayList(
              Design.Dependency.Builder()
                  .type(Design.DesignType.RESEARCH)
                  .level(2)
                  .build()))
          .upgrades(Lists.newArrayList(
              Design.Upgrade.Builder()
                  .build_cost(Design.BuildCost.Builder()
                      .minerals(74000f)
                      .population(500f)
                      .build())
                  .effects(Lists.newArrayList(
                      Design.Effect.Builder()
                          .type(Design.EffectType.WORMHOLE_DISRUPT)
                          .range(14.5f)
                          .build()
                  ))
                  .dependencies(Lists.newArrayList(
                      Design.Dependency.Builder()
                          .type(Design.DesignType.RESEARCH)
                          .level(2)
                          .build()))
                  .build(),
              Design.Upgrade.Builder()
                  .build_cost(Design.BuildCost.Builder()
                      .minerals(74000f)
                      .population(500f)
                      .build())
                  .effects(Lists.newArrayList(
                      Design.Effect.Builder()
                          .type(Design.EffectType.WORMHOLE_DISRUPT)
                          .range(19.5f)
                          .build()
                  ))
                  .dependencies(Lists.newArrayList(
                      Design.Dependency.Builder()
                          .type(Design.DesignType.RESEARCH)
                          .level(2)
                          .build()))
                  .build(),
              Design.Upgrade.Builder()
                  .build_cost(Design.BuildCost.Builder()
                      .minerals(74000f)
                      .population(500f)
                      .build())
                  .effects(Lists.newArrayList(
                      Design.Effect.Builder()
                          .type(Design.EffectType.WORMHOLE_DISRUPT)
                          .range(24.5f)
                          .build()
                  ))
                  .dependencies(Lists.newArrayList(
                      Design.Dependency.Builder()
                          .type(Design.DesignType.RESEARCH)
                          .level(2)
                          .build()))
                  .build(),
              Design.Upgrade.Builder()
                  .build_cost(Design.BuildCost.Builder()
                      .minerals(74000f)
                      .population(500f)
                      .build())
                  .effects(Lists.newArrayList(
                      Design.Effect.Builder()
                          .type(Design.EffectType.WORMHOLE_DISRUPT)
                          .range(29.5f)
                          .build()
                  ))
                  .dependencies(Lists.newArrayList(
                      Design.Dependency.Builder()
                          .type(Design.DesignType.RESEARCH)
                          .level(2)
                          .build()))
                  .build()
          ))
          .show_in_solar_system(true)
          .build()
  )).build()
}