"""simulation.py: Contains the logic for simulating stars, colonies and combat."""

from datetime import datetime, timedelta
import logging
import math

from google.appengine.ext import db
from google.appengine.ext import deferred

import import_fixer
import_fixer.FixImports("google", "protobuf")

import ctrl
from ctrl import empire as empire_ctl
from ctrl import sector as sector_ctl
from ctrl import designs
from model import empire as mdl
from model import sector as sector_mdl
from protobufs import messages_pb2 as pb


def _def_star_fetcher(star_key):
  """This is the default star_fetcher implementation."""
  return sector_ctl.getStar(star_key)


def _def_combat_report_fetcher(star_key, now):
  cache_key = "star:%s:latest-combat-report" % (star_key)
  values = ctrl.getCached([cache_key], pb.CombatReport)
  if cache_key in values:
    return values[cache_key]

  query = (mdl.CombatReport.all()
              .ancestor(db.Key(star_key))
              .order("-startTime"))
  for combat_report_mdl in query.fetch(1):
    if combat_report_mdl.endTime < datetime(2000, 1, 1) or combat_report_mdl.endTime >= now:
      combat_report_pb = pb.CombatReport()
      ctrl.combatReportModelToPb(combat_report_pb, combat_report_mdl, summary=False)
      ctrl.setCached({cache_key: combat_report_pb})
      return combat_report_pb


def _def_log(msg):
  """Default implementation of log, does nothing."""
  pass


