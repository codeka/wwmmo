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


def createEmpire(empire_pb):
  empire_model = mdl.Empire()
  empire_model.cash = 500.0
  ctrl.empirePbToModel(empire_model, empire_pb)
  empire_model.put()

  # We need to set you up with some initial bits and pieces. First, we need to find
  # sector for your colony. We look for one with no existing colonized stars and
  # close to the centre of the universe. We chose a random one of the five closest
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
    logging.error("Could not find any sectors for new empire [%s]" % (str(empire_model.key())))
    return

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

  # colonize the planet!
  star_pb = sector.getStar(str(star_model.key()))
  _colonize(sector_model.key(), empire_model, star_pb, planet_index)

  # add some initial goods and minerals to the colony
  simulate(star_pb)
  for empire_presence_pb in star_pb.empires:
    if empire_presence_pb.empire_key == str(empire_model.key()):
      empire_presence_pb.total_goods += 100
      empire_presence_pb.total_minerals += 100
  updateAfterSimulate(star_pb)

  # give them a colony ship and a couple of scouts for free
  fleet_model = mdl.Fleet(parent=star_model)
  fleet_model.empire = empire_model
  fleet_model.sector = sector_model
  fleet_model.designName = "colonyship"
  fleet_model.numShips = 1
  fleet_model.state = pb.Fleet.IDLE
  fleet_model.stateStartTime = datetime.now()
  fleet_model.put()

  fleet_model = mdl.Fleet(parent=star_model)
  fleet_model.empire = empire_model
  fleet_model.sector = sector_model
  fleet_model.designName = "scout"
  fleet_model.numShips = 10
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


def updateColony(colony_key, updated_colony_pb):
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
  star_pb = sector.getStar(colony_pb.star_key)
  simulate(star_pb, colony_pb.empire_key)

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
  simulate(star_pb, colony_pb.empire_key)
  updateAfterSimulate(star_pb, colony_pb.empire_key)

  return colony_pb


def collectTaxes(colony_key):
  """Transfer the uncollected taxes from the given colony into that colony's empire."""
  colony_pb = getColony(colony_key)
  star_pb = sector.getStar(colony_pb.star_key)
  simulate(star_pb, colony_pb.empire_key)

  empire_pb = getEmpire(colony_pb.empire_key)
  empire_pb.cash += colony_pb.uncollected_taxes

  logging.debug("Collect $%.2f in taxes from colony %s" % (colony_pb.uncollected_taxes, colony_pb.key))

  # reset the uncollected taxes of this colony, but make sure it's the colony_pb that's
  # actually in the star (otherwise updateAfterSimulate don't work!)
  for star_colony_pb in star_pb.colonies:
    if colony_pb.key == star_colony_pb.key:
      star_colony_pb.uncollected_taxes = 0.0

  updateAfterSimulate(star_pb, colony_pb.empire_key)

  empire_model = mdl.Empire.get(colony_pb.empire_key)
  empire_model.cash = empire_pb.cash
  empire_model.put() 
  ctrl.clearCached(["empire:%s" % (colony_pb.empire_key),
                    "empire:for-user:%s" % (empire_model.user.email())])


def _log_noop(msg):
  """This is the default logging function for simulate() -- it does nothing."""
  pass


def _log_logging(msg):
  #logging.debug(msg)
  pass


