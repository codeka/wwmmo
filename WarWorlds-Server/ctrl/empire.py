"""empire.py: Controller for empire-related functions. Aso contains the 'simulate' method."""


import copy
from datetime import datetime, timedelta
import logging
import math
import os
import random
from xml.etree import ElementTree as ET

from google.appengine.ext import db
from google.appengine.api import taskqueue

import ctrl
from ctrl import sector
from ctrl import sectorgen
from model import sector as sector_mdl
from model import empire as mdl
from protobufs import warworlds_pb2 as pb


def getEmpireForUser(user):
  cache_key = "empire:for-user:%s" % (user.email())
  values = ctrl.getCached([cache_key], pb.Empire)
  if cache_key in values:
    return values[cache_key]

  empire_model = mdl.Empire.getForUser(user)
  if not empire_model:
    return None

  empire_pb = pb.Empire()
  ctrl.empireModelToPb(empire_pb, empire_model)
  ctrl.setCached({cache_key: empire_pb})
  return empire_pb


def getEmpire(empire_key):
  cache_key = "empire:%s" % (empire_key)
  values = ctrl.getCached([cache_key], pb.Empire)
  if cache_key in values:
    return values[cache_key]

  empire_model = mdl.Empire.get(empire_key)
  empire_pb = pb.Empire()
  ctrl.empireModelToPb(empire_pb, empire_model)
  ctrl.setCached({cache_key: empire_pb})
  return empire_pb


def createEmpire(empire_pb, sim):
  empire_model = mdl.Empire()
  empire_model.cash = 500.0
  ctrl.empirePbToModel(empire_model, empire_pb)
  empire_model.put()

  # We need to set you up with some initial bits and pieces. First, we need to find
  # sector for your colony. We look for one with no existing colonized stars and
  # close to the centre of the universe. We chose a random one of the five closest
  sector_model = None
  while not sector_model:
    query = sector_mdl.Sector.all().filter("numColonies =", 0).order("distanceToCentre").fetch(5)
    index = random.randint(0, 4)
    sector_model = None
    for s in query:
      if index == 0:
        sector_model = s
        break
      index -= 1
  
    if not sector_model:
      # this would happen if there's no sectors loaded that have no colonies... that's bad!!
      logging.warn("Could not find any sectors for new empire [%s], creating some..." % (
                     str(empire_model.key())))
      sectorgen.expandUniverse(immediate=True)

  # Now find a star within that sector. We'll want one with two terran planets with highish
  # population stats, close to the centre of the sector. We'll score each of the stars based on
  # these factors and then choose the one with the highest score
  starScores = []
  for star_model in sector_mdl.Star.all().filter("sector", sector_model):
    centre = sector.SECTOR_SIZE / 2
    distance_to_centre = math.sqrt((star_model.x - centre) * (star_model.x - centre) +
                                   (star_model.y - centre) * (star_model.y - centre))

    # 0 -- 10 (0 is edge of sector, 10 is centre of sector)
    distance_score = (centre - distance_to_centre) / (centre / 10)

    num_terran_planets = 0
    population_congeniality = 0
    farming_congeniality = 0
    mining_congeniality = 0
    for planet in star_model.planets:
      if planet.planet_type == pb.Planet.TERRAN:
        num_terran_planets += 1
        population_congeniality += planet.population_congeniality
        farming_congeniality += planet.farming_congeniality
        mining_congeniality += planet.mining_congeniality

    planet_score = 0
    if num_terran_planets >= 2:
      planet_score = num_terran_planets

    # if there's no terran planets at all, just ignore this star
    if num_terran_planets == 0:
      continue

    # the average of the congenialities / 100 (should make it approximately 0..10)
    congeniality_score = (population_congeniality / num_terran_planets +
                          farming_congeniality / num_terran_planets +
                          mining_congeniality / num_terran_planets)
    congeniality_score /= 100

    score = distance_score * planet_score * congeniality_score
    starScores.append((score, star_model))

  # just choose the star with the highest score
  (score, star_model) = sorted(starScores, reverse=True)[0]

  # next, choose the planet on this star with the highest population congeniality, that'll
  # be the one we start out on
  max_population_congeniality = 0
  planet_index = 0
  for index,planet in enumerate(star_model.planets):
    if planet.planet_type == pb.Planet.TERRAN:
      if planet.population_congeniality > max_population_congeniality:
        planet_index = index + 1
        max_population_congeniality = planet.population_congeniality

  star_key = str(star_model.key())
  star_pb = sim.getStar(star_key, True)

  # by default, the star will have a bunch of native colonies and fleets... drop those!
  for fleet_pb in star_pb.fleets:
    fleet_mdl = mdl.Fleet.get(fleet_pb.key)
    fleet_mdl.delete()
  del star_pb.fleets[:]
  for colony_pb in star_pb.colonies:
    colony_mdl = mdl.Colony.get(colony_pb.key)
    colony_mdl.delete()
  del star_pb.colonies[:]

  # colonize the planet!
  _colonize(sector_model.key(), empire_model, star_pb, planet_index)

  # add some initial goods and minerals to the colony
  sim.simulate(star_key)
  for empire_presence_pb in star_pb.empires:
    if empire_presence_pb.empire_key == str(empire_model.key()):
      empire_presence_pb.total_goods += 100
      empire_presence_pb.total_minerals += 100

  # give them a colony ship and a couple of scouts for free
  fleet_model = mdl.Fleet(parent=star_model)
  fleet_model.empire = empire_model
  fleet_model.sector = sector_model
  fleet_model.designName = "colonyship"
  fleet_model.numShips = 1.0
  fleet_model.state = pb.Fleet.IDLE
  fleet_model.stateStartTime = datetime.now()
  fleet_model.put()

  fleet_model = mdl.Fleet(parent=star_model)
  fleet_model.empire = empire_model
  fleet_model.sector = sector_model
  fleet_model.designName = "scout"
  fleet_model.numShips = 10.0
  fleet_model.state = pb.Fleet.IDLE
  fleet_model.stateStartTime = datetime.now()
  fleet_model.put()


