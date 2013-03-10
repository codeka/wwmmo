"""empire.py: Controller for empire-related functions."""


import base64
from datetime import datetime, timedelta
import logging
import math
import random

from google.appengine.api import memcache
from google.appengine.api import taskqueue
from google.appengine.ext import db
from google.appengine.ext import deferred

import ctrl
from ctrl import sector
from ctrl import sectorgen
from ctrl import designs
from model import sector as sector_mdl
from model import empire as mdl
from model import statistics as stats_mdl
from protobufs import messages_pb2 as pb


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


def getEmpireByName(name):
  """Searches for an empire by name."""
  for empire_key in (mdl.Empire.all(keys_only=True)
                               .filter("searchName >=", name)
                               .filter("searchName <", name+u"\ufffd").fetch(1)):
    return getEmpire(str(empire_key))
  return None


def getEmpiresByName(name):
  """Searches for an empire by name."""
  empires = []
  for empire_key in (mdl.Empire.all(keys_only=True)
                               .filter("searchName >=", name)
                               .filter("searchName <", name+u"\ufffd")):
    empires.append(getEmpire(str(empire_key)))
  return empires


def getEmpiresByRank(minRank, maxRank):
  if minRank < 1:
    minRank = 1
  if maxRank < minRank:
    maxRank = minRank + 5

  empires = []
  for empire_rank_mdl in (stats_mdl.EmpireRank.all().filter("rank >=", minRank)
                                                    .filter("rank <=", maxRank)):
    empire_key = stats_mdl.EmpireRank.empire.get_value_for_datastore(empire_rank_mdl)
    empires.append(getEmpire(str(empire_key)))
  return empires


def getEmpire(empire_key):
  cache_key = "empire:%s" % (empire_key)
  values = ctrl.getCached([cache_key], pb.Empire)
  if cache_key in values:
    return values[cache_key]

  empire_model = mdl.Empire.get(empire_key)
  empire_pb = pb.Empire()
  ctrl.empireModelToPb(empire_pb, empire_model)
  home_star_key = mdl.Empire.homeStar.get_value_for_datastore(empire_model)
  if home_star_key:
    home_star_pb = sector.getStar(str(home_star_key))
    sector.sumarize(home_star_pb, empire_pb.home_star)
  ctrl.setCached({cache_key: empire_pb})
  return empire_pb


def getEmpireRanks(empire_keys):
  cache_keys = []
  for empire_key in empire_keys:
    cache_keys.append("empire-rank:%s" % empire_key)
  values = ctrl.getCached(cache_keys, pb.EmpireRank)

  empire_ranks = []
  missing_keys = []
  for empire_key in empire_keys:
    cache_key = "empire-rank:%s" % empire_key
    if cache_key in values:
      empire_ranks.append(values[cache_key])
    else:
      missing_keys.append(db.Key(empire_key))

  new_cache_values = {}
  for empire_rank_mdl in stats_mdl.EmpireRank.all().filter("empire IN", missing_keys):
    empire_rank_pb = pb.EmpireRank()
    ctrl.empireRankModelToPb(empire_rank_pb, empire_rank_mdl)
    empire_ranks.append(empire_rank_pb)
    new_cache_values["empire-rank:%s" % empire_rank_pb.empire_key] = empire_rank_pb
  ctrl.setCached(new_cache_values)

  return empire_ranks


# findStarForNewEmpire will fill this with sectors that have all stors with
# a zero (or at least very low) score. We know not to look at these...
_fullSectors = []