def updateAfterSimulate(star_pb, empire_key=None, log=_log_noop):
  """After you've simulated a star for a particular empire, this updates the data store.

  Usually, you'll simulate the star, update a colony, and then update. This handles the "update"
  phase, making sure all data is updated, caches cleared, etc.
  """

  if empire_key is None:
    # it's easier to do this empire-by-empire, rather then have special-cases
    # throughout the logic below....
    done_empires = set()
    for colony_pb in star_pb.colonies:
      if colony_pb.empire_key not in done_empires:
        done_empires.add(colony_pb.empire_key)
        updateAfterSimulate(star_pb, colony_pb.empire_key, log)
    return

  log("Updating empire: %s" % (empire_key))

  keys_to_clear = []
  keys_to_clear.append("buildqueue:for-empire:%s" % empire_key)

  for colony_pb in star_pb.colonies:
    if colony_pb.empire_key != empire_key:
      continue

    colony_model = mdl.Colony.get(colony_pb.key)
    ctrl.colonyPbToModel(colony_model, colony_pb)
    colony_model.put()
    keys_to_clear.append("colony:%s" % colony_pb.key)

  for empire_pb in star_pb.empires:
    if empire_pb.empire_key != empire_key:
      continue
    if empire_pb.key == "":
      empire_model = mdl.EmpirePresence(parent=db.Key(star_pb.key))
    else:
      empire_model = mdl.EmpirePresence.get(empire_pb.key)
    ctrl.empirePresencePbToModel(empire_model, empire_pb)
    empire_model.put()
    # This is never cached separately...

  for build_pb in star_pb.build_requests:
    if build_pb.empire_key != empire_key:
      continue
    build_model = mdl.BuildOperation.get(build_pb.key)
    ctrl.buildRequestPbToModel(build_model, build_pb)
    log("Updating build-request '%s' start_time=%s end_time=%s" % 
        (build_model.designName, build_model.startTime, build_model.endTime))
    build_model.put()
  keys_to_clear.append("buildqueue:for-empire:%s" % empire_key)

  keys_to_clear.append("star:%s" % star_pb.key)
  ctrl.clearCached(keys_to_clear)


def simulate(star_pb, empire_key=None, log=_log_noop):
  """Simulates the star and gets all of the colonies up to date.

  When simulating a star, we simulate all colonies in that star that belong to the given empire
  at once. This is because there are certain resources (particularly food & minerals) that get
  shared between all colonies in the starsystem.

  Args:
    star_pb: A star protobuf containing details of all the colonies, planets and whatnot in the
        star we're going to simulate.
    empire_key: The key of the empire we're going to simulate. If None, the default, we'll
        simulate all colonies in the star.
    log: A function we'll call to log message as we simulate (by default, this is logging.debug)
  """
  if empire_key is None:
    # it's easier to do this empire-by-empire, rather then have special-cases
    # throughout the logic below....
    done_empires = set()
    for colony_pb in star_pb.colonies:
      if colony_pb.empire_key not in done_empires:
        done_empires.add(colony_pb.empire_key)
        simulate(star_pb, colony_pb.empire_key, log)
    return

  # figure out the start time, which is the oldest last_simulation time
  start_time = 0
  for colony_pb in star_pb.colonies:
    if colony_pb.empire_key != empire_key:
      continue
    if start_time == 0 or colony_pb.last_simulation < start_time:
      start_time = colony_pb.last_simulation

  if start_time == 0:
    # No colonies worth simulating...
    return

  start_time = ctrl.epochToDateTime(start_time)
  end_time = datetime.now()

  # if we have less than a few seconds of time to simulate, we'll extend the end time
  # a little to ensure there's no rounding errors and such
  if (end_time - timedelta(seconds=3)) < start_time:
    end_time = start_time + timedelta(seconds=3)

  # We'll simulate in "prediction mode" for an extra bit of time so that we can get a
  # more accurate estimate of the end time for builds. We won't *record* the population
  # growth and such, just the end time of builds. We'll also record the time that the
  # population drops below a certain threshold so that we can warn the player.
  prediction_time = end_time + timedelta(hours=24)
  prediction_star_pb = None

  while True:
    dt = timedelta(minutes=15)
    step_end_time = start_time + dt
    if step_end_time < end_time:
      _simulateStep(dt, start_time, star_pb, empire_key, log)
      start_time = step_end_time
    elif step_end_time < prediction_time:
      if not prediction_star_pb:
        log("")
        log("---- Last simulation step before prediction phase")
        log("")

        dt = end_time - start_time
        if dt.total_seconds() > 0:
          # last little bit of the simulation
          _simulateStep(dt, start_time, star_pb, empire_key, log)
        start_time += dt

        log("")
        log("---- Prediction phase beginning")
        log("")

        prediction_star_pb = copy.deepcopy(star_pb)
        dt = timedelta(minutes=15) - dt

      _simulateStep(dt, start_time, prediction_star_pb, empire_key, log)
      start_time = step_end_time
    else:
      break

  # copy the end times for builds from prediction_star_pb
  for build_req in star_pb.build_requests:
    for prediction_build_req in prediction_star_pb.build_requests:
      if prediction_build_req.key == build_req.key:
        build_req.end_time = prediction_build_req.end_time

  # Finally, we make sure last_simulation is correct
  last_simulation = ctrl.dateTimeToEpoch(datetime.now())
  for colony_pb in star_pb.colonies:
    if colony_pb.empire_key != empire_key:
      continue

    colony_pb.last_simulation = last_simulation