def getColoniesForEmpire(empire_pb):
  cache_key = "colony:for-empire:%s" % empire_pb.key
  values = ctrl.getCached([cache_key], pb.Colonies)
  if cache_key in values:
    return values[cache_key]

  colony_models = mdl.Colony.getForEmpire(empire_pb.key)
  colonies_pb = pb.Colonies()
  for colony_model in colony_models:
    colony_pb = colonies_pb.colonies.add()
    ctrl.colonyModelToPb(colony_pb, colony_model)

  ctrl.setCached({cache_key: colonies_pb})
  return colonies_pb


def getColony(colony_key):
  cache_key = "colony:%s" % colony_key
  values = ctrl.getCached([cache_key], pb.Colony)
  if cache_key in values:
    return values[cache_key]

  colony_model = mdl.Colony.get(colony_key)
  if colony_model:
    colony_pb = pb.Colony()
    ctrl.colonyModelToPb(colony_pb, colony_model)
    ctrl.setCached({cache_key: colony_pb})
    return colony_pb


def getScoutReports(star_key, empire_key):
  cache_key = "scout-report:%s:%s" % (star_key, empire_key)
  values = ctrl.getCached([cache_key], pb.ScoutReports)
  if cache_key in values:
    return values[cache_key]

  scout_reports_pb = pb.ScoutReports()
  scout_report_mdls = mdl.ScoutReport.getReports(db.Key(star_key), db.Key(empire_key))
  for scout_report_mdl in scout_report_mdls:
    scout_report_pb = scout_reports_pb.reports.add()
    ctrl.scoutReportModelToPb(scout_report_pb, scout_report_mdl)

  # note: we WANT to cache an empty one, if there's none in the data store...
  ctrl.setCached({cache_key: scout_reports_pb})
  return scout_reports_pb


def getCombatReports(star_key):
  """Gets a 'summary' of combat reports for the given star.

  We don't actually return the round data in this call, you can fetch the rounds report-by-report
  by calling getCombatReport()."""
  cache_key = "combat-reports:%s" % (star_key)
  values = ctrl.getCached([cache_key], pb.CombatReports)
  if cache_key in values:
    return values[cache_key]

  combat_reports_pb = pb.CombatReports()
  combat_report_mdls = mdl.CombatReport.getReports(db.Key(star_key))
  for combat_report_mdl in combat_report_mdls:
    combat_report_pb = combat_reports_pb.reports.add()
    ctrl.combatReportModelToPb(combat_report_pb, combat_report_mdl, summary=True)

  ctrl.setCached({cache_key: combat_reports_pb})
  return combat_reports_pb