def findStarForNewEmpire():
  """Find a star which is suitable for a new empire.

  When a new player joins the game, we want to find a star for their initial
  colony. We need to choose a star that's close to other players, but not TOO
  close as to make them an easy target."""
  sector_model = None
  while not sector_model:
    query = (sector_mdl.Sector.all().filter("numColonies <", 30)
                                    .filter("numColonies >=", 1)
                                    .order("numColonies")
                                    .order("distanceToCentre")
                                    .fetch(10))
    models = []
    for s in query:
      key = "%d:%d" % (s.x, s.y)
      if key not in _fullSectors:
        models.append(s)

    if len(models) == 0:
      query = (sector_mdl.Sector.all().filter("numColonies <", 30)
                                      .order("numColonies")
                                      .order("distanceToCentre")
                                      .fetch(10))
      for s in query:
        models.append(s)

    if len(models) == 0:
      # this would happen if there's no sectors loaded that have no colonies... that's bad!!
      logging.warn("Could not find any sectors for new empire, creating some...")
      sectorgen.expandUniverse(immediate=True)
    else:
      index = random.randint(0, len(models) - 1)
      logging.info("Found %d potential sectors, index=%d" % (len(models), index))
      sector_model = models[index]

  # get the existing colonies/fleets in this sector, stars with colonies
  # or fleets are not candidates for new empires
  colonized_stars = []
  for colony_model in mdl.Colony.all().filter("sector", sector_model):
    empire_key = mdl.Colony.empire.get_value_for_datastore(colony_model)
    if empire_key:
      star_key = str(colony_model.key().parent())
      if star_key not in colonized_stars:
        colonized_stars.append(star_key)
  for fleet_model in mdl.Fleet.all().filter("sector", sector_model):
    empire_key = mdl.Fleet.empire.get_value_for_datastore(fleet_model)
    if empire_key:
      star_key = (fleet_model.key().parent())
      if star_key not in colonized_stars:
        colonized_stars.append(star_key)

  stars = []
  for star_model in sector_mdl.Star.all().filter("sector", sector_model):
    star = {"star_model": star_model}
    star["is_colonized"] = (str(star_model.key()) in colonized_stars)
    stars.append(star)

  # Now find a star within that sector. We'll want one with two terran planets
  # with highish population stats, close to the centre of the sector, but far
  # from any existing (non-Native) colonies. We'll score each of the stars
  # based on these factors and then choose the one with the highest score
  starScores = []
  for star in stars:
    if star["is_colonized"]:
      # colonized stars are right out...
      continue
    star_model = star["star_model"]

    centre = sector.SECTOR_SIZE / 2
    distance_to_centre = math.sqrt((star_model.x - centre) * (star_model.x - centre) +
                                   (star_model.y - centre) * (star_model.y - centre))

    # 0 -- 10 (0 is edge of sector, 10 is centre of sector)
    distance_to_centre_score = (centre - distance_to_centre) / (centre / 10)
    if distance_to_centre_score < 1:
      distance_to_centre_score = 1.0

    # figure out the distance to the closest colony
    distance_to_colony_score = 1.0
    distance_to_other_colony = 0
    other_colony = None
    for other_star in stars:
      if other_star["is_colonized"]:
        distance_to_this_colony = math.sqrt(
            (star_model.x - other_star["star_model"].x) * (star_model.x - other_star["star_model"].x) +
            (star_model.y - other_star["star_model"].y) * (star_model.y - other_star["star_model"].y))
        if not other_colony or distance_to_this_colony < distance_to_other_colony:
          other_colony = other_star
          distance_to_other_colony = distance_to_this_colony
    if other_colony:
      if distance_to_other_colony < 400:
        distance_to_colony_score = 0
      else:
        distance_to_colony_score = 400 / distance_to_other_colony
      distance_to_colony_score *= distance_to_colony_score

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

    score = (distance_to_centre_score * planet_score * congeniality_score *
             distance_to_colony_score)
    logging.debug("Star[%s] score=%.2f distance_to_centre_score=%.2f planet_score=%.2f congeniality_score=%.2f distance_to_colony_score=%.2f distance_to_nearest_colony=%.2f" % (
                  star_model.name, score, distance_to_centre_score,
                  planet_score, congeniality_score, distance_to_colony_score,
                  distance_to_other_colony))

    starScores.append((score, star_model))

  # just choose the star with the highest score
  (score, star_model) = sorted(starScores, reverse=True)[0]
  if score < 5:
    logging.warn("Highest score was %.2f, which is too low. Marking this sector full and trying again." % (score))
    key = "%d:%d" % (star_model.sector.x, star_model.sector.y)
    _fullSectors.append(key)
    return findStarForNewEmpire()
  logging.info("Chose star with score=%d (%s)" % (score, star_model.name))

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
  return (star_key, planet_index)


def calculateEmpireSearchName(name):
  """Given an empire name, calculate the "search name", which is actually a list of the individual
     words in the name, normalized for easy searching."""
  search_name = []
  for name_component in name.split():
    normalized = name_component.lower()
    search_name.append(normalized)
  return search_name