def _simulateStep(dt, now, star_pb, empire_key, log):
  """Simulates a single step of the colonies in the star.

  The order of simulation needs to be well-defined, so we define it here:
   1. Farming
   2. Mining
   3. Construction
   4. Population

  See comments in the code for the actual algorithm.

  Args:
    dt: A timedelta that represents the time of this step (usually 15 minutes for a complete step,
        but could be a partial step as well).
    now: A datetime representing the "current" time (that is, the start of the current step) which
        we can use to determine things like whether a particular build has actually started or not.
    star_pb: The star protocol buffer we're simulating.
    empire_key: The key of the empire we're simulating. If None, we'll simulate all empires in
        the starsystem.
    log: A function we'll call to log messages (for debugging)
  """

  log("Simulation @ %s (dt=%.4f hrs)" % (now, (dt.total_seconds() / 3600.0)))
  total_goods = None
  total_minerals = None
  total_population = 0.0
  for empire in star_pb.empires:
    if empire_key != empire.empire_key:
      continue
    total_goods = empire.total_goods
    total_minerals = empire.total_minerals

  max_goods = 500
  max_minerals = 500

  if total_goods is None and total_minerals is None:
    # This means we didn't find their entry... add it now
    empire_pb = star_pb.empires.add()
    empire_pb.key = ""
    empire_pb.empire_key = empire_key
    empire_pb.star_key = star_pb.key
    total_goods = 0.0
    total_minerals = 0.0

  dt_in_hours = dt.total_seconds() / 3600.0

  goods_delta_per_hour = 0
  minerals_delta_per_hour = 0

  for n,colony_pb in enumerate(star_pb.colonies):
    log("--- Colony[%d]: pop=%.0f focus=(pop: %.2f, farm: %.2f, mine: %.2f, cons: %.2f)" % (
         n, colony_pb.population,
         colony_pb.focus_population, colony_pb.focus_farming,
         colony_pb.focus_mining, colony_pb.focus_construction))
    if colony_pb.empire_key != empire_key:
      continue

    planet_pb = star_pb.planets[colony_pb.planet_index - 1]

    log("--- planet: congeniality=(pop: %.2f, farm: %.2f, mine: %.2f)" %(
         planet_pb.population_congeniality, planet_pb.farming_congeniality,
         planet_pb.mining_congeniality))

    # calculate the output from farming this turn and add it to the star global
    goods = colony_pb.population*colony_pb.focus_farming * (planet_pb.farming_congeniality/100.0)
    colony_pb.delta_goods = goods
    log("goods: %.2f" % (goods * dt_in_hours))
    total_goods += goods * dt_in_hours
    goods_delta_per_hour += goods

    # calculate the output from mining this turn and add it to the star global
    minerals = colony_pb.population*colony_pb.focus_mining * (planet_pb.mining_congeniality/100.0)
    colony_pb.delta_minerals = minerals
    log("minerals: %.2f" % (minerals * dt_in_hours))
    total_minerals += minerals * dt_in_hours
    minerals_delta_per_hour += minerals

    total_population += colony_pb.population

    for buildings_pb in star_pb.buildings:
      if buildings_pb.colony_key == colony_pb.key:
        design = BuildingDesign.getDesign(buildings_pb.design_name)
        for storage_effect in design.getEffects("storage"):
          log("storage effect, adding: %d goods %d minerals to max storage (%d goods, %d minerals)" %
              (storage_effect.goods, storage_effect.minerals, max_goods, max_minerals))
          max_goods += storage_effect.goods
          max_minerals += storage_effect.minerals

  # A second loop though the colonies, once the goods/minerals have been calculated. This way,
  # goods minerals are shared between colonies
  for colony_pb in star_pb.colonies:
    if colony_pb.empire_key != empire_key:
      continue

    build_requests = []
    for build_req in star_pb.build_requests:
      if build_req.colony_key == colony_pb.key:
        build_requests.append(build_req)

    # not all build requests will be processed this turn. We divide up the population
    # based on the number of ACTUAL build requests they'll be working on this turn
    num_valid_build_requests = 0
    for build_request in build_requests:
      startTime = ctrl.epochToDateTime(build_request.start_time)
      if startTime > (now + dt):
        continue

      # the end_time will be accurate, since it'll have been updated last step
      endTime = ctrl.epochToDateTime(build_request.end_time)
      if endTime < now and endTime > datetime(2000, 1, 1):
        continue

      # as long as it's started but hasn't finished, we'll be working on it this turn
      num_valid_build_requests += 1

    # If we have pending build requests, we'll have to update them as well
    if num_valid_build_requests > 0:
      log("--- Building:")

      total_workers = colony_pb.population * colony_pb.focus_construction
      workers_per_build_request = total_workers / num_valid_build_requests
      log("Total workers = %d, requests = %d, workers per build request = %d" %
          (total_workers, num_valid_build_requests, workers_per_build_request))

      # OK, we can spare at least ONE population
      if workers_per_build_request == 0:
        workers_per_build_request = 1

      for build_request in build_requests:
        design = Design.getDesign(build_request.build_kind, build_request.design_name)
        log("--- Building: %s" % build_request.design_name)

        # work out if the building is supposed to be started this timestep or not. Even if it's
        # scheduled to start this timestep, it may only be scheduled half-way through this time
        # step (for example) so we need to remember that...
        startTime = ctrl.epochToDateTime(build_request.start_time)
        if startTime > (now + dt):
          log("Building not scheduled to be started until %s, skipping" % (startTime))
          continue

        # So the build time the design specifies is the time to build the structure with
        # 100 workers available. Double the workers and you halve the build time. Halve
        # the workers and you double the build time.
        total_build_time_in_hours = build_request.count * design.buildTimeSeconds / 3600.0
        total_build_time_in_hours *= (100.0 / workers_per_build_request)
        log("total_build_time = %.2f hrs" % total_build_time_in_hours)

        # the number of hours of work required, assuming we have all the minerals we need
        time_remaining_in_hours = (1.0 - build_request.progress) * total_build_time_in_hours
        log("start_time = %s, time remaining = %.2f hrs" % (startTime, time_remaining_in_hours))

        dt_used = dt_in_hours
        if startTime > now:
          start_offset = now - startTime
          dt_used -= start_offset.total_seconds() / 3600.0
        if dt_used > time_remaining_in_hours:
          dt_used = time_remaining_in_hours

        # what is the current amount of time we have now as a percentage of the total build
        # time?
        progress_this_turn = dt_used / total_build_time_in_hours
        log("progress this turn: %.4f%% (%.4f hrs)" % (progress_this_turn * 100.0, dt_used))

        if progress_this_turn <= 0:
          log("no progress this turn (building complete?)")
          continue

        # work out how many minerals we require for this turn
        minerals_required = build_request.count * design.buildCostMinerals * progress_this_turn
        log("mineral_required = %.2f, minerals_available = %.2f"
            % (minerals_required, total_minerals))

        if total_minerals < minerals_required:
          # not enough minerals, no progress will be made this turn
          log("no progress this turn (not enough minerals)")
        else:
          # awesome, we have enough minerals so we can make some progress. We'll start by
          # removing the minerals we need from the global pool...
          total_minerals -= minerals_required
          build_request.progress += progress_this_turn
          minerals_delta_per_hour -= minerals_required / dt_in_hours

        # adjust the end_time for this turn
        time_remaining_in_hours = (1.0 - build_request.progress) * total_build_time_in_hours
        end_time = now + timedelta(hours=dt_used) + timedelta(hours=time_remaining_in_hours)
        build_request.end_time = ctrl.dateTimeToEpoch(end_time)

        if build_request.progress >= 1:
          # if we've finished this turn, just set progress
          build_request.progress = 1

        log("total progress: %.2f%% completion time: %s" % (build_request.progress * 100.0, end_time))

    # work out the amount of taxes this colony has generated in the last turn
    tax_per_population_per_hour = 0.004
    tax_this_turn = tax_per_population_per_hour * dt_in_hours * colony_pb.population
    colony_pb.uncollected_taxes += tax_this_turn
    log("tax generated: %.4f; total: %.4f" % (tax_this_turn, colony_pb.uncollected_taxes))

  log("--- Updating population:")

  # Finally, update the population. The first thing we need to do is evenly distribute goods
  # between all of the colonies.
  total_goods_per_hour = total_population / 10.0
  total_goods_required = total_goods_per_hour * dt_in_hours
  goods_delta_per_hour -= total_goods_per_hour
  log("total_goods_required: %.4f, goods_available: %.4f" % (total_goods_required, total_goods))

  # If we have more than total_goods_required stored, then we're cool. Otherwise, our population
  # suffers...
  goods_efficiency = 1.0
  if total_goods_required > total_goods and total_goods_required > 0:
    goods_efficiency = total_goods / total_goods_required
  log("goods_efficiency: %.4f" % (goods_efficiency))

  # subtract all the goods we'll need
  total_goods -= total_goods_required
  if total_goods < 0.0:
    # We've run out of goods! That's bad...
    total_goods = 0.0

  # now loop through the colonies and update the population/goods counter
  for n, colony_pb in enumerate(star_pb.colonies):
    if colony_pb.empire_key != empire_key:
      continue

    log("--- Colony[%d]:" % (n))

    population_increase = colony_pb.population * colony_pb.focus_population
    if goods_efficiency >= 1:
      population_increase *= 0.25
    else:
      population_increase *= goods_efficiency - 1.0
    colony_pb.delta_population = population_increase

    planet_pb = star_pb.planets[colony_pb.planet_index - 1]

    # if we're increasing population, it slows down the closer you get to the population
    # congeniality. If population is decreasing, it slows down the FURTHER you get.
    if planet_pb.population_congeniality < 10:
      congeniality_factor = colony_pb.population / 10.0
    else:
      congeniality_factor = colony_pb.population / planet_pb.population_congeniality
    if population_increase >= 0.0:
      congeniality_factor = 1.0 - congeniality_factor
    population_increase *= congeniality_factor

    population_increase *= dt_in_hours
    log("population_increase: %.2f" % (population_increase))

    colony_pb.population += population_increase

  if total_goods > max_goods:
    total_goods = max_goods
  if total_minerals > max_minerals:
    total_minerals = max_minerals

  for empire in star_pb.empires:
    if empire_key != empire.empire_key:
      continue
    empire.total_goods = total_goods
    empire.total_minerals = total_minerals
    empire.goods_delta_per_hour = goods_delta_per_hour
    empire.minerals_delta_per_hour = minerals_delta_per_hour

  log(("simulation step: empire=%s dt=%.2f (hrs), goods=%.2f (%.4f / hr), "
       "minerals=%.2f (%.4f / hr), population=%.2f")
       % (empire_key, dt_in_hours, total_goods, goods_delta_per_hour,
          total_minerals, minerals_delta_per_hour, total_population))
  log('')