def getCombatReport(star_key, combat_report_key):
  """Gets a 'summary' of combat reports for the given star.

  We don't actually return the round data in this call, you can fetch the rounds report-by-report
  by calling getCombatReport()."""
  cache_key = "combat-report:%s" % (combat_report_key)
  values = ctrl.getCached([cache_key], pb.CombatReport)
  if cache_key in values:
    return values[cache_key]

  combat_report_pb = pb.CombatReport()
  combat_report_mdl = mdl.CombatReport.get(db.Key(combat_report_key))
  ctrl.combatReportModelToPb(combat_report_pb, combat_report_mdl, summary=False)

  ctrl.setCached({cache_key: combat_report_pb})
  return combat_report_pb


def updateColony(colony_key, updated_colony_pb, sim):
  """Updates the colony with the given colony_key with the new parameters in updated_colony_pb.

  When updating a colony, there's a few things we need to do. For example, we need to simulate
  the colony with it's old parameters to bring it up to date. Then we need to make sure the
  new parameters are valid (e.g. all the focus_* properties add up to 1), then we can save the
  new colony details.

  We don't care about the potential for race conditions here since you can only update the
  parameters for your own colony and we don't expect a single user to be updating the same
  colony at the same time.

  Args:
    colony_key: The key that identifies which colony we're going to update (technically,
        updated_colony_pb should have the same key)
    update_colony_pb: A protobuf with the updated parameters. We don't always update everything,
        just certain things that you can actually change.

  Returns:
    An updated colony protobuf.
  """

  colony_pb = getColony(colony_key)
  sim.simulate(colony_pb.star_key)
  star_pb = sim.getStar(colony_pb.star_key)

  # Make sure we're updating the colony in the star
  for cpb in star_pb.colonies:
    if cpb.key == colony_pb.key:
      colony_pb = cpb
      break

  # normalize the focus values so that they all add up to 1.0
  focus_total = (updated_colony_pb.focus_population +
                 updated_colony_pb.focus_farming +
                 updated_colony_pb.focus_mining +
                 updated_colony_pb.focus_construction)
  colony_pb.focus_population = updated_colony_pb.focus_population / focus_total
  colony_pb.focus_farming = updated_colony_pb.focus_farming / focus_total
  colony_pb.focus_mining = updated_colony_pb.focus_mining / focus_total
  colony_pb.focus_construction = updated_colony_pb.focus_construction / focus_total

  # We need to simulate once more to ensure the new end-time for builds, production rates and
  # whatnot are up to date. Then, because of the simulate, we need to update the colonies
  sim.simulate(star_pb.key)

  return colony_pb


def collectTaxes(colony_key, sim):
  """Transfer the uncollected taxes from the given colony into that colony's empire."""
  colony_pb = getColony(colony_key)
  sim.simulate(colony_pb.star_key)
  star_pb = sim.getStar(colony_pb.star_key)

  empire_pb = getEmpire(colony_pb.empire_key)
  empire_pb.cash += colony_pb.uncollected_taxes

  logging.debug("Collect $%.2f in taxes from colony %s" % (colony_pb.uncollected_taxes, colony_pb.key))

  # reset the uncollected taxes of this colony, but make sure it's the colony_pb that's
  # actually in the star (otherwise updateAfterSimulate don't work!)
  for star_colony_pb in star_pb.colonies:
    if colony_pb.key == star_colony_pb.key:
      star_colony_pb.uncollected_taxes = 0.0

  empire_model = mdl.Empire.get(colony_pb.empire_key)
  empire_model.cash = empire_pb.cash
  empire_model.put() 
  ctrl.clearCached(["empire:%s" % (colony_pb.empire_key),
                    "empire:for-user:%s" % (empire_model.user.email())])