class Simulation(object):
  """This class contains the state of a simulation.

  You can use this to simulate a star over and over and then do one single data store update
  at the end, rather than doing it multiple times."""
  def __init__(self, star_fetcher=_def_star_fetcher,
                      combat_report_fetcher=_def_combat_report_fetcher,
                      log=_def_log):
    """Constructs a new Simulation with the given star_fetcher.

    Args:
      star_fetcher: A function that takes a key and returns a Star protocol buffer with the
                    given star. The default implementation fetches stars from
                    ctrl.sector.getStar."""
    self.star_fetcher = star_fetcher
    self.combat_report_fetcher = combat_report_fetcher
    self.log = log
    self.empire_pbs = []
    self.star_pbs = []
    self.combat_report_pbs = []
    self.scout_report_pbs = []
    self.destroyed_colony_pbs = []
    self.need_update = False
    self.now = datetime.now()


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


  def getCombatReport(self, star_key, fetch=False):
    for combat_report_pb in self.combat_report_pbs:
      if combat_report_pb.star_key == star_key:
        if combat_report_pb.start_time == 0 and combat_report_pb.end_time == 0:
          return None
        return combat_report_pb

    if fetch:
      combat_report_pb = self.combat_report_fetcher(star_key, self.now)
      if not combat_report_pb:
        combat_report_pb = pb.CombatReport()
        combat_report_pb.star_key = star_key
      self.combat_report_pbs.append(combat_report_pb)
      return self.getCombatReport(star_key, fetch=False)

    return None


  def addScoutReport(self, scout_report_pb):
    self.scout_report_pbs.append(scout_report_pb)


  def _setCombatReport(self, combat_report_pb):
    for n,existing_combat_report_pb in enumerate(self.combat_report_pbs):
      if existing_combat_report_pb.star_key == combat_report_pb.star_key:
        self.combat_report_pbs[n] = combat_report_pb
        return
    self.combat_report_pbs.append(combat_report_pb)


  def updateBuildRequest(self, build_request_pb):
    for star_pb in self.star_pbs:
      for colony_pb in star_pb.colonies:
        if colony_pb.key == build_request_pb.colony_key:
          for n,existing_build_request_pb in enumerate(star_pb.build_requests):
            if existing_build_request_pb.key == build_request_pb:
              star_pb.build_requests[n] = build_request_pb
              return
          star_pb.build_requests.extend([build_request_pb])
          return

  def destroyColony(self, colony_pb):
    self.destroyed_colony_pbs.append(colony_pb)

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

    empire_keys = set()
    for colony_pb in star_pb.colonies:
      if colony_pb.empire_key not in empire_keys:
        empire_keys.add(colony_pb.empire_key)

    # figure out the start time, which is the oldest last_simulation time
    start_time = self._getSimulateStartTime(star_pb)
    if start_time == 0:
      # No colonies worth simulating...
      return

    start_time = ctrl.epochToDateTime(start_time)
    end_time = self.now

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
        self._simulateStepForAllEmpires(dt, start_time, star_pb, empire_keys)
        start_time = step_end_time
      elif step_end_time < prediction_time:
        if not prediction_star_pb:
          self.log("")
          self.log("---- Last simulation step before prediction phase")
          self.log("")

          self.now = end_time
          dt = end_time - start_time
          if dt.total_seconds() > 0:
            # last little bit of the simulation
            self._simulateStepForAllEmpires(dt, start_time, star_pb, empire_keys)
          start_time += dt

          self.log("")
          self.log("---- Prediction phase beginning")
          self.log("")

          prediction_star_pb = pb.Star()
          prediction_star_pb.ParseFromString(star_pb.SerializeToString())
          dt = timedelta(minutes=15) - dt

        self._simulateStepForAllEmpires(dt, start_time, prediction_star_pb, empire_keys)
        start_time = step_end_time
      else:
        break

    # copy the end times for builds from prediction_star_pb
    for build_req in star_pb.build_requests:
      for prediction_build_req in prediction_star_pb.build_requests:
        if prediction_build_req.key == build_req.key:
          build_req.end_time = prediction_build_req.end_time

    # any fleets that *will be* destroyed, remember the time of their death
    for fleet_pb in star_pb.fleets:
      for prediction_fleet_pb in prediction_star_pb.fleets:
        if fleet_pb.key == prediction_fleet_pb.key:
          fleet_pb.time_destroyed = prediction_fleet_pb.time_destroyed

    # make sure last_simulation is correct
    last_simulation = ctrl.dateTimeToEpoch(self.now)
    for colony_pb in star_pb.colonies:
      colony_pb.last_simulation = last_simulation

    self.need_update = True

  def _getSimulateStartTime(self, star_pb):
    """Gets the time we should start simulate from."""
    start_time = 0
    for colony_pb in star_pb.colonies:
      if start_time == 0 or colony_pb.last_simulation < start_time:
        start_time = colony_pb.last_simulation
    return start_time

  def _simulateStepForAllEmpires(self, dt, now, star_pb, empire_keys):
    for empire_key in empire_keys:
      self._simulateStep(dt, now, star_pb, empire_key)

    # Don't forget to simulate combat for this step as well (TODO: what to do if combat continues
    # after the prediction phase?)
    self._simulateCombat(star_pb, now, dt)

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
    for empire in star_pb.empires:
      if empire_key != empire.empire_key:
        continue
      max_goods = empire.max_goods
      max_minerals = empire.max_minerals

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
        self.log("Total workers = %.2f, requests = %d, workers per build request = %.2f" %
            (total_workers, num_valid_build_requests, workers_per_build_request))

        # OK, we can spare at least ONE population
        if workers_per_build_request < 1:
          workers_per_build_request = 1

        for build_request in build_requests:
          design = designs.Design.getDesign(build_request.build_kind, build_request.design_name)
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
      tax_per_population_per_hour = 0.012
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
        population_increase *= 0.5
      else:
        population_increase *= 0.5 * (goods_efficiency - 1.0)

      # if we're increasing population, it slows down the closer you get to the
      # max population. If population is decreasing, it slows down the FURTHER
      # you get.
      max_factor = float(colony_pb.max_population)
      if max_factor < 10:
        max_factor = 10
      max_factor = colony_pb.population / max_factor
      if max_factor > 1.0:
        # population is bigger than it should be...
        max_factor -= 1.0
        population_increase = -max_factor * population_increase
      else:
        max_factor = 1.0 - max_factor

      population_increase *= max_factor
      colony_pb.delta_population = population_increase

      population_increase *= dt_in_hours
      self.log("max_population: %.2f factor=%.2f population_increase: %.2f new_population: %.2f" % (
              float(colony_pb.max_population), max_factor, population_increase,
              colony_pb.population + population_increase))

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

  def _simulateCombat(self, star_pb, now, dt):
    """Simulates combat for one step of the simulation (i.e. 15 rounds)."""
    def populateCache(star_pb):
      cache = {}
      for fleet_pb in star_pb.fleets:
        cache[fleet_pb.key] = {"design": designs.ShipDesign.getDesign(fleet_pb.design_name),
                               "fleet": fleet_pb}
      return cache

    combat_report_pb = self.getCombatReport(star_pb.key, fetch=True)

    fleets_attacking = False
    for fleet_pb in star_pb.fleets:
      if fleet_pb.state == pb.Fleet.ATTACKING:
        fleets_attacking = True

    if fleets_attacking:
      self.log("")
      self.log("---- Resolving conflicts")
      self.log("")
      cache = populateCache(star_pb)

      if not combat_report_pb:
        combat_report_pb = pb.CombatReport()
        combat_report_pb.star_key = star_pb.key

      attack_start_time = 0
      for fleet_pb in star_pb.fleets:
        if fleet_pb.state != pb.Fleet.ATTACKING:
          continue
        if attack_start_time == 0 or fleet_pb.state_start_time < attack_start_time:
          attack_start_time = fleet_pb.state_start_time

      # round to the next minute
      ast = ctrl.epochToDateTime(attack_start_time)
      attack_start_time = datetime(ast.year, ast.month, ast.day, ast.hour, ast.minute,
                                   0, tzinfo=ast.tzinfo)
      attack_start_time += timedelta(minutes=1)

      # if nobody is scheduled to start attacking yet, then don't...
      if attack_start_time > (now + dt):
        return

      # attacks happen in turns, one per minute. We keep simulating until the conflict is
      # fully resolved (usually not too long)
      end_time = now + dt
      while now < end_time:
        if now < attack_start_time:
          now += timedelta(minutes=1)
          continue

        combat_round_pb = combat_report_pb.rounds.add()
        combat_round_pb.star_key = star_pb.key
        combat_round_pb.round_time = ctrl.dateTimeToEpoch(now)
        still_attacking = self._simulateCombatRound(now, star_pb, cache, combat_round_pb)
        self.log("fleets_attacked = %d" % (len(combat_round_pb.fleets_attacked)))
        if not combat_round_pb.fleets_attacked:
          del combat_report_pb.rounds[-1:]
        elif combat_report_pb.start_time == 0:
            combat_report_pb.start_time = ctrl.dateTimeToEpoch(now)

        now += timedelta(minutes=1)

        if not still_attacking:
          break
        else:
          combat_report_pb.end_time = ctrl.dateTimeToEpoch(now)

      if combat_report_pb.start_time > 0 and not self.getCombatReport(star_pb.key, fetch=False):
        self._setCombatReport(combat_report_pb)

      # go through any fleets that have been destroyed and... do something, I dunno...

  def _simulateCombatRound(self, now, star_pb, cache, combat_round_pb):
    fleet_indices = {}
    for fleet_pb in star_pb.fleets:
      fleet_summary_pb = combat_round_pb.fleets.add()
      fleet_summary_pb.fleet_key = fleet_pb.key
      fleet_summary_pb.empire_key = fleet_pb.empire_key
      fleet_summary_pb.design_id = fleet_pb.design_name
      fleet_summary_pb.num_ships = fleet_pb.num_ships
      fleet_indices[fleet_pb.key] = len(combat_round_pb.fleets) - 1

    # any fleets whose state_start_time is less than now, but more than minute ago has
    # just joined the fray. Let's advertise that fact
    for fleet_pb in star_pb.fleets:
      state_start_time = ctrl.epochToDateTime(fleet_pb.state_start_time)
      if state_start_time < now and state_start_time >= (now - timedelta(minutes=1)):
        combat_round_pb.fleets_joined.add().fleet_index = fleet_indices[fleet_pb.key]

    # any fleet that's aggressive and not currently targetting anything should
    # target something
    for fleet_pb in star_pb.fleets:
      if fleet_pb.state == pb.Fleet.ATTACKING and fleet_pb.target_fleet_key:
        # decide if we should stop attacking this guy...
        if fleet_pb.target_fleet_key not in cache:
          fleet_pb.target_fleet_key = ""
          fleet_pb.state = pb.Fleet.IDLE
        else:
          target = cache[fleet_pb.target_fleet_key]["fleet"]
          if not target or target.time_destroyed:
            fleet_pb.target_fleet_key = ""
            fleet_pb.state = pb.Fleet.IDLE
      if ((fleet_pb.stance == pb.Fleet.AGGRESSIVE or fleet_pb.state == pb.Fleet.ATTACKING) and
           not fleet_pb.target_fleet_key):
        state_start_time = ctrl.epochToDateTime(fleet_pb.state_start_time)
        if state_start_time >= now:
          continue
        # make it idle unless we can find a target
        fleet_pb.state = pb.Fleet.IDLE
        for potential_target_fleet_pb in star_pb.fleets:
          if potential_target_fleet_pb.time_destroyed:
            continue
          if potential_target_fleet_pb.empire_key == fleet_pb.empire_key:
            continue
          fleet_pb.target_fleet_key = potential_target_fleet_pb.key
          fleet_pb.state = pb.Fleet.ATTACKING
          fleet_target_pb = combat_round_pb.fleets_targetted.add()
          fleet_target_pb.fleet_index = fleet_indices[fleet_pb.key]
          fleet_target_pb.target_index = fleet_indices[potential_target_fleet_pb.key]
          break

    # all fleets that are currently attacking fire at once
    hits = []
    for fleet_pb in star_pb.fleets:
      if fleet_pb.state != pb.Fleet.ATTACKING or fleet_pb.time_destroyed:
        continue
      if not fleet_pb.target_fleet_key:
        continue
      state_start_time = ctrl.epochToDateTime(fleet_pb.state_start_time)
      if state_start_time > now:
        continue
      if fleet_pb.target_fleet_key not in cache:
        continue

      target = cache[fleet_pb.target_fleet_key]["fleet"]
      fleet_design = cache[fleet_pb.key]["design"]
      damage = float(fleet_pb.num_ships) * fleet_design.baseAttack # todo: more complicated!
      hits.append({"fleet_key": target.key, "damage": damage})
      self.log("Fleet {%s} attacked by {%s} for %d damage" % (fleet_pb.target_fleet_key,
                                                              fleet_pb.key, int(damage)))
      fleet_attack_pb = combat_round_pb.fleets_attacked.add()
      fleet_attack_pb.fleet_index = fleet_indices[fleet_pb.key]
      fleet_attack_pb.target_index = fleet_indices[fleet_pb.target_fleet_key]
      fleet_attack_pb.damage = damage

    # apply the damage from this round after every fleet has had a turn inflicting it
    for fleet_pb in star_pb.fleets:
      total_damage = 0.0
      for hit in hits:
        if hit["fleet_key"] == fleet_pb.key:
          total_damage += float(hit["damage"])

      if total_damage > 0.0:
        fleet_design = cache[fleet_pb.key]["design"]
        total_damage /= fleet_design.baseDefence
        fleet_pb.num_ships -= total_damage
        if fleet_pb.num_ships < 0.75:
          fleet_pb.num_ships = 0.0
        fleet_damage_pb = combat_round_pb.fleets_damaged.add()
        fleet_damage_pb.fleet_index = fleet_indices[fleet_pb.key]
        fleet_damage_pb.damage = total_damage

    # and fleets that are now destroyed, mark them as such
    for fleet_pb in star_pb.fleets:
      if fleet_pb.num_ships <= 0:
        fleet_pb.num_ships = 0
        if not fleet_pb.time_destroyed:
          self.log("FLEET DESTROYED: %s" % (fleet_pb.key))
          fleet_pb.time_destroyed = ctrl.dateTimeToEpoch(now)

    # go through the attacking fleets and make sure they're still attacking...
    for fleet_pb in star_pb.fleets:
      if fleet_pb.state != pb.Fleet.ATTACKING:
        continue
      if fleet_pb.time_destroyed:
        continue
      return True

    # if there's no more fleets attacking, then any that are left over
    # were victorious, so mark them as such
    for fleet_pb in star_pb.fleets:
      if fleet_pb.time_destroyed:
        continue
      self.log("FLEET VICTORIOUS: %s" % (fleet_pb.key))
      fleet_pb.last_victory = ctrl.dateTimeToEpoch(now)

    return False


  def onFleetArrived(self, new_fleet_key, star_key):
    """This is called when a fleet moves into a new star system.

    We will go through the existing fleets and old fleets and make sure they all know about the
    new fleet. Some may start attacking and so on.

    Args:
      new_fleet_key: The key of the fleet that just arrived. We assume it's already in the star.
      star_key: The key of the star the fleet has moved to.
    """
    star_pb = self.getStar(star_key, True)
    new_fleet_pb = None
    for fpb in star_pb.fleets:
      if fpb.key == new_fleet_key:
        new_fleet_pb = fpb

    # apply any "star landed" effects
    design = designs.ShipDesign.getDesign(new_fleet_pb.design_name)
    for effect in design.getEffects():
      effect.onStarLanded(new_fleet_pb, star_pb, self)

    # if there's any ships already there, apply their onFleetArrived effects
    for other_fleet_pb in star_pb.fleets:
      if other_fleet_pb.key == new_fleet_pb.key:
        continue
      design = designs.ShipDesign.getDesign(other_fleet_pb.design_name)
      for effect in design.getEffects():
        effect.onFleetArrived(star_pb, new_fleet_pb, other_fleet_pb, self)


  def update(self):
    """Apply all of the updates we've made to the data store."""
    if not self.need_update:
      return

    self.log("Applying updates...")

    keys_to_clear = []
    for empire_pb in self.empire_pbs:
      keys_to_clear.append("buildqueue:for-empire:%s" % empire_pb.key)

    for star_pb in self.star_pbs:
      self._updateColonies(star_pb, keys_to_clear)
      self._updateEmpirePresences(star_pb)
      self._updateBuildRequests(star_pb)
      self._updateFleets(star_pb)
      self._updateCombatReport(star_pb)
      self._scheduleFleetDestroy(star_pb)
      self._scheduleFleetVictory(star_pb)

      keys_to_clear.append("star:%s" % star_pb.key)
      keys_to_clear.append("sector:%d,%d" % (star_pb.sector_x, star_pb.sector_y))

    for scout_report_pb in self.scout_report_pbs:
      self._updateScoutReport(scout_report_pb)
      keys_to_clear.append("scout-report:%s:%s" % (scout_report_pb.star_key,
                                                   scout_report_pb.empire_key))

    ctrl.clearCached(keys_to_clear)

  def _updateScoutReport(self, scout_report_pb):
    scout_report_mdl = mdl.ScoutReport(parent=db.Key(scout_report_pb.star_key))
    scout_report_mdl.empire = db.Key(scout_report_pb.empire_key)
    scout_report_mdl.report = scout_report_pb.star_pb
    scout_report_mdl.date = ctrl.epochToDateTime(scout_report_pb.date)
    scout_report_mdl.put()
    scout_report_pb.key = str(scout_report_mdl.key())

  def _updateColonies(self, star_pb, keys_to_clear):
    for colony_pb in star_pb.colonies:
      keys_to_clear.append("colony:%s" % colony_pb.key)

      colony_model = mdl.Colony.get(colony_pb.key)
      for destroyed_colony_pb in self.destroyed_colony_pbs:
        if destroyed_colony_pb.key == colony_pb.key:
          colony_model.delete()
          #TODO: notify owner!

          # if there's no more colonies in this star, make sure we update
          # the time_emptied field so that it doesn't just create a bunch
          # more native colonies the next time around...
          num_remaining_colonies = len(star_pb.colonies)
          for star_colony_pb in star_pb.colonies:
            for destroyed_colony_pb in self.destroyed_colony_pbs:
              if star_colony_pb.key == destroyed_colony_pb.key:
                num_remaining_colonies -= 1
          if num_remaining_colonies <= 0:
            star_mdl = sector_mdl.Star.get(db.Key(star_pb.key))
            star_mdl.time_emptied = ctrl.dateTimeToEpoch(self.now)
            star_mdl.put()
            # note: we'll clear the cached version of this star anyway...

          return
      ctrl.colonyPbToModel(colony_model, colony_pb)
      colony_model.put()

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
      # it could be None if it completed in the meantime...
      if build_operation_model:
        ctrl.buildRequestPbToModel(build_operation_model, build_request_pb)
        self.log("Updating build-request '%s' start_time=%s end_time=%s" % 
                 (build_operation_model.designName, build_operation_model.startTime,
                  build_operation_model.endTime))
        build_operation_model.put()

  def _updateFleets(self, star_pb):
    """Updates the fleet objects inside the given star."""
    for fleet_pb in star_pb.fleets:
      if fleet_pb.key:
        fleet_model = mdl.Fleet.get(fleet_pb.key)
      else:
        fleet_model = None
      if fleet_model:
        ctrl.fleetPbToModel(fleet_model, fleet_pb)
      else:
        fleet_model = mdl.Fleet(parent=db.Key(star_pb.key))
        ctrl.fleetPbToModel(fleet_model, fleet_pb)
        fleet_model.sector = sector_mdl.SectorManager.getSectorKey(star_pb.sector_x,
                                                                   star_pb.sector_y)
      fleet_model.put()

  def _updateCombatReport(self, star_pb):
    # check for an existing one that we want to update instead of adding. If there's any
    # overlap between the start/end dates that's what we wnat to update
    combat_report_pb = self.getCombatReport(star_pb.key, fetch=False)
    if combat_report_pb is None or combat_report_pb.start_time == 0:
      return

    if combat_report_pb.key and len(combat_report_pb.key) > 1:
      combat_report_mdl = mdl.CombatReport.get(db.Key(combat_report_pb.key))
    else:
      combat_report_mdl = mdl.CombatReport(parent=db.Key(star_pb.key))

    combat_report_mdl.startTime = ctrl.epochToDateTime(combat_report_pb.start_time)
    combat_report_mdl.endTime = ctrl.epochToDateTime(combat_report_pb.end_time)
    #combat_report_mdl.startEmpireKeys = combat_report_pb.start_empire_keys
    #combat_report_mdl.endEmpireKeys = combat_report_pb.end_empire_keys
    combat_report_mdl.rounds = combat_report_pb.SerializeToString()
    combat_report_mdl.put()
    combat_report_pb.key = str(combat_report_mdl.key())
    ctrl.clearCached(["star:%s:latest-combat-report" % (star_pb.key)])

  def _scheduleFleetDestroy(self, star_pb):
    """Schedules a task that'll ensure the given fleets is destroyed when it should be."""
    for fleet_pb in star_pb.fleets:
      if fleet_pb.time_destroyed:
        now = ctrl.dateTimeToEpoch(datetime.now())
        countdown = fleet_pb.time_destroyed - now
        combat_report_pb = self.getCombatReport(star_pb.key, fetch=False)
        combat_report_key = None
        if combat_report_pb:
          combat_report_key = combat_report_pb.key

        deferred.defer(on_fleet_destroyed,
                       fleet_pb, combat_report_key,
                       _countdown = countdown)

  def _scheduleFleetVictory(self, star_pb):
    """Schedules a task that'll ensure the given fleets is notified of victory
       when it should be."""
    for fleet_pb in star_pb.fleets:
      if fleet_pb.last_victory and fleet_pb.empire_key:
        now = ctrl.dateTimeToEpoch(datetime.now())
        countdown = fleet_pb.time_destroyed - now
        combat_report_pb = self.getCombatReport(star_pb.key, fetch=False)
        if not combat_report_pb:
          return

        deferred.defer(on_fleet_victorious,
                       fleet_pb, combat_report_pb.key,
                       _countdown = countdown)


