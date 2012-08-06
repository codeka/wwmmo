"""empire.py: Controller for empire-related functions. Aso contains the 'simulate' method."""


import copy
from datetime import datetime, timedelta
import logging
import os
from xml.etree import ElementTree as ET

from google.appengine.ext import db
from google.appengine.api import taskqueue

import ctrl
from ctrl import sector
from model import sector as sector_mdl
from model import empire as mdl
from protobufs import warworlds_pb2 as pb


def getEmpireForUser(user):
  cache_key = "empire:for-user:%s" % (user.user_id())
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
  ctrl.empirePbToModel(empire_model, empire_pb)
  empire_model.put()


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


def _log_noop(msg):
  """This is the default logging function for simulate() -- it does nothing."""
  #pass


def _log_logging(msg):
  logging.debug(msg)


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
  # a little.
  if (end_time - timedelta(seconds=3)) < start_time:
    end_time = start_time + timedelta(seconds=3)

  # We'll simulate in "prediction mode" for an extra bit of time so that we can get a
  # more accurate estimate of the end time for builds. We won't *record* the population
  # growth and such, just the end time of builds. We'll also record the time that the
  # population drops below a certain threshold so that we can warn the player.
  prediction_time = end_time + timedelta(hours=24)
  prediction_star_pb = None

  while True:
    step_end_time = start_time + timedelta(minutes=15)
    if step_end_time < end_time:
      _simulateStep(timedelta(minutes=15), start_time, star_pb, empire_key, log)
      start_time = step_end_time
    elif step_end_time < prediction_time:
      if not prediction_star_pb:
        # if we haven't saved the "final" star_pb yet, do it now
        dt = end_time - start_time
        start_time += dt
        if dt.total_seconds() > 0:
          # last little bit of the simulation
          _simulateStep(dt, start_time, star_pb, empire_key, log)

        log("")
        log("---- Prediction phase beginning")
        log("")
        prediction_star_pb = copy.deepcopy(star_pb)

      _simulateStep(timedelta(minutes=15), start_time, prediction_star_pb, empire_key, log)
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

  log("Simulation @ %s" % (now))
  total_goods = None
  total_minerals = None
  total_population = 0.0
  for empire in star_pb.empires:
    if empire_key != empire.empire_key:
      continue
    total_goods = empire.total_goods
    total_minerals = empire.total_minerals

  if total_goods is None and total_minerals is None:
    # This means we didn't find their entry... add it now
    empire_pb = star_pb.empires.add()
    empire_pb.key = ""
    empire_pb.empire_key = empire_key
    empire_pb.star_key = star_pb.key
    total_goods = 0.0
    total_minerals = 0.0

  dt_in_hours = dt.total_seconds() / 3600.0

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

    # calculate the output from mining this turn and add it to the star global
    minerals = colony_pb.population*colony_pb.focus_mining * (planet_pb.mining_congeniality/100.0)
    colony_pb.delta_minerals = minerals
    log("minerals: %.2f" % (minerals * dt_in_hours))
    total_minerals += minerals * dt_in_hours

    total_population += colony_pb.population

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
      if endTime < now:
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
        total_build_time_in_hours = design.buildTimeSeconds / 3600.0
        total_build_time_in_hours *= (100.0 / workers_per_build_request)
        log("total_build_time = %.2f hrs" % total_build_time_in_hours)

        # Work out how many hours we've spend so far
        time_spent = now - ctrl.epochToDateTime(build_request.start_time)
        time_spent = time_spent.total_seconds() / 3600.0
        # time_spend could be negative, which means we start the build half-way through this
        # step. That's OK, the math still works out
        log("time_spent = %.2f" % time_spent)

        # dt_required is the amount of time available this turn to spend on building.
        dt_required = dt_in_hours
        if time_spent + dt_required > total_build_time_in_hours:
          # If we're going to finish on this turn, we only need a fraction of the minerals we'd
          # otherwise use, so make sure dt_required is correct
          dt_required = total_build_time_in_hours - time_spent
        if dt_required < 0:
          log("Building complete!")
          continue

        # take starting half-way through this turn into account
        if time_spent < 0:
          dt_required += time_spent
          if dt_required < 0:
            dt_required = 0

        log("dt_required = %.2f" % dt_required)

        # work out how many minerals we require for this turn
        minerals_required_per_hour = design.buildCostMinerals / total_build_time_in_hours
        minerals_required = minerals_required_per_hour * dt_required
        log("mineral_required_per_hour = %.2f, minerals_required (this turn) = %.2f"
            % (minerals_required_per_hour, minerals_required))

        if total_minerals > minerals_required:
          # awesome, we have enough minerals so we can make some progress. We'll start by
          # removing the minerals we need from the global pool...
          total_minerals -= minerals_required
          log("remaining minerals: %.2f" % (total_minerals))

          # next, work out the actual amount of progress this turn...
          build_request.progress += dt_required / total_build_time_in_hours
          if build_request.progress >= 1:
            # complete!
            build_request.progress = 1
          log("build progress: %.2f" % (build_request.progress))

          # adjust the end_time for this turn
          build_request.end_time = int(build_request.start_time +
                                       total_build_time_in_hours * 3600.0)
          # note if the build request has already finished, we don't actually have to do
          # anything since it'll be fixed up by the tasks/empire/build-check task.
        else:
          # if we don't have enough minerals, the end time is essentially infinite
          log("  not enough minerals, cannot progress")
          build_request.end_time = 0

  log("--- Updating population:")

  # Finally, update the population. The first thing we need to do is evenly distribute goods
  # between all of the colonies.
  total_goods_per_hour = total_population / 10.0
  total_goods_required = total_goods_per_hour * dt_in_hours
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
    congeniality_factor = colony_pb.population / planet_pb.population_congeniality
    if population_increase >= 0.0:
      congeniality_factor = 1.0 - congeniality_factor
    population_increase *= congeniality_factor

    population_increase *= dt_in_hours
    log("population_increase: %.2f" % (population_increase))

    colony_pb.population += population_increase

  for empire in star_pb.empires:
    if empire_key != empire.empire_key:
      continue
    empire.total_goods = total_goods
    empire.total_minerals = total_minerals

  log(("simulation step: empire=%s dt=%.2f (hrs), delta goods=%.2f, "
       "delta minerals=%.2f, population=%.2f")
       % (empire_key, dt_in_hours, total_goods, total_minerals, total_population))
  log('')


