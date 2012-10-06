"""simulation.py: Contains the logic for simulating stars, colonies and combat."""

from datetime import datetime, timedelta

from google.appengine.ext import db
from google.appengine.api import taskqueue

import import_fixer
import_fixer.FixImports("google", "protobuf")

import ctrl
from ctrl import empire as empire_ctl
from ctrl import sector as sector_ctl
from model import empire as mdl
from protobufs import warworlds_pb2 as pb


def _def_star_fetcher(star_key):
  """This is the default star_fetcher implementation."""
  return sector_ctl.getStar(star_key)


def _def_log(msg):
  """Default implementation of log, does nothing."""
  pass


class Simulation(object):
  """This class contains the state of a simulation.

  You can use this to simulate a star over and over and then do one single data store update
  at the end, rather than doing it multiple times."""
  def __init__(self, star_fetcher=_def_star_fetcher, log=_def_log):
    """Constructs a new Simulation with the given star_fetcher.

    Args:
      star_fetcher: A function that takes a key and returns a Star protocol buffer with the
                    given star. The default implementation fetches stars from
                    ctrl.sector.getStar."""
    self.star_fetcher = star_fetcher
    self.log = log
    self.empire_pbs = []
    self.star_pbs = []
    self.need_update = False

  def getStars(self):
    return self.star_pbs

  def getStar(self, star_key, fetch=False):
    """Gets the given star_pb from our cache (or fetches it if required)."""
    for star_pb in self.star_pbs:
      if star_pb.key == star_key:
        return star_pb
    if not fetch:
      return None
    star_pb = self.star_fetcher(star_key)
    self.star_pbs.append(star_pb)
    return star_pb

  def simulate(self, star_key):
    """Simulates the star with the given key and gets all of the colonies and fleets up to date.
  
    When simulating a star, we simulate all colonies in that star that belong to the given empire
    at once. This is because there are certain resources (particularly food & minerals) that get
    shared between all colonies in the starsystem.
  
    We also simulate any non-IDLE fleets all at once (regardless of empire) because the rules for
    attacking are quite strict (i.e. alternating attacks, etc). Simulating attacks is relatively rare,
    because they typically resolve within a few minutes.
  
    Args:
      star_pb: A star protobuf containing details of all the colonies, planets and whatnot in the
          star we're going to simulate.
      empire_key: The key of the empire we're going to simulate. If None, the default, we'll
          simulate all colonies in the star.
      log: A function we'll call to log message as we simulate (by default, this is logging.debug)
    """
    star_pb = self.getStar(star_key, True)

    # it's easier to do this empire-by-empire, rather then have special-cases
    # throughout the logic below....
    done_empires = set()
    for colony_pb in star_pb.colonies:
      if colony_pb.empire_key not in done_empires:
        done_empires.add(colony_pb.empire_key)
        self._simulateEmpire(star_pb, colony_pb.empire_key)

    # make sure last_simulation is correct
    last_simulation = ctrl.dateTimeToEpoch(datetime.now())
    for colony_pb in star_pb.colonies:
      colony_pb.last_simulation = last_simulation

    # Now that the colonies are up-to-date, we'll want to simulate any ATTACKING fleets.
    self._simulateCombat(star_pb)
    self.need_update = True

  def _simulateEmpire(self, star_pb, empire_key):
    """It's simpler to simulate empire-by-empire, so we'll do that."""
    # figure out the start time, which is the oldest last_simulation time
    start_time = self._getSimulateStartTime(star_pb, empire_key)
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
        self._simulateStep(dt, start_time, star_pb, empire_key)
        start_time = step_end_time
      elif step_end_time < prediction_time:
        if not prediction_star_pb:
          self.log("")
          self.log("---- Last simulation step before prediction phase")
          self.log("")

          dt = end_time - start_time
          if dt.total_seconds() > 0:
            # last little bit of the simulation
            self._simulateStep(dt, start_time, star_pb, empire_key)
          start_time += dt

          self.log("")
          self.log("---- Prediction phase beginning")
          self.log("")

          prediction_star_pb = pb.Star()
          prediction_star_pb.ParseFromString(star_pb.SerializeToString())
          dt = timedelta(minutes=15) - dt

        self._simulateStep(dt, start_time, prediction_star_pb, empire_key)
        start_time = step_end_time
      else:
        break

    # copy the end times for builds from prediction_star_pb
    for build_req in star_pb.build_requests:
      for prediction_build_req in prediction_star_pb.build_requests:
        if prediction_build_req.key == build_req.key:
          build_req.end_time = prediction_build_req.end_time

  def _getSimulateStartTime(self, star_pb, empire_key):
    """Gets the time we should start simulate from for the given empire."""
    start_time = 0
    for colony_pb in star_pb.colonies:
      if colony_pb.empire_key != empire_key:
        continue
      if start_time == 0 or colony_pb.last_simulation < start_time:
        start_time = colony_pb.last_simulation
    return start_time

  def _simulateStep(self, dt, now, star_pb, empire_key):
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
  
    self.log("Simulation @ %s (dt=%.4f hrs)" % (now, (dt.total_seconds() / 3600.0)))
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
      self.log("--- Colony[%d]: pop=%.0f focus=(pop: %.2f, farm: %.2f, mine: %.2f, cons: %.2f)" % (
               n, colony_pb.population,
               colony_pb.focus_population, colony_pb.focus_farming,
               colony_pb.focus_mining, colony_pb.focus_construction))
      if colony_pb.empire_key != empire_key:
        continue

      planet_pb = star_pb.planets[colony_pb.planet_index - 1]

      self.log("--- planet: congeniality=(pop: %.2f, farm: %.2f, mine: %.2f)" % (
               planet_pb.population_congeniality, planet_pb.farming_congeniality,
               planet_pb.mining_congeniality))

      # calculate the output from farming this turn and add it to the star global
      goods = colony_pb.population*colony_pb.focus_farming * (planet_pb.farming_congeniality/100.0)
      colony_pb.delta_goods = goods
      self.log("goods: %.2f" % (goods * dt_in_hours))
      total_goods += goods * dt_in_hours
      goods_delta_per_hour += goods

      # calculate the output from mining this turn and add it to the star global
      minerals = colony_pb.population*colony_pb.focus_mining * (planet_pb.mining_congeniality/100.0)
      colony_pb.delta_minerals = minerals
      self.log("minerals: %.2f" % (minerals * dt_in_hours))
      total_minerals += minerals * dt_in_hours
      minerals_delta_per_hour += minerals

      total_population += colony_pb.population

      for buildings_pb in star_pb.buildings:
        if buildings_pb.colony_key == colony_pb.key:
          design = empire_ctl.BuildingDesign.getDesign(buildings_pb.design_name)
          for storage_effect in design.getEffects("storage"):
            self.log("storage effect, adding: %d goods %d minerals to max storage (%d goods, %d minerals)" %
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
        self.log("--- Building:")

        total_workers = colony_pb.population * colony_pb.focus_construction
        workers_per_build_request = total_workers / num_valid_build_requests
        self.log("Total workers = %d, requests = %d, workers per build request = %d" %
            (total_workers, num_valid_build_requests, workers_per_build_request))

        # OK, we can spare at least ONE population
        if workers_per_build_request == 0:
          workers_per_build_request = 1

        for build_request in build_requests:
          design = empire_ctl.Design.getDesign(build_request.build_kind, build_request.design_name)
          self.log("--- Building: %s" % build_request.design_name)

          # work out if the building is supposed to be started this timestep or not. Even if it's
          # scheduled to start this timestep, it may only be scheduled half-way through this time
          # step (for example) so we need to remember that...
          startTime = ctrl.epochToDateTime(build_request.start_time)
          if startTime > (now + dt):
            self.log("Building not scheduled to be started until %s, skipping" % (startTime))
            continue

          # So the build time the design specifies is the time to build the structure with
          # 100 workers available. Double the workers and you halve the build time. Halve
          # the workers and you double the build time.
          total_build_time_in_hours = build_request.count * design.buildTimeSeconds / 3600.0
          total_build_time_in_hours *= (100.0 / workers_per_build_request)
          self.log("total_build_time = %.2f hrs" % total_build_time_in_hours)

          # the number of hours of work required, assuming we have all the minerals we need
          time_remaining_in_hours = (1.0 - build_request.progress) * total_build_time_in_hours
          self.log("start_time = %s, time remaining = %.2f hrs" % (startTime, time_remaining_in_hours))

          dt_used = dt_in_hours
          if startTime > now:
            start_offset = now - startTime
            dt_used -= start_offset.total_seconds() / 3600.0
          if dt_used > time_remaining_in_hours:
            dt_used = time_remaining_in_hours

          # what is the current amount of time we have now as a percentage of the total build
          # time?
          progress_this_turn = dt_used / total_build_time_in_hours
          self.log("progress this turn: %.4f%% (%.4f hrs)" % (progress_this_turn * 100.0, dt_used))

          if progress_this_turn <= 0:
            self.log("no progress this turn (building complete?)")
            continue

          # work out how many minerals we require for this turn
          minerals_required = build_request.count * design.buildCostMinerals * progress_this_turn
          self.log("mineral_required = %.2f, minerals_available = %.2f"
                   % (minerals_required, total_minerals))

          if total_minerals < minerals_required:
            # not enough minerals, no progress will be made this turn
            self.log("no progress this turn (not enough minerals)")
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

          self.log("total progress: %.2f%% completion time: %s" %
                   (build_request.progress * 100.0, end_time))

      # work out the amount of taxes this colony has generated in the last turn
      tax_per_population_per_hour = 0.004
      tax_this_turn = tax_per_population_per_hour * dt_in_hours * colony_pb.population
      colony_pb.uncollected_taxes += tax_this_turn
      self.log("tax generated: %.4f; total: %.4f" % (tax_this_turn, colony_pb.uncollected_taxes))

    self.log("--- Updating population:")

    # Finally, update the population. The first thing we need to do is evenly distribute goods
    # between all of the colonies.
    total_goods_per_hour = total_population / 10.0
    total_goods_required = total_goods_per_hour * dt_in_hours
    goods_delta_per_hour -= total_goods_per_hour
    self.log("total_goods_required: %.4f, goods_available: %.4f" % (total_goods_required, total_goods))

    # If we have more than total_goods_required stored, then we're cool. Otherwise, our population
    # suffers...
    goods_efficiency = 1.0
    if total_goods_required > total_goods and total_goods_required > 0:
      goods_efficiency = total_goods / total_goods_required
    self.log("goods_efficiency: %.4f" % (goods_efficiency))

    # subtract all the goods we'll need
    total_goods -= total_goods_required
    if total_goods < 0.0:
      # We've run out of goods! That's bad...
      total_goods = 0.0

    # now loop through the colonies and update the population/goods counter
    for n, colony_pb in enumerate(star_pb.colonies):
      if colony_pb.empire_key != empire_key:
        continue

      self.log("--- Colony[%d]:" % (n))

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
      self.log("population_increase: %.2f" % (population_increase))

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

    self.log(("simulation step: empire=%s dt=%.2f (hrs), goods=%.2f (%.4f / hr), "
             "minerals=%.2f (%.4f / hr), population=%.2f")
             % (empire_key, dt_in_hours, total_goods, goods_delta_per_hour,
                total_minerals, minerals_delta_per_hour, total_population))
    self.log("")

  def _simulateCombat(self, star_pb):
    """Simulates combat (i.e. fleets that are in ATTACKING state)."""
    def populateCache(star_pb):
      cache = {}
      for fleet_pb in star_pb.fleets:
        cache[fleet_pb.key] = {"design": empire_ctl.ShipDesign.getDesign(fleet_pb.design_name),
                               "fleet": fleet_pb}
      return cache

    def findNewTarget(star_pb, fleet_pb):
      fleet_pb.target_fleet_key = ""
      if fleet_pb.stance == pb.Fleet.AGGRESSIVE:
        for potential_target_fleet_pb in star_pb.fleets:
          if potential_target_fleet_pb.num_ships == 0:
            continue
          if fleet_pb.empire_key != potential_target_fleet_pb.empire_key:
            fleet_pb.target_fleet_key = potential_target_fleet_pb.key
          break
      if not fleet_pb.target_fleet_key:
        fleet_pb.state = pb.Fleet.IDLE

    fleets_attacking = False
    for fleet_pb in star_pb.fleets:
      if fleet_pb.state == pb.Fleet.ATTACKING:
        fleet_pb.time_destroyed = 0
        fleets_attacking = True

    if fleets_attacking:
      self.log("")
      self.log("---- Resolving conflicts")
      self.log("")
      cache = populateCache(star_pb)

      attack_start_time = 0
      for fleet_pb in star_pb.fleets:
        if fleet_pb.state != pb.Fleet.ATTACKING:
          continue
        if attack_start_time == 0 or fleet_pb.state_start_time < attack_start_time:
          attack_start_time = fleet_pb.state_start_time

      # round to the next minute
      dt = ctrl.epochToDateTime(attack_start_time)
      attack_start_time = datetime(dt.year, dt.month, dt.day, dt.hour, dt.minute, 0)
      attack_start_time += timedelta(minutes=1)

      # attacks happen in turns, one per minute. We keep simulating until the conlict is
      # fully resolved (usually not too long)
      now = attack_start_time
      attack_round = 0
      is_predicting = False
      real_star_pb = star_pb
      while True:
        if now > datetime.now() and not is_predicting:
          # OK, now we're in prediction mode, work on a copy of the protocol buffer so that we
          # don't kill off things that haven't finished yet...
          self.log("Finished actual time, now predicting...")
          is_predicting = True
          serialized = star_pb.SerializeToString()
          star_pb = pb.Star()
          star_pb.ParseFromString(serialized)
          cache = populateCache(star_pb)

        hits = []
        for fleet_pb in star_pb.fleets:
          if fleet_pb.state != pb.Fleet.ATTACKING:
            continue
          if fleet_pb.num_ships == 0:
            continue
          if fleet_pb.target_fleet_key not in cache:
            findNewTarget(star_pb, fleet_pb)
            if fleet_pb.state != pb.Fleet.ATTACKING:
              continue

          if fleet_pb.num_ships > 0:
            target = cache[fleet_pb.target_fleet_key]["fleet"]

            if target.num_ships > 0:
              damage = fleet_pb.num_ships # todo: more complicated!
              hits.append({"fleet_key": target.key, "damage": damage})

        # apply the damage from this round after every fleet has had a turn inflicting it
        for hit in hits:
          fleet_pb = cache[hit["fleet_key"]]["fleet"]
          fleet_pb.num_ships -= int(hit["damage"])
          if fleet_pb.num_ships <= 0:
            fleet_pb.num_ships = 0
            if not fleet_pb.time_destroyed:
              self.log("FLEET DESTROYED: %s" %(fleet_pb.key))
              fleet_pb.time_destroyed = ctrl.dateTimeToEpoch(now)

        # go through the attacking fleets and make sure they're still attacking...
        remaining_attacking_fleets = []
        for fleet_pb in star_pb.fleets:
          if fleet_pb.state != pb.Fleet.ATTACKING:
            continue
          if fleet_pb.num_ships == 0:
            continue

          if fleet_pb.target_fleet_key not in cache:
            findNewTarget(star_pb, fleet_pb)
            if fleet_pb.state != pb.Fleet.ATTACKING:
              continue

          target = cache[fleet_pb.target_fleet_key]["fleet"]
          if target.num_ships == 0:
            findNewTarget(star_pb, fleet_pb)
            if fleet_pb.state != pb.Fleet.ATTACKING:
              continue

          remaining_attacking_fleets.append(fleet_pb)

        now += timedelta(minutes=1)
        attack_round += 1
        self.log("Round:%d results:" % (attack_round))
        for fleet_pb in star_pb.fleets:
          self.log("  Fleet:%s [empire=%s] [design=%s] [num-ships=%d] [state=%d] [stance=%d]" %
              (fleet_pb.key, fleet_pb.empire_key, fleet_pb.design_name, fleet_pb.num_ships,
               fleet_pb.state, fleet_pb.stance))

        if not remaining_attacking_fleets:
          break

      if is_predicting:
        # if we ended up predicting stuff, make sure we copy the "time destroyed" at least...
        for predicted_fleet_pb in star_pb.fleets:
          if not predicted_fleet_pb.time_destroyed:
            continue
          for fleet_pb in real_star_pb.fleets:
            if fleet_pb.key == predicted_fleet_pb.key:
              fleet_pb.time_destroyed = predicted_fleet_pb.time_destroyed

      # go through any fleets that have been destroyed and... do something, I dunno...

  def onFleetArrived(self, fleet_key, star_key):
    """This is called when a fleet moves into a new star system.

    We will go through the existing fleets and old fleets and make sure they all know about the
    new fleet. Some may start attacking and so on.

    Args:
      fleet_key: The key of the fleet that moved. We assume it's already in the star.
      star_key: The key of the star the fleet has moved to.
    """
    star_pb = self.getStar(star_key, True)
    fleet_pb = None
    for fpb in star_pb.fleets:
      if fpb.key == fleet_key:
        fleet_pb = fpb

    # apply any "star landed" effects
    design = empire_ctl.ShipDesign.getDesign(fleet_pb.design_name)
    for effect in design.getEffects():
      effect.onStarLanded(fleet_pb, star_pb, self)

    # if there's any ships already there, apply their onFleetArrived effects
    for other_fleet_pb in star_pb.fleets:
      if other_fleet_pb.key == fleet_pb.key:
        continue
      design = empire_ctl.ShipDesign.getDesign(other_fleet_pb.design_name)
      for effect in design.getEffects():
        effect.onFleetArrived(star_pb, fleet_pb, other_fleet_pb, self)

  def update(self):
    """Apply all of the updates we've made to the data store."""
    if not self.need_update:
      return

    self.log("Applying updated...")

    keys_to_clear = []
    for empire_pb in self.empire_pbs:
      keys_to_clear.append("buildqueue:for-empire:%s" % empire_pb.key)

    for star_pb in self.star_pbs:
      self._updateColonies(star_pb, keys_to_clear)
      self._updateEmpirePresences(star_pb)
      self._updateBuildRequests(star_pb)
      self._updateFleets(star_pb)
      self._scheduleFleetDestroy(star_pb)

      keys_to_clear.append("star:%s" % star_pb.key)
      keys_to_clear.append("sector:%d,%d" % (star_pb.sector_x, star_pb.sector_y))

    ctrl.clearCached(keys_to_clear)

  def _updateColonies(self, star_pb, keys_to_clear):
    for colony_pb in star_pb.colonies:
      colony_model = mdl.Colony.get(colony_pb.key)
      ctrl.colonyPbToModel(colony_model, colony_pb)
      colony_model.put()
      keys_to_clear.append("colony:%s" % colony_pb.key)

  def _updateEmpirePresences(self, star_pb):
    """Updates the empire presence models in the given star."""
    for empire_pb in star_pb.empires:
      if not empire_pb.empire_key:
        continue
      if empire_pb.key == "":
        empire_model = mdl.EmpirePresence(parent=db.Key(star_pb.key))
      else:
        empire_model = mdl.EmpirePresence.get(empire_pb.key)
      ctrl.empirePresencePbToModel(empire_model, empire_pb)
      empire_model.put()

  def _updateBuildRequests(self, star_pb):
    """Updated the build requests in the given star."""
    for build_request_pb in star_pb.build_requests:
      build_operation_model = mdl.BuildOperation.get(build_request_pb.key)
      ctrl.buildRequestPbToModel(build_operation_model, build_request_pb)
      self.log("Updating build-request '%s' start_time=%s end_time=%s" % 
               (build_operation_model.designName, build_operation_model.startTime,
                build_operation_model.endTime))
      build_operation_model.put()

  def _updateFleets(self, star_pb):
    """Updates the fleet objects inside the given star."""
    for fleet_pb in star_pb.fleets:
      fleet_model = mdl.Fleet.get(fleet_pb.key)
      ctrl.fleetPbToModel(fleet_model, fleet_pb)
      fleet_model.put()

  def _scheduleFleetDestroy(self, star_pb):
    """Schedules a task that'll ensure the given fleets is destroyed when it should be."""
    for fleet_pb in star_pb.fleets:
      if fleet_pb.time_destroyed:
        time = ctrl.epochToDateTime(fleet_pb.time_destroyed)
        taskqueue.add(queue_name="fleet",
                      url=("/tasks/empire/fleet/%s/destroy?dt=%d" % (fleet_pb.key,
                                                                     fleet_pb.time_destroyed)),
                      method="GET",
                      eta=time)