def _colonize(sector_key, empire_model, star_pb, planet_index):
  colony_model = mdl.Colony(parent=db.Key(star_pb.key))
  colony_model.empire = empire_model
  colony_model.planet_index = planet_index
  colony_model.sector = sector_key
  colony_model.population = 100.0
  colony_model.lastSimulation = datetime.now()
  colony_model.focusPopulation = 0.25
  colony_model.focusFarming = 0.25
  colony_model.focusMining = 0.25
  colony_model.focusConstruction = 0.25
  colony_model.put()

  def inc_colony_count():
    sector_model = sector_mdl.Sector.get(sector_key)
    if sector_model.numColonies is None:
      sector_model.numColonies = 1
    else:
      sector_model.numColonies += 1
    sector_model.put()
  db.run_in_transaction(inc_colony_count)

  # clear the cache of the various bits and pieces who are now invalid
  keys = ["sector:%d,%d" % (star_pb.sector_x, star_pb.sector_y),
          "star:%s" % (star_pb.key)]
  if empire_model:
    keys.append("colony:for-empire:%s" % (empire_model.key()))
  ctrl.clearCached(keys)

  return colony_model


def colonize(empire_pb, star_key, colonize_request):
  """Colonizes the planet given in the colonize_request.

  You can't colonize a planet until there's a colony ship that belongs to your empire around
  this star. We'll check for that and return an error if there's no ship. If there is a ship,
  then that ship will be "destroyed" when the colony is created.

  Args:
    empire_pb: The empire protobuf
    star_key: The key of the star you're going to colonize
    colonize_request: a ColonizeRequest protobuf, containing the planet
        and star key of the planet we want to colonize.
  """

  logging.info("Colonizing: Star=%s Planet=%d" % (star_key,
                                                  colonize_request.planet_index))

  star_pb = sector.getStar(star_key)
  if star_pb is None:
    logging.warn("Could not find star with key: %s" % star_key)
    return None

  if len(star_pb.planets) < colonize_request.planet_index:
    logging.warn("colonize_request's planet_index was out of bounds")
    return None

  # find a colony ship we can destroy
  colony_ship_fleet_pb = None
  for fleet_pb in star_pb.fleets:
    if fleet_pb.empire_key != empire_pb.key:
      continue
    if fleet_pb.design_name == "colonyship": # TODO: hard-coded??
      colony_ship_fleet_pb = fleet_pb
      break

  if not colony_ship_fleet_pb:
    logging.warn("colonize_request impossible because there's no colony ship")
    return None

  def destroy_ship(fleet_key):
    fleet_model = mdl.Fleet.get(fleet_key)
    fleet_model.numShips -= 1
    if fleet_model.numShips == 0:
      fleet_model.delete()
    else:
      fleet_model.put()
  db.run_in_transaction(destroy_ship, colony_ship_fleet_pb.key)
  keys = ["fleet:for-empire:%s" % (empire_pb.key),
          "fleet:%s" % (colony_ship_fleet_pb.key)]
  ctrl.clearCached(keys)

  sector_key = sector_mdl.SectorManager.getSectorKey(star_pb.sector_x, star_pb.sector_y)
  empire_model = mdl.Empire.get(empire_pb.key)

  colony_model = _colonize(sector_key, empire_model, star_pb, colonize_request.planet_index)

  colony_pb = pb.Colony()
  ctrl.colonyModelToPb(colony_pb, colony_model)
  return colony_pb