def colonize(empire_pb, star_key, colonize_request):
  """Colonizes the planet given in the colonize_request.

  Args:
    empire_pb: The empire protobuf
    star_key: The key of the star you're going to colonize
    colonize_request: a ColonizeRequest protobuf, containing the planet
        and star key of the planet we want to colonize.
  """

  logging.info("Colonizing: Star=%s Planet=%d" % (star_key,
                                                  colonize_request.planet_index))

  star_model = sector_mdl.Star.get(star_key)
  if star_model is None:
    logging.warn("Could not find star with key: %s" % star_key)
    return None

  if len(star_model.planets) < colonize_request.planet_index:
    logging.warn("colonize_request's planet_index was out of bounds")
    return None

  empire_model = mdl.Empire.get(empire_pb.key)
  colony_model = empire_model.colonize(star_model, colonize_request.planet_index)

  # clear the cache of the various bits and pieces who are now invalid
  keys = ["sector:%d,%d" % (star_model.sector.x, star_model.sector.y),
          "star:%s" % (star_model.key()),
          "colony:for-empire:%s" % (empire_model.key())]
  ctrl.clearCached(keys)

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
  build_operation_model.startTime = datetime.now()
  build_operation_model.endTime = build_operation_model.startTime + timedelta(seconds=10)
  build_operation_model.progress = 0.0
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


def orderFleet(fleet_pb, order_pb):
  if order_pb.order == pb.FleetOrder.SPLIT:
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

    right_model = mdl.Fleet()
    right_model.empire = mdl.Fleet.empire.get_value_for_datastore(left_model)
    right_model.star = mdl.Fleet.star.get_value_for_datastore(left_model)
    right_model.designName = left_model.designName
    right_model.state = pb.Fleet.IDLE
    right_model.numShips = right_size
    right_model.stateStartTime = datetime.now()

    left_model.put()
    right_model.put()

    ctrl.clearCached(["fleet:%s" % fleet_pb.key,
                      "fleet:for-empire:%s" % fleet_pb.empire_key,
                      "star:%s" % fleet_pb.star_key])
    return True

  return False

class Design(object):
  @staticmethod
  def getDesign(kind, name):
    if kind == pb.BuildRequest.BUILDING:
      return BuildingDesign.getDesign(name)
    else:
      return ShipDesign.getDesign(name)


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
  return design


def _parseShipDesign(designXml):
  """Parses a single <design> from the ships.xml file."""

  design = ShipDesign()
  logging.debug("Parsing ship <design id=\"%s\">" % (designXml.get("id")))
  _parseDesign(designXml, design)
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