def _colonize(sector_key, empire_model, star_pb, planet_index):
  colony_model = mdl.Colony(parent=db.Key(star_pb.key))
  colony_model.empire = empire_model.key()
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
          "star:%s" % (star_pb.key),
          "colony:for-empire:%s" % (empire_model.key())]
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


def build(empire_pb, colony_pb, request_pb):
  """Initiates a build operation at the given colony.

  Args:
    empire_pb: The empire that is requesting the build (we assume you've already validated
        the fact that this empire owns the colony)
    colony_pb: The colony where the request has been made.
    request_pb: A BuildRequest protobuf with details of the build request.
  """

  design = Design.getDesign(request_pb.build_kind, request_pb.design_name)
  if not design:
    logging.warn("Asked to build design '%s', which does not exist." % (request_pb.design_name))
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
  star_pb = sector.getStar(colony_pb.star_key)
  simulate(star_pb, empire_pb.key, log=_log_logging)
  updateAfterSimulate(star_pb, empire_pb.key, log=_log_logging)

  # Schedule a build check so that we make sure we'll update everybody when this build completes
  scheduleBuildCheck()

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


def scheduleBuildCheck():
  """Checks when the next build is due to complete and schedules a task to run at that time.

  Because of the way that tasks a scheduled, it's possible that multiple tasks can be scheduled
  at the same time. That's OK because the task itself is idempotent (its just a waste of resources)
  """

  query = mdl.BuildOperation.all().order("endTime").fetch(1)
  for build in query:
    # The first one we fetch (because of the ordering) will be the next one. So we'll schedule
    # the build-check to run 5 seconds later (if there's a bunch scheduled at the same time,
    # it's more efficient that way...)
    time = build.endTime + timedelta(seconds=5)

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
  left_model.numShips = left_size

  right_model = mdl.Fleet(parent = left_model.key().parent())
  right_model.sector = mdl.Fleet.sector.get_value_for_datastore(left_model)
  right_model.empire = mdl.Fleet.empire.get_value_for_datastore(left_model)
  right_model.designName = left_model.designName
  right_model.state = pb.Fleet.IDLE
  right_model.numShips = right_size
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
  distance_in_pc = sector.get_distance_between_stars(src_star, dst_star)

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