def createEmpire(empire_pb, sim):
  empire_model = mdl.Empire()
  empire_model.cash = 500.0
  ctrl.empirePbToModel(empire_model, empire_pb)
  empire_model.searchName = calculateEmpireSearchName(empire_model.displayName)
  empire_model.put()

  (star_key, planet_index) = findStarForNewEmpire()
  star_pb = sim.getStar(star_key, True)

  # by default, the star will have a bunch of native colonies and fleets... drop those!
  for fleet_pb in star_pb.fleets:
    fleet_mdl = mdl.Fleet.get(fleet_pb.key)
    if fleet_mdl:
      fleet_mdl.delete()
  del star_pb.fleets[:]
  for colony_pb in star_pb.colonies:
    colony_mdl = mdl.Colony.get(colony_pb.key)
    colony_mdl.delete()
  del star_pb.colonies[:]

  # colonize the planet!
  sector_key = sector_mdl.SectorManager.getSectorKey(star_pb.sector_x,
                                                     star_pb.sector_y)
  _colonize(sector_key, empire_model, star_pb, planet_index, count_in_sector=False)

  # add some initial goods and minerals to the colony
  sim.simulate(star_key)
  for empire_presence_pb in star_pb.empires:
    if empire_presence_pb.empire_key == str(empire_model.key()):
      empire_presence_pb.total_goods += 100
      empire_presence_pb.total_minerals += 100

  # give them a colony ship and a couple of scouts for free
  fleet_model = mdl.Fleet(parent=db.Key(star_pb.key))
  fleet_model.empire = empire_model
  fleet_model.sector = sector_key
  fleet_model.designName = "colonyship"
  fleet_model.numShips = 1.0
  fleet_model.state = pb.Fleet.IDLE
  fleet_model.stateStartTime = datetime.now()
  fleet_model.put()

  fleet_model = mdl.Fleet(parent=db.Key(star_pb.key))
  fleet_model.empire = empire_model
  fleet_model.sector = sector_key
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
  if focus_total <= 0.0:
    colony_pb.focus_population = 0.25
    colony_pb.focus_farming = 0.25
    colony_pb.focus_mining = 0.25
    colony_pb.focus_construction = 0.25
  else:
    colony_pb.focus_population = updated_colony_pb.focus_population / focus_total
    colony_pb.focus_farming = updated_colony_pb.focus_farming / focus_total
    colony_pb.focus_mining = updated_colony_pb.focus_mining / focus_total
    colony_pb.focus_construction = updated_colony_pb.focus_construction / focus_total

  # We need to simulate once more to ensure the new end-time for builds, production rates and
  # whatnot are up to date. Then, because of the simulate, we need to update the colonies
  sim.simulate(star_pb.key)

  return colony_pb


def collectTaxesFromColony(colony_key, sim):
  """Transfer the uncollected taxes from the given colony into that colony's empire."""
  colony_pb = getColony(colony_key)
  if not colony_pb:
    return
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


def collectTaxesFromEmpire(empire_pb, sim, async=False):
  if async:
    # if aysnc is true, we just defer ourself on a task queue, where we'll have longer to actually
    # simulate all the stars, etc. Note that the client then takes the responsibility for simulating
    deferred.defer(collectTaxesFromEmpire, empire_pb, sim, False)
    return

  logging.info("Collecting taxes from empire [%s] %s" % (empire_pb.key, empire_pb.display_name))
  colonies_pb = getColoniesForEmpire(empire_pb)

  # first, simulate all of the stars up to this point
  simulated_stars = []
  for colony_pb in colonies_pb.colonies:
    if colony_pb.star_key not in simulated_stars:
      simulated_stars.append(colony_pb.star_key)
      sim.simulate(colony_pb.star_key, do_prediction=False)

  total_cash = empire_pb.cash
  for star_key in simulated_stars:
    star_pb = sim.getStar(star_key)
    for colony_pb in star_pb.colonies:
      if colony_pb.empire_key == empire_pb.key:
        total_cash += colony_pb.uncollected_taxes
        colony_pb.uncollected_taxes = 0.0

  empire_mdl = mdl.Empire.get(empire_pb.key)
  empire_mdl.cash = total_cash
  empire_mdl.put()
  sim.update()
  ctrl.clearCached(["empire:%s" % (empire_pb.key),
                    "empire:for-user:%s" % (empire_mdl.user.email())])

  # send a notification that the empire has been updated
  ctrl.sendNotificationToUser(empire_pb.email, {"empire_updated": 1})


