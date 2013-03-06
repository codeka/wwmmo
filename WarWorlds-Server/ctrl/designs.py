"""designs.py: Contains ship and building design details"""

from datetime import timedelta
import logging
import os
from xml.etree import ElementTree as ET

import ctrl
from protobufs import messages_pb2 as pb


class Design(object):
  def __init__(self):
    self.effects = []
    self.dependencies = []
    self.upgrades = []

  def getEffects(self, kind=None, level=1):
    """Gets the effects of the given kind, or an empty list if there's none."""
    all_effects = self.effects
    if level > 1:
      all_effects = self.upgrades[level - 2].effects

    if not kind:
      return all_effects
    return (effect for effect in all_effects if (effect.kind == kind))

  def hasEffect(self, kind, level=1):
    for _ in self.getEffects(kind, level):
      return True
    return False

  def getDependencies(self, level=1):
    dependencies = self.dependencies
    if level > 1:
      dependencies = self.upgrades[level - 2].dependencies
    return dependencies

  @staticmethod
  def getDesign(kind, name):
    if kind == pb.BuildRequest.BUILDING:
      return BuildingDesign.getDesign(name)
    else:
      return ShipDesign.getDesign(name)


class DesignDependency(object):
  def __init__(self, designID, level):
    self.designID = designID
    self.level = level


class Effect(object):
  """Base class for BuildingEffect and ShipEffect."""
  def __init__(self, kind):
    self.kind = kind


class BuildingEffect(Effect):
  """A BuildingEffect add certain bonuses and what-not to a star."""
  def __init__(self, kind):
    super(BuildingEffect, self).__init__(kind)
    self.level = None

  def applyToColony(self, building_pb, colony_pb):
    """Applies this effect to the given colony."""
    pass

  def applyToEmpirePresence(self, building_pb, empire_presence_pb):
    """Applies this effect to the given empire presence."""
    pass


class BuildingEffectStorage(BuildingEffect):
  """A BuildingEffectStorage adjusts the star's total available storage for minerals and goods."""
  def __init__(self, kind, effectXml):
    super(BuildingEffectStorage, self).__init__(kind)
    self.goods = int(effectXml.get("goods"))
    self.minerals = int(effectXml.get("minerals"))

  def applyToEmpirePresence(self, building_pb, empire_presence_pb):
    empire_presence_pb.max_goods += self.goods
    empire_presence_pb.max_minerals += self.minerals


class BuildingEffectDefence(BuildingEffect):
  """A building effect that boosts a colonies defence capabilities."""
  def __init__(self, kind, effectXml):
    self.defence_bonus = float(effectXml.get("bonus"))

  def applyToColony(self, building_pb, colony_pb):
    colony_pb.defence_bonus += self.defence_bonus


class BuildingEffectPopulationBoost(BuildingEffect):
  """A building effect that boosts a colony's maximum population."""
  def __init__(self, kind, effectXml):
    self.population_boost = float(effectXml.get("boost"))
    self.min = int(effectXml.get("min"))

  def applyToColony(self, building_pb, colony_pb):
    extra_population = colony_pb.max_population * self.population_boost
    if extra_population < self.min:
      extra_population = self.min
    colony_pb.max_population += extra_population


class Upgrade(object):
  """Upgrades can be applied to building to change their effects and so on."""
  def __init__(self, upgradeXml):
    effectsXml = upgradeXml.find("effects")
    if effectsXml is not None:
      self.effects = _parseBuildingEffects(effectsXml)
    else:
      self.effects = []

    costXml = upgradeXml.find("cost")
    self.buildTimeSeconds = float(costXml.get("time")) * 3600
    self.buildCostMinerals = float(costXml.get("minerals"))

    dependenciesXml = upgradeXml.find("dependencies")
    if dependenciesXml is not None:
      self.dependencies = _parseDependencies(dependenciesXml)
    else:
      self.dependencies = []


class BuildingDesign(Design):
  _parsedDesigns = None

  @staticmethod
  def getDesigns():
    """Gets all of the building designs, which we populate from the data/buildings.xml file."""

    if not BuildingDesign._parsedDesigns:
      BuildingDesign._parsedDesigns = _parseBuildingDesigns()
    return BuildingDesign._parsedDesigns

  @staticmethod
  def getDesign(designId):
    """Gets the design with the given ID, or None if none exists."""

    designs = BuildingDesign.getDesigns()
    if designId not in designs:
      return None
    return designs[designId]