def build(empire_pb, colony_pb, request_pb, sim):
  """Initiates a build operation at the given colony.

  Args:
    empire_pb: The empire that is requesting the build (we assume you've already validated
        the fact that this empire owns the colony)
    colony_pb: The colony where the request has been made.
    request_pb: A BuildRequest protobuf with details of the build request.
    sim: A simulation.Simulation() object
  """

  design = Design.getDesign(request_pb.build_kind, request_pb.design_name)
  if not design:
    logging.warn("Asked to build design '%s', which does not exist." % (request_pb.design_name))
    return False

  if len(design.dependencies) > 0:
    # if there's dependenices, make sure this colony meets them first
    for dependency in design.dependencies:
      matching_building = None
      star_pb = sim.getStar(colony_pb.star_key, True)
      for building_pb in star_pb.buildings:
        if building_pb.colony_key != colony_pb.key:
          continue
        if building_pb.design_name != dependency.designID:
          continue
        # todo: if building_pb.level < dependency.level:
        # todo:   continue
        matching_building = building_pb
      if not matching_building:
        logging.warn("Cannot build %s, because dependency %s is not met." % (
                     request_pb.build_kind, dependency.designID))
        return False


  # Save the initial build model. There's two writes here, once now and once after it
  build_operation_model = mdl.BuildOperation(parent=db.Key(colony_pb.star_key))
  build_operation_model.colony = db.Key(colony_pb.key)
  build_operation_model.empire = db.Key(empire_pb.key)
  build_operation_model.designName = request_pb.design_name
  build_operation_model.designKind = request_pb.build_kind
  build_operation_model.startTime = datetime.now() - timedelta(seconds=5)
  build_operation_model.endTime = build_operation_model.startTime + timedelta(seconds=15)
  build_operation_model.progress = 0.0
  build_operation_model.count = request_pb.count
  build_operation_model.put()

  # make sure we clear the cache so we get the latest version with the new build
  keys = ["buildqueue:for-empire:%s" % empire_pb.key,
          "star:%s" % colony_pb.star_key]
  ctrl.clearCached(keys)

  # We'll need to re-simulate the star now since this new building will affect the ability to
  # build other things as well. It'll also let us calculate the exact end time of this build.
  sim.simulate(colony_pb.star_key)

  # Schedule a build check so that we make sure we'll update everybody when this build completes
  scheduleBuildCheck(sim)

  ctrl.buildRequestModelToPb(request_pb, build_operation_model)
  return request_pb


def getBuildQueueForEmpire(empire_key):
  """Gets the current build queue for the given empire."""

  cache_key = "buildqueue:for-empire:%s" % empire_key
  build_queue = ctrl.getCached([cache_key], pb.BuildQueue)
  if cache_key in build_queue:
    return build_queue[cache_key]

  build_queue = pb.BuildQueue()
  query = mdl.BuildOperation().all().filter("empire", db.Key(empire_key))
  for build_model in query:
    build_pb = build_queue.requests.add()
    ctrl.buildRequestModelToPb(build_pb, build_model)
  ctrl.setCached({cache_key: build_queue})
  return build_queue


def getBuildQueuesForEmpires(empire_keys):
  """Gets the build queue for *multiple* empires, at the same time."""

  build_queue_list = []
  for empire_key in empire_keys:
    build_queue_list.append(getBuildQueueForEmpire(empire_key))

  build_queues = pb.BuildQueue()
  for build_queue in build_queue_list:
    build_queues.requests.extend(build_queue.requests)
  return build_queues


def scheduleBuildCheck(sim=None):
  """Checks when the next build is due to complete and schedules a task to run at that time.

  Because of the way that tasks a scheduled, it's possible that multiple tasks can be scheduled
  at the same time. That's OK because the task itself is idempotent (its just a waste of resources)
  """
  time = None
  query = mdl.BuildOperation.all().order("endTime").fetch(1)
  for build in query:
    # The first one we fetch (because of the ordering) will be the next one. So we'll schedule
    # the build-check to run 5 seconds later (if there's a bunch scheduled at the same time,
    # it's more efficient that way...)
    time = build.endTime + timedelta(seconds=5)

  # Check the simulation -- any builds there might be scheduled before this one
  if sim:
    for star_pb in sim.getStars():
      for build_request_pb in star_pb.build_requests:
        t = ctrl.epochToDateTime(build_request_pb.end_time)
        if t < time:
          time = t

  if not time:
    return

  # It'll be < now() if the next building is never going to finished (and hence it's endTime
  # will be the epoch -- 1970. We'll schedule a build-check in ten minutes anyway
  if time < datetime.now():
    time = datetime.now() + timedelta(minutes=10)

  logging.info("Scheduling next build-check at %s" % (time))
  taskqueue.add(queue_name="build",
                url="/tasks/empire/build-check",
                method="GET",
                eta=time)


def getFleetsForEmpire(empire_pb):
  cache_key = "fleet:for-empire:%s" % empire_pb.key
  values = ctrl.getCached([cache_key], pb.Fleets)
  if cache_key in values:
    return values[cache_key]

  fleet_models = mdl.Fleet.getForEmpire(empire_pb.key)
  logging.debug("Adding %d fleets for %s" % (len(fleet_models), empire_pb.key))
  fleets_pb = pb.Fleets()
  for fleet_model in fleet_models:
    fleet_pb = fleets_pb.fleets.add()
    ctrl.fleetModelToPb(fleet_pb, fleet_model)

  ctrl.setCached({cache_key: fleets_pb})
  return fleets_pb