def _colonize(sector_key, empire_model, star_pb, planet_index, count_in_sector=True):
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
  colony_model.cooldownEndTime = datetime.now() + timedelta(hours=8)
  colony_model.put()

  def inc_colony_count():
    sector_model = sector_mdl.Sector.get(sector_key)
    if sector_model.numColonies is None:
      sector_model.numColonies = 1
    else:
      sector_model.numColonies += 1
    sector_model.put()
  if count_in_sector:
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
    if fleet_pb.state != pb.Fleet.IDLE:
      continue
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
  keys = ["fleet:for-empire:%s" % (empire_pb.key)]
  ctrl.clearCached(keys)

  sector_key = sector_mdl.SectorManager.getSectorKey(star_pb.sector_x, star_pb.sector_y)
  empire_model = mdl.Empire.get(empire_pb.key)

  colony_model = _colonize(sector_key, empire_model, star_pb, colonize_request.planet_index)

  colony_pb = pb.Colony()
  ctrl.colonyModelToPb(colony_pb, colony_model)
  return colony_pb


def attackColony(empire_pb, colony_pb, sim):
  """Attacking colonies is actually quite simple, at least compared with
     attacking fleets. The number of ships you have with the "troop carrier"
     effect represents your attack score. The defence score of the colony is
     0.25 * it's population * it's defence boost.

     The number of ships remaining after an attack is:
     num_ships - (population * 0.25 * defence_bonus)
     The number of population remaining after an attack is:
     population - (num_ships * 4 / defence_bonus)

     This is guaranteed to reduce at least one of the numbers to below zero
     in which case, which ever has > 0 is the winner. It could also result in
     both == 0, which is considered a win for the attacking fleet.

     If the population goes below zero, the colony is destroyed. If the number
     of ships goes below zero, the colony remains, but with reduce population
     (hopefully you can rebuild before more ships come!).
  """
  sim.simulate(colony_pb.star_key)
  star_pb = sim.getStar(colony_pb.star_key)

  num_troop_carriers = 0
  troop_carrier_fleet_pbs = []
  for fleet_pb in star_pb.fleets:
    if fleet_pb.empire_key != empire_pb.key:
      continue
    if fleet_pb.state != pb.Fleet.IDLE:
      continue
    design = designs.ShipDesign.getDesign(fleet_pb.design_name)
    if design.hasEffect("troopcarrier"):
      num_troop_carriers += fleet_pb.num_ships
      troop_carrier_fleet_pbs.append(fleet_pb)

  colony_defence = (colony_pb.population * 0.25 * colony_pb.defence_bonus)
  if colony_defence < 1.0:
    colony_defence = 1.0
  remaining_ships = num_troop_carriers - colony_defence
  remaining_population = colony_pb.population - (num_troop_carriers * 4 / colony_pb.defence_bonus)

  if remaining_population <= 0:
    logging.debug("colony destroyed: remaining_population=%.2f remaining_ships=%.2f" % (
                  remaining_population, remaining_ships))
    # the ships won: the colony is destroyed! First, update the fleets so that
    # they have the correct remaining ships, some fleets may be destroyed
    num_lost = num_troop_carriers - remaining_ships
    for fleet_pb in troop_carrier_fleet_pbs:
      if fleet_pb.num_ships < num_lost:
        num_lost - fleet_pb.num_ships
        fleet_pb.time_destroyed = ctrl.dateTimeToEpoch(sim.now)
        fleet_pb.num_ships = 0
      else:
        fleet_pb.num_ships -= num_lost
        num_lost = 0
    # now mark the colony destroyed
    sim.destroyColony(colony_pb)
  else:
    logging.debug("fleets destroyed: remaining_population=%.2f remaining_ships=%.2f" % (
                  remaining_population, remaining_ships))
    # all of the ships were destroyed... first, reduce the colony's population
    # by the corresponding amount
    for star_colony_pb in star_pb.colonies:
      if star_colony_pb.key == colony_pb.key:
        star_colony_pb.population = remaining_population
    # and then destroyed all the fleets
    for fleet_pb in troop_carrier_fleet_pbs:
      fleet_pb.time_destroyed = ctrl.dateTimeToEpoch(sim.now)
      fleet_pb.num_ships = 0