class ShipEffect(Effect):
  """A ShipEffect add certain bonuses and what-not to a ship."""
  def __init__(self, kind):
    super(ShipEffect, self).__init__(kind)

  def onStarLanded(self, fleet_pb, star_pb, sim):
    """This is called when a fleet with this effect "lands" on a star."""
    pass

  def onFleetArrived(self, star_pb, fleet_pb, new_fleet_pb, sim):
    """This is called when we're orbiting a star, and a new fleet arrives."""
    pass

  def onAttacked(self, star_pb, fleet_pb, attacker_fleet_pb, sim):
    """This is called when another fleet attacks us."""
    pass


class ShipEffectScout(ShipEffect):
  """The scout effect will generate a scout report every time the ship reaches a star."""
  def __init__(self, kind, effectXml):
    super(ShipEffectScout, self).__init__(kind)

  def onStarLanded(self, fleet_pb, star_pb, sim):
    """This is called when a fleet with this effect "lands" on a star."""
    logging.info("Generating scout report.... star=%s (# planets=%d)" % (star_pb.name, len(star_pb.planets)))
    scout_report_pb = pb.ScoutReport()
    scout_report_pb.empire_key = fleet_pb.empire_key
    scout_report_pb.star_key = star_pb.key
    scout_report_pb.star_pb = star_pb.SerializeToString()
    scout_report_pb.date = ctrl.dateTimeToEpoch(sim.now)
    sim.addScoutReport(scout_report_pb)


class ShipEffectFighter(ShipEffect):
  """This fighter effect is for any ship that allows for fighting (which is most of them)"""
  def __init__(self, kind, effectXml):
    super(ShipEffectFighter, self).__init__(kind)

  def onStarLanded(self, fleet_pb, star_pb, sim):
    """This is called when a fleet with this effect "lands" on a star.

    We want to check if there's any other fleets already on the star that we can attack."""

    if fleet_pb.stance != pb.Fleet.AGGRESSIVE:
      return

    for other_fleet_pb in star_pb.fleets:
      if other_fleet_pb.empire_key != fleet_pb.empire_key:
        logging.debug("We landed at this star and now we're going to attack fleet %s" %
                      other_fleet_pb.key)

        fleet_pb.state = pb.Fleet.ATTACKING
        # Note: we don't set the target here, we'll let the simulation do that so that it's
        # recorded in the combat report
        #fleet_pb.target_fleet_key = other_fleet_pb.key
        fleet_pb.state_start_time = ctrl.dateTimeToEpoch(sim.now)
        break


  def onFleetArrived(self, star_pb, new_fleet_pb, fleet_pb, sim):
    """Called when a new fleet arrives at the star we're orbiting.

    If the new fleet is an enemy (i.e. a different empire) and we're in aggressive mode, then
    we'll want to attack it. Change into attack mode and simulate."""
    if new_fleet_pb.empire_key == fleet_pb.empire_key:
      return

    if fleet_pb.stance != pb.Fleet.AGGRESSIVE:
      return

    if fleet_pb.state != pb.Fleet.IDLE:
      return

    logging.debug("A fleet (%s) has arrived, and we're going to attack it!" % new_fleet_pb.key)

    for star_fleet_pb in star_pb.fleets:
      if star_fleet_pb.key == fleet_pb.key:
        star_fleet_pb.state = pb.Fleet.ATTACKING
        # Note: we don't set the target here, we'll let the simulation do that so that it's
        # recorded in the combat report
        #fleet_pb.target_fleet_key = other_fleet_pb.key
        star_fleet_pb.state_start_time = ctrl.dateTimeToEpoch(sim.now - timedelta(seconds=1))
        break


  def onAttacked(self, star_pb, fleet_pb, attacker_fleet_pb, sim):
    """If another fleet attacks us, then we'll attack back (but only if we're idle."""
    if fleet_pb.state != pb.Fleet.IDLE:
      return

    logging.debug("A fleet (%s) is attacking us, fight back!" % (attacker_fleet_pb.key))

    for star_fleet_pb in star_pb.fleets:
      if star_fleet_pb.key == fleet_pb.key:
        star_fleet_pb.state = pb.Fleet.ATTACKING
        star_fleet_pb.target_fleet_key = attacker_fleet_pb.key
        star_fleet_pb.state_start_time = ctrl.dateTimeToEpoch(sim.now - timedelta(seconds=1))
        break


class ShipEffectTroopCarrier(ShipEffect):
  """The troop carrier effect means this ship carriers troops that can be used
     to attack colonies."""
  def __init__(self, kind, effectXml):
    super(ShipEffectTroopCarrier, self).__init__(kind)