def on_fleet_destroyed(fleet_pb, combat_report_key):
  """This is a deferred task that's called when a fleet is destroyed."""
  def doDelete(fleet_pb):
    fleet_mdl = mdl.Fleet.get(fleet_pb.key)
    if fleet_mdl and fleet_mdl.timeDestroyed == ctrl.epochToDateTime(fleet_pb.time_destroyed):
      fleet_mdl.delete()
      return True
    return False

  # quick check to make sure the fleet is really scheduled to be destroyed now
  fleet_mdl = mdl.Fleet.get(fleet_pb.key)
  if not fleet_mdl:
    return
  if fleet_mdl.timeDestroyed != ctrl.epochToDateTime(fleet_pb.time_destroyed):
    logging.debug("Not scheduled to delete fleet at this time (actually, at %s), skipping." % (
                  fleet_mdl.timeDestroyed))
    return

  # simulate until *just before* the fleet is destroyed, so that all of that fleet's effects
  # will be felt, because it's destroyed.
  sim = Simulation()
  sim.now = ctrl.epochToDateTime(fleet_pb.time_destroyed)
  star_pb = sim.getStar(fleet_pb.star_key, True)
  sim.simulate(star_pb.key)

  if db.run_in_transaction(doDelete, fleet_pb):
    # if it turns out we didn't delete the fleet after all, we don't need to update()
    for n,star_fleet_pb in enumerate(star_pb.fleets):
      if star_fleet_pb.key == fleet_pb.key:
        del star_pb.fleets[n]
        break

    sim.update()
    if fleet_pb.empire_key and fleet_pb.empire_key != "":
      keys_to_clear = ["fleet:for-empire:%s" % (fleet_pb.empire_key)]
      ctrl.clearCached(keys_to_clear)
      # Save a sitrep for this situation
      sitrep_pb = pb.SituationReport()
      sitrep_pb.empire_key = fleet_pb.empire_key
      sitrep_pb.report_time = ctrl.dateTimeToEpoch(sim.now)
      sitrep_pb.star_key = star_pb.key
      sitrep_pb.planet_index = -1
      sitrep_pb.fleet_destroyed_record.fleet_design_id = fleet_pb.design_name
      if combat_report_key:
        sitrep_pb.fleet_destroyed_record.combat_report_key = combat_report_key
      empire_ctl.saveSituationReport(sitrep_pb)