def build(empire_pb, colony_pb, request_pb, sim):
  """Initiates a build operation at the given colony.

  Args:
    empire_pb: The empire that is requesting the build (we assume you've already validated
        the fact that this empire owns the colony)
    colony_pb: The colony where the request has been made.
    request_pb: A BuildRequest protobuf with details of the build request.
    sim: A simulation.Simulation() object
  """

  design = designs.Design.getDesign(request_pb.build_kind, request_pb.design_name)
  if not design:
    logging.warn("Asked to build design '%s', which does not exist." % (request_pb.design_name))
    return False

  if (request_pb.build_kind == pb.BuildRequest.BUILDING
      and design.maxPerColony > 0
      and not request_pb.existing_building_key):
    num_existing = 0
    star_pb = sim.getStar(colony_pb.star_key, True)
    for building_pb in star_pb.buildings:
      if building_pb.colony_key != colony_pb.key:
        continue
      if building_pb.design_name == request_pb.design_name:
        num_existing += 1
    for build_request_pb in star_pb.build_requests:
      if build_request_pb.colony_key != colony_pb.key:
        continue
      if build_request_pb.design_name == request_pb.design_name:
        num_existing += 1
    if num_existing >= design.maxPerColony:
      msg = "Cannot build %s, because we already have the maximum." % (
            request_pb.design_name)
      raise ctrl.ApiError(pb.GenericError.CannotBuildMaxPerColonyReached, msg)
  if (request_pb.build_kind == pb.BuildRequest.BUILDING
      and design.maxPerEmpire > 0
      and not request_pb.existing_building_key):
    num_existing = 0
    colonies_pb = getColoniesForEmpire(empire_pb)
    for other_colony_pb in colonies_pb.colonies:
      star_pb = sim.getStar(other_colony_pb.star_key, True)
      for building_pb in star_pb.buildings:
        if building_pb.colony_key != other_colony_pb.key:
          continue
        if building_pb.design_name == request_pb.design_name:
          num_existing += 1
      for build_request_pb in star_pb.build_requests:
        if build_request_pb.colony_key != other_colony_pb.key:
          continue
        if build_request_pb.design_name == request_pb.design_name:
          num_existing += 1
    if num_existing >= design.maxPerEmpire:
      msg = "Cannot build %s, because we already have the maximum." % (
            request_pb.design_name)
      raise ctrl.ApiError(pb.GenericError.CannotBuildMaxPerEmpireReached, msg)

  dependencies = design.dependencies

  if request_pb.existing_building_key:
    star_pb = sim.getStar(colony_pb.star_key, True)
    for building_pb in star_pb.buildings:
      if building_pb.key == request_pb.existing_building_key:
        if building_pb.level > len(design.upgrades):
          msg = "Cannot build %s, existing building is at maximum level." % (
                 request_pb.design_name)
          raise ctrl.ApiError(pb.GenericError.CannotBuildMaxLevelReached, msg)
        dependencies = design.getDependencies(building_pb.level + 1)

  if len(dependencies) > 0:
    # if there's dependenices, make sure this colony meets them first
    for dependency in dependencies:
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
        if request_pb.existing_building_key:
          msg = "Cannot upgrade %s, because dependency %s is not met." % (
                request_pb.design_name, dependency.designID)
        else:
          msg = "Cannot build %s, because dependency %s is not met." % (
                request_pb.design_name, dependency.designID)
        raise ctrl.ApiError(pb.GenericError.CannotBuildDependencyNotMet, msg)

  # make sure the star is simulated up to this point
  sim.simulate(colony_pb.star_key)

  # Save the initial build model. There's two writes here, once now and once after it
  build_operation_model = mdl.BuildOperation(parent=db.Key(colony_pb.star_key))
  build_operation_model.colony = db.Key(colony_pb.key)
  build_operation_model.empire = db.Key(empire_pb.key)
  build_operation_model.designName = request_pb.design_name
  build_operation_model.designKind = request_pb.build_kind
  build_operation_model.startTime = sim.now - timedelta(seconds=5)
  build_operation_model.endTime = sim.now + timedelta(seconds=15)
  build_operation_model.progress = 0.0
  build_operation_model.count = request_pb.count
  if request_pb.existing_building_key:
    build_operation_model.existingBuilding = db.Key(request_pb.existing_building_key)
  build_operation_model.put()
  ctrl.buildRequestModelToPb(request_pb, build_operation_model)

  # make sure we clear the cache so we get the latest version with the new build
  keys = ["buildqueue:for-empire:%s" % empire_pb.key,
          "star:%s" % colony_pb.star_key]
  ctrl.clearCached(keys)

  # We'll need to re-simulate the star now since this new building will affect the ability to
  # build other things as well. It'll also let us calculate the exact end time of this build.
  sim.updateBuildRequest(request_pb)
  sim.simulate(colony_pb.star_key)

  # Schedule a build check so that we make sure we'll update everybody when this build completes
  scheduleBuildCheck(sim)

  return request_pb