class ShipDesign(Design):
  _parsedDesigns = None

  @staticmethod
  def getDesigns():
    """Gets all of the ship designs, which we populate from the data/ships.xml file."""

    if not ShipDesign._parsedDesigns:
      ShipDesign._parsedDesigns = _parseShipDesigns()
    return ShipDesign._parsedDesigns

  @staticmethod
  def getDesign(designId):
    """Gets the design with the given ID, or None if none exists."""

    designs = ShipDesign.getDesigns()
    if designId not in designs:
      return None
    return designs[designId]


def _parseBuildingDesigns():
  """Parses the /data/buildings.xml file and returns a list of BuildingDesign objects."""

  filename = os.path.join(os.path.dirname(__file__), "../data/buildings.xml")
  logging.debug("Parsing %s" % (filename))
  designs = {}
  xml = ET.parse(filename)
  for designXml in xml.iterfind("design"):
    design = _parseBuildingDesign(designXml)
    designs[design.id] = design
  return designs


def _parseShipDesigns():
  """Parses the /data/ships.xml file and returns a list of ShipDesign objects."""

  filename = os.path.join(os.path.dirname(__file__), "../data/ships.xml")
  logging.debug("Parsing %s" % (filename))
  designs = {}
  xml = ET.parse(filename)
  for designXml in xml.iterfind("design"):
    design = _parseShipDesign(designXml)
    designs[design.id] = design
  return designs


def _parseBuildingEffects(effectsXml):
  effects = []
  for effectXml in effectsXml.iterfind("effect"):
    kind = effectXml.get("kind")
    if kind == "storage":
      effect = BuildingEffectStorage(kind, effectXml)
    elif kind == "defence":
      effect = BuildingEffectDefence(kind, effectXml)
    elif kind == "populationBoost":
      effect = BuildingEffectPopulationBoost(kind, effectXml)
    effects.append(effect)
  return effects


def _parseBuildingDesign(designXml):
  """Parses a single <design> from the buildings.xml file."""

  design = BuildingDesign()
  logging.debug("Parsing building <design id=\"%s\">" % (designXml.get("id")))
  _parseDesign(designXml, design)
  effectsXml = designXml.find("effects")
  if effectsXml is not None:
    design.effects = _parseBuildingEffects(effectsXml)
  design.maxPerColony = 0
  design.maxPerEmpire = 0
  limitsXml = designXml.find("limits")
  if limitsXml is not None:
    maxPerColony = limitsXml.get("maxPerColony")
    if maxPerColony:
      design.maxPerColony = int(maxPerColony)
    maxPerEmpire = limitsXml.get("maxPerEmpire")
    if maxPerEmpire:
      design.maxPerEmpire = int(maxPerEmpire)
  upgradesXml = designXml.find("upgrades")
  if upgradesXml is not None:
    for upgradeXml in upgradesXml.iterfind("upgrade"):
      design.upgrades.append(Upgrade(upgradeXml))
  return design


def _parseShipDesign(designXml):
  """Parses a single <design> from the ships.xml file."""

  design = ShipDesign()
  logging.debug("Parsing ship <design id=\"%s\">" % (designXml.get("id")))
  _parseDesign(designXml, design)
  statsXml = designXml.find("stats")
  design.speed = float(statsXml.get("speed"))
  design.baseAttack = float(statsXml.get("baseAttack"))
  design.baseDefence = float(statsXml.get("baseDefence"))
  fuelXml = designXml.find("fuel")
  design.fuelCostPerParsec = float(fuelXml.get("costPerParsec"))
  effectsXml = designXml.find("effects")
  if effectsXml is not None:
    for effectXml in effectsXml.iterfind("effect"):
      kind = effectXml.get("kind")
      if kind == "scout":
        effect = ShipEffectScout(kind, effectXml)
      elif kind == "fighter":
        effect = ShipEffectFighter(kind, effectXml)
      elif kind == "troopcarrier":
        effect = ShipEffectTroopCarrier(kind, effectXml)
      design.effects.append(effect)
  return design


def _parseDependencies(dependenciesXml):
  dependencies = []
  for requiresXml in dependenciesXml.iterfind("requires"):
    designID = requiresXml.get("building")
    level = requiresXml.get("level")
    dep = DesignDependency(designID, level)
    dependencies.append(dep)
  return dependencies


def _parseDesign(designXml, design):
  design.id = designXml.get("id")
  design.name = designXml.findtext("name")
  design.description = designXml.findtext("description")
  design.icon = designXml.findtext("icon")
  costXml = designXml.find("cost")
  design.buildTimeSeconds = float(costXml.get("time")) * 3600
  design.buildCostMinerals = float(costXml.get("minerals"))
  dependenciesXml = designXml.find("dependencies")
  if dependenciesXml != None:
    design.dependencies = _parseDependencies(dependenciesXml)