def getFleet(fleet_key):
  cache_key = "fleet:%s" % (fleet_key)
  fleet = ctrl.getCached([cache_key], pb.Fleet)
  if fleet:
    return fleet[cache_key]

  fleet_model = mdl.Fleet.get(fleet_key)
  fleet_pb = pb.Fleet()
  ctrl.fleetModelToPb(fleet_pb, fleet_model)
  ctrl.setCached({cache_key: fleet_pb})
  return fleet_pb


def _orderFleet_split(fleet_pb, order_pb):
  left_size = order_pb.split_left
  right_size = order_pb.split_right
  if left_size + right_size != fleet_pb.num_ships:
    logging.debug("Number of ships in left/right split (%d/%d) don't match total "
                  "ships in current fleet (%d)" % (left_size, right_size, fleet_pb.num_ships))
    return False

  # This can happen if the original size is 1, or you move the slider all the way
  # over, essentially, it's no change
  if left_size <= 0 or right_size <= 0:
    return True

  left_model = mdl.Fleet.get(fleet_pb.key)
  left_model.numShips = float(left_size)

  right_model = mdl.Fleet(parent = left_model.key().parent())
  right_model.sector = mdl.Fleet.sector.get_value_for_datastore(left_model)
  right_model.empire = mdl.Fleet.empire.get_value_for_datastore(left_model)
  right_model.designName = left_model.designName
  right_model.state = pb.Fleet.IDLE
  right_model.numShips = float(right_size)
  right_model.stateStartTime = datetime.now()

  left_model.put()
  right_model.put()

  return True


def _orderFleet_move(fleet_pb, order_pb):
  fleet_mdl = mdl.Fleet.get(fleet_pb.key)

  if fleet_mdl.state != pb.Fleet.IDLE:
    logging.debug("Cannot move fleet, it's not currently idle.")
    return False

  src_star = sector.getStar(fleet_pb.star_key)
  dst_star = sector.getStar(db.Key(order_pb.star_key))
  distance_in_pc = sector.getDistanceBetweenStars(src_star, dst_star)

  empire_mdl = fleet_mdl.empire
  design = ShipDesign.getDesign(fleet_pb.design_name)

  # work out how much this move operation is going to cost
  fuel_cost = design.fuelCostPerParsec * fleet_mdl.numShips * distance_in_pc
  if fuel_cost > empire_mdl.cash:
    logging.info("Insufficient funds for move: distance=%.2f, num_ships=%d, cost=%.2f"
                 % (distance_in_pc, fleet_mdl.numShips, fuel_cost))
    return False
  else:
    empire_mdl.cash -= fuel_cost
    empire_mdl.put()
    ctrl.clearCached(["empire:%s" % (str(empire_mdl.key())),
                      "empire:for-user:%s" % (empire_mdl.user.email())])

  fleet_mdl.state = pb.Fleet.MOVING
  fleet_mdl.stateStartTime = datetime.now()
  fleet_mdl.destinationStar = db.Key(order_pb.star_key)
  fleet_mdl.put()

  # Let's just hard-code this to 1 hour for now...
  time = datetime.now() + timedelta(hours=(distance_in_pc / design.speed))
  logging.info("distance=%.2f pc, speed=%.2f pc/hr, cost=%.2f, fleet will reach it's destination at %s"
               % (distance_in_pc, design.speed, fuel_cost, time))
  taskqueue.add(queue_name="fleet",
                url="/tasks/empire/fleet/"+fleet_pb.key+"/move-complete",
                method="GET",
                eta=time)

  return True


def _orderFleet_setStance(fleet_pb, order_pb):
  fleet_mdl = mdl.Fleet.get(fleet_pb.key)
  fleet_mdl.stance = order_pb.stance
  fleet_mdl.put()
  return True