def accelerateBuild(empire_pb, star_pb, build_request_pb, sim, accelerate_amount):
  """Accelerates the given build by subtracting some cash from the owning empire and updating the
     'percent complete' accordingly

  Args:
    empire_pb: Empire whose build we're accelerating.
    star_pb: Star of the build we're accelerating.
    build_request_pb: The actual build request to accelerate.
    sim: A Simulation object which we got the above values from.
    accelerate_amount: A value between 0.5 and 1.0 which described how much of the remaining
                       build to accelerate by (e.g. 0.5 = finish half the build, 1.0 = finish
                       the whole build)
  """
  sim.simulate(star_pb.key)

  remaining_progress = 1.0 - build_request_pb.progress
  progress_to_complete = remaining_progress * accelerate_amount

  design = designs.Design.getDesign(build_request_pb.build_kind, build_request_pb.design_name)
  minerals_to_use = design.buildCostMinerals * progress_to_complete
  cost = minerals_to_use * build_request_pb.count
  if not _subtractCash(empire_pb.key, cost):
    err = pb.GenericError()
    err.error_code = pb.GenericError.InsufficientCash
    err.error_message = "You don't have enough cash to accelerate this build."
    return err

  # adjust the progress, then re-simulate to re-calculate the end_time
  build_request_pb.progress += progress_to_complete
  sim.simulate(star_pb.key)

  return build_request_pb


def stopBuild(empire_pb, star_pb, build_request_pb, sim):
  """Stops (cancels) the given build."""

  # delete the build request from the data store, remove it from the star_pb
  # and re-simulate
  build_op_mdl = mdl.BuildOperation.get(db.Key(build_request_pb.key))
  if build_op_mdl:
    build_op_mdl.delete()

    for n,star_build_request_pb in enumerate(star_pb.build_requests):
      if star_build_request_pb.key == build_request_pb.key:
        del star_pb.build_requests[n]
        break

    sim.simulate(star_pb.key)

    ctrl.clearCached(["buildqueue:for-empire:%s" % empire_pb.key,
                      "star:%s" % star_pb.key])

    return True
  return False


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


def scheduleBuildCheck(sim=None, force_reschedule=False):
  """Checks when the next build is due to complete and schedules a task to run at that time.

  Because of the way that tasks a scheduled, it's possible that multiple tasks can be scheduled
  at the same time. That's OK because the task itself is idempotent (its just a waste of resources)
  """
  time = None
  query = mdl.BuildOperation.all().order("endTime").fetch(1)
  for build in query:
    # The first one we fetch (because of the ordering) will be the next one. So we'll schedule
    # the build-check to run 1 second later (if there's a bunch scheduled at the same time,
    # it's more efficient that way...)
    time = build.endTime + timedelta(seconds=1)

  # Check the simulation -- any builds there might be scheduled before this one
  if sim:
    for star_pb in sim.getStars():
      for build_request_pb in star_pb.build_requests:
        t = ctrl.epochToDateTime(build_request_pb.end_time)
        if not time or t < time:
          time = t

  if not time:
    return
  if time < datetime(2000, 1, 1):
    return
  if time < datetime.now():
    time = datetime.now() + timedelta(seconds=1)

  mc = memcache.Client()
  time_dt = ctrl.dateTimeToEpoch(time)
  existing_dt = mc.get("build-check")
  now_dt = ctrl.dateTimeToEpoch(datetime.now())
  if (not existing_dt or time_dt < existing_dt
       or force_reschedule or existing_dt <= now_dt):
    logging.info("Scheduling next build-check at %s" % (time))
    taskqueue.add(queue_name="build",
                  url="/tasks/empire/build-check?auto=1",
                  method="GET",
                  eta=time)
    mc.set("build-check", time_dt)
  else:
    logging.info("(this_dt=%d, existing_dt=%d) - not scheduling build-check, another is already queued." % (
                 time_dt, existing_dt))