def orderFleet(fleet_pb, order_pb):
  success = False
  if order_pb.order == pb.FleetOrder.SPLIT:
    success = _orderFleet_split(fleet_pb, order_pb)
  elif order_pb.order == pb.FleetOrder.MOVE:
    success = _orderFleet_move(fleet_pb, order_pb)

  if success:
    star_pb = sector.getStar(fleet_pb.star_key)
    ctrl.clearCached(["fleet:%s" % (fleet_pb.key),
                      "fleet:for-empire:%s" % (fleet_pb.empire_key),
                      "star:%s" % (fleet_pb.star_key),
                      "sector:%d,%d" % (star_pb.sector_x, star_pb.sector_y)])

  return success

class Design(object):
  @staticmethod
  def getDesign(kind, name):
    if kind == pb.BuildRequest.BUILDING:
      return BuildingDesign.getDesign(name)
    else:
      return ShipDesign.getDesign(name)


class BuildingEffect(object):
  """A BuildingEffect add certain bonuses and whatnot to a star."""
  def __init__(self, kind):
    self.level = None
    self.kind = kind


class BuildingEffectStorage(BuildingEffect):
  """A BuildingEffectStorage adjusts the star's total available storage for minerals and goods."""
  def __init__(self, kind, effectXml):
    super(BuildingEffectStorage, self).__init__(kind)
    self.goods = int(effectXml.get("goods"))
    self.minerals = int(effectXml.get("minerals"))


class BuildingDesign(Design):
  _parsedDesigns = None

  def __init__(self):
    self.effects = []

  def getEffects(self, kind=None, level=1):
    """Gets the effects of the given kind, or an empty list if there's none."""
    if not kind:
      return self.effects
    return (effect for effect in self.effects if (effect.kind == kind and
                                                  (effect.level == level or effect.level is None)))

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
  fuelXml = designXml.find("fuel")
  design.fuelCostPerParsec = float(fuelXml.get("costPerParsec"))
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