def orderFleet(fleet_pb, order_pb):
  success = False
  if order_pb.order == pb.FleetOrder.SPLIT:
    success = _orderFleet_split(fleet_pb, order_pb)
  elif order_pb.order == pb.FleetOrder.MOVE:
    success = _orderFleet_move(fleet_pb, order_pb)
  elif order_pb.order == pb.FleetOrder.SET_STANCE:
    success = _orderFleet_setStance(fleet_pb, order_pb)

  if success:
    star_pb = sector.getStar(fleet_pb.star_key)
    ctrl.clearCached(["fleet:%s" % (fleet_pb.key),
                      "fleet:for-empire:%s" % (fleet_pb.empire_key),
                      "star:%s" % (fleet_pb.star_key),
                      "sector:%d,%d" % (star_pb.sector_x, star_pb.sector_y)])

  return success


class Design(object):
  def __init__(self):
    self.effects = []
    self.dependencies = []

  def getEffects(self, kind=None, level=1):
    """Gets the effects of the given kind, or an empty list if there's none."""
    if not kind:
      return self.effects
    return (effect for effect in self.effects if (effect.kind == kind and
                                                  (effect.level == level or effect.level is None)))

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
  """A BuildingEffect add certain bonuses and whatnot to a star."""
  def __init__(self, kind):
    super(BuildingEffect, self).__init__(kind)
    self.level = None


class BuildingEffectStorage(BuildingEffect):
  """A BuildingEffectStorage adjusts the star's total available storage for minerals and goods."""
  def __init__(self, kind, effectXml):
    super(BuildingEffectStorage, self).__init__(kind)
    self.goods = int(effectXml.get("goods"))
    self.minerals = int(effectXml.get("minerals"))


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
  """A ShipEffect add certain bonuses and whatnot to a ship."""
  def __init__(self, kind):
    super(ShipEffect, self).__init__(kind)

  def onStarLanded(self, fleet_pb, star_pb, sim):
    """This is called when a fleet with this effect "lands" on a star."""
    pass

  def onFleetArrived(self, star_pb, fleet_pb, new_fleet_pb, sim):
    """This is called when we're orbiting a star, and a new fleet arrives."""
    pass

class ShipEffectScout(ShipEffect):
  """The scout effect will generate a scout report every time the ship reaches a star."""
  def __init__(self, kind, effectXml):
    super(ShipEffectScout, self).__init__(kind)

  def onStarLanded(self, fleet_pb, star_pb, sim):
    """This is called when a fleet with this effect "lands" on a star."""
    logging.info("Generating scout report.... star=%s (# planets=%d)" % (star_pb.name, len(star_pb.planets)))
    scout_report_mdl = mdl.ScoutReport(parent=db.Key(star_pb.key))
    scout_report_mdl.empire = db.Key(fleet_pb.empire_key)
    scout_report_mdl.report = star_pb.SerializeToString()
    scout_report_mdl.date = sim.now
    scout_report_mdl.put()
    ctrl.clearCached(["scout-report:%s:%s" % (star_pb.key, fleet_pb.empire_key)])


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
        star_fleet_pb.state_start_time = ctrl.dateTimeToEpoch(sim.now)
        break


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


def _parseBuildingDesign(designXml):
  """Parses a single <design> from the buildings.xml file."""

  design = BuildingDesign()
  logging.debug("Parsing building <design id=\"%s\">" % (designXml.get("id")))
  _parseDesign(designXml, design)
  effectsXml = designXml.find("effects")
  if effectsXml is not None:
    for effectXml in effectsXml.iterfind("effect"):
      level = int(effectXml.get("level"))
      kind = effectXml.get("kind")
      if kind == "storage":
        effect = BuildingEffectStorage(kind, effectXml)
      effect.level = level
      design.effects.append(effect)
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
      design.effects.append(effect)
  return design


def _parseDesign(designXml, design):
  design.id = designXml.get("id")
  design.name = designXml.findtext("name")
  design.description = designXml.findtext("description")
  design.icon = designXml.findtext("icon")
  costXml = designXml.find("cost")
  design.buildCost = costXml.get("credits")
  design.buildTimeSeconds = float(costXml.get("time")) * 3600
  design.buildCostMinerals = float(costXml.get("minerals"))
  dependenciesXml = designXml.find("dependencies")
  if dependenciesXml != None:
    for requiresXml in dependenciesXml.iterfind("requires"):
      designID = requiresXml.get("building")
      level = requiresXml.get("level")
      dep = DesignDependency(designID, level)
      design.dependencies.append(dep)