def scheduleMoveCheck():
  time = None
  query = mdl.Fleet.all().filter("eta !=", None).order("eta").fetch(1)
  for fleet in query:
    # The first one we fetch (because of the ordering) will be the next one. So we'll schedule
    # the build-check to run 1 second later (if there's a bunch scheduled at the same time,
    # it's more efficient that way...)
    time = fleet.eta + timedelta(seconds=1)

  if not time:
    return
  if time < datetime(2000, 1, 1):
    return
  if time < datetime.now():
    time = datetime.now() + timedelta(seconds=1)

  mc = memcache.Client()
  time_dt = ctrl.dateTimeToEpoch(time)
  existing_dt = mc.get("move-check")
  now_dt = ctrl.dateTimeToEpoch(datetime.now())
  if (not existing_dt or time_dt < existing_dt
      or existing_dt <= now_dt):
    logging.info("Scheduling next move-check at %s" % (time))
    taskqueue.add(queue_name="fleet",
                  url="/tasks/empire/move-check?auto=1",
                  method="GET",
                  eta=time)
    mc.set("move-check", time_dt)
  else:
    logging.info("(this_dt=%d, existing_dt=%d) - not scheduling move-check, another is already queued." % (
                 time_dt, existing_dt))



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


def _orderFleet_split(star_pb, fleet_pb, order_pb):
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

  fleet_pb.num_ships = float(left_size)

  new_fleet_pb = star_pb.fleets.add()
  new_fleet_pb.empire_key = fleet_pb.empire_key
  new_fleet_pb.design_name = fleet_pb.design_name
  new_fleet_pb.state = pb.Fleet.IDLE
  new_fleet_pb.stance = fleet_pb.stance
  new_fleet_pb.num_ships = float(right_size)
  new_fleet_pb.state_start_time = ctrl.dateTimeToEpoch(datetime.now())

  return True


def _orderFleet_merge(star_pb, fleet_pb, order_pb):
  if fleet_pb.state != pb.Fleet.IDLE:
    logging.info("Cannot merge a (src) fleet that's not IDLE")
    return False
  merged = False
  for other_fleet_pb in star_pb.fleets:
    if other_fleet_pb.key == order_pb.merge_fleet_key:
      if other_fleet_pb.state != pb.Fleet.IDLE:
        logging.info("Cannot merge a (dest) fleet that's not IDLE")
        return False
      if other_fleet_pb.design_name != fleet_pb.design_name:
        logging.info("Cannot merge fleets of different designs")
        return False
      logging.info("Merging fleet [%s] into [%s]" % (
          other_fleet_pb.key, fleet_pb.key))
      fleet_pb.num_ships += other_fleet_pb.num_ships
      other_fleet_pb.time_destroyed = ctrl.dateTimeToEpoch(datetime.now())
      other_fleet_pb.block_notification_on_destroy = True
      merged = True
  if not merged:
    logging.info("No fleet to merge: %s" % (order_pb.merge_fleet_key))
  return merged


def _orderFleet_move(star_pb, fleet_pb, order_pb):
  if fleet_pb.state != pb.Fleet.IDLE:
    logging.debug("Cannot move fleet, it's not currently idle.")
    return False

  src_star = star_pb
  dst_star = sector.getStar(order_pb.star_key)
  distance_in_pc = sector.getDistanceBetweenStars(src_star, dst_star)

  design = designs.ShipDesign.getDesign(fleet_pb.design_name)

  # work out how much this move operation is going to cost
  fuel_cost = design.fuelCostPerParsec * fleet_pb.num_ships * distance_in_pc
  if not _subtractCash(fleet_pb.empire_key, fuel_cost):
    logging.info("Insufficient funds for move: distance=%.2f, num_ships=%d, cost=%.2f"
                 % (distance_in_pc, fleet_pb.num_ships, fuel_cost))
    return False

  fleet_pb.state = pb.Fleet.MOVING
  fleet_pb.state_start_time = ctrl.dateTimeToEpoch(datetime.now())
  fleet_pb.destination_star_key = order_pb.star_key

  # Let's just hard-code this to 1 hour for now...
  time = datetime.now() + timedelta(hours=(distance_in_pc / design.speed))
  logging.info("Fleet [%s]: distance=%.2f pc, speed=%.2f pc/hr, cost=%.2f, fleet will reach it's destination at %s"
               % (fleet_pb.key, distance_in_pc, design.speed, fuel_cost, time))
  fleet_pb.eta = ctrl.dateTimeToEpoch(time)

  return True