def on_fleet_victorious(fleet_pb, combat_report_key):
  """This is a deferred task that's called when a fleet is victorious."""
  def doUpdate(fleet_pb):
    fleet_mdl = mdl.Fleet.get(fleet_pb.key)
    if fleet_mdl and fleet_mdl.lastVictory == ctrl.epochToDateTime(fleet_pb.last_victory):
      fleet_mdl.lastVictory = None
      fleet_mdl.put()
      return True
    return False

  if db.run_in_transaction(doUpdate, fleet_pb):
    if fleet_pb.empire_key and fleet_pb.empire_key != "":
      keys_to_clear = ["fleet:for-empire:%s" % (fleet_pb.empire_key)]
      ctrl.clearCached(keys_to_clear)
      # Save a sitrep for this situation
      sitrep_pb = pb.SituationReport()
      sitrep_pb.empire_key = fleet_pb.empire_key
      sitrep_pb.report_time = fleet_pb.last_victory
      sitrep_pb.star_key = fleet_pb.star_key
      sitrep_pb.planet_index = -1
      sitrep_pb.fleet_victorious_record.fleet_key = fleet_pb.key
      sitrep_pb.fleet_victorious_record.fleet_design_id = fleet_pb.design_name
      sitrep_pb.fleet_victorious_record.num_ships = fleet_pb.num_ships
      sitrep_pb.fleet_victorious_record.combat_report_key = combat_report_key
      empire_ctl.saveSituationReport(sitrep_pb)