def _subtractCash(empire_key, amount):
  """Removes the given amount of cash from the given empire.

  Returns:
    True if the cash was removed, False if you don't have enough cash."""
  def subtractCashInTx(empire_key, amount):
    if amount <= 0:
      return
    empire_mdl = mdl.Empire.get(empire_key)
    if empire_mdl.cash < amount:
      return False
    empire_mdl.cash -= amount
    empire_mdl.put()

    ctrl.clearCached(["empire:%s" % (str(empire_mdl.key())),
                      "empire:for-user:%s" % (empire_mdl.user.email())])
    return True

  return db.run_in_transaction(subtractCashInTx, db.Key(empire_key), int(math.floor(amount)))


def _orderFleet_setStance(star_pb, fleet_pb, order_pb, sim):
  fleet_pb.stance = order_pb.stance
  if order_pb.stance == pb.Fleet.AGGRESSIVE:
    sim.onFleetArrived(fleet_pb.key, star_pb.key)
  return True


def orderFleet(star_pb, fleet_pb, order_pb, sim):
  success = False
  if order_pb.order == pb.FleetOrder.SPLIT:
    success = _orderFleet_split(star_pb, fleet_pb, order_pb)
  elif order_pb.order == pb.FleetOrder.MOVE:
    success = _orderFleet_move(star_pb, fleet_pb, order_pb)
  elif order_pb.order == pb.FleetOrder.SET_STANCE:
    success = _orderFleet_setStance(star_pb, fleet_pb, order_pb, sim)
  elif order_pb.order == pb.FleetOrder.MERGE:
    success = _orderFleet_merge(star_pb, fleet_pb, order_pb)

  if success:
    star_pb = sector.getStar(fleet_pb.star_key)
    ctrl.clearCached(["fleet:for-empire:%s" % (fleet_pb.empire_key),
                      "star:%s" % (fleet_pb.star_key),
                      "sector:%d,%d" % (star_pb.sector_x, star_pb.sector_y)])

  return success


def saveSituationReport(sitrep_pb):
  """Saves the given situation report (a pb.SituationReport) to the data store, and (possibly)
     generates a notification for the user as well."""
  sitrep_blob = sitrep_pb.SerializeToString()
  sitrep_mdl = mdl.SituationReport(parent=db.Key(sitrep_pb.empire_key))
  sitrep_mdl.reportTime = ctrl.epochToDateTime(sitrep_pb.report_time)
  sitrep_mdl.star = db.Key(sitrep_pb.star_key)
  sitrep_mdl.report = sitrep_blob
  sitrep_mdl.put()

  ctrl.clearCached(["sitrep:for-empire:%s" % (sitrep_pb.empire_key),
                    "sitrep:for-star:%s:%s" % (sitrep_pb.empire_key, sitrep_pb.star_key)])

  empire_pb = getEmpire(sitrep_pb.empire_key)
  ctrl.sendNotificationToUser(empire_pb.email, {"sitrep": base64.b64encode(sitrep_blob)})


def getSituationReports(empire_key, star_key=None, cursor=None):
  if not cursor:
    if star_key:
      cache_key = "sitrep:for-star:%s:%s" % (empire_key, star_key)
    else:
      cache_key = "sitrep:for-empire:%s" % empire_key
    values = ctrl.getCached([cache_key], pb.SituationReports)
    if cache_key in values:
      return values[cache_key]

  if star_key:
    (sitrep_models, new_cursor) = mdl.SituationReport.getForStar(empire_key, star_key, cursor)
  else:
    (sitrep_models, new_cursor) = mdl.SituationReport.getForEmpire(empire_key, cursor)
  sitreps_pb = pb.SituationReports()
  sitreps_pb.cursor = new_cursor
  for sitrep_model in sitrep_models:
    sitrep_pb = sitreps_pb.situation_reports.add()
    sitrep_pb.ParseFromString(sitrep_model.report)

  if not cursor:
    ctrl.setCached({cache_key: sitreps_pb})

  return sitreps_pb


