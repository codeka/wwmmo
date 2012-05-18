
import ctrl
from ctrl import sector
from model import sector as sector_mdl
from model import empire as mdl
from protobufs import warworlds_pb2 as pb
import logging
from xml.etree import ElementTree as ET
from datetime import datetime, timedelta
import os
from google.appengine.ext import db
from google.appengine.api import taskqueue


def getEmpireForUser(user):
  cache_key = 'empire:for-user:%s' % (user.user_id())
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
  cache_key = 'empire:%s' % (empire_key)
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
  cache_key = 'colony:for-empire:%s' % empire_pb.key
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
  cache_key = 'colony:%s' % colony_key
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
  '''Updates the colony with the given colony_key with the new parameters in updated_colony_pb.

  When updating a colony, there's a few things we need to do. For example, we need to simulate
  the colony with it's old parameters to bring it up to date. Then we need to make sure th 
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
  '''
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

  # because of the simulation, we have to update all colonies.
  updateAfterSimulate(star_pb, colony_pb.empire_key)

def updateAfterSimulate(star_pb, empire_key):
  '''After you've simulated a star for a particular empire, this updates the data store.

  Usually, you'll simulate the star, update a colony, and then update. This handles the "update"
  phase, making sure all data is updated, caches cleared, etc.
  '''
  if empire_key is None:
    # it's easier to do this empire-by-empire, rather then have special-cases
    # throughout the logic below....
    done_empires = set()
    for colony_pb in star_pb.colonies:
      if colony_pb.empire_key not in done_empires:
        done_empires.add(colony_pb.empire_key)
        updateAfterSimulate(star_pb, colony_pb.empire_key)
    return

  keys_to_clear = []
  keys_to_clear.append('buildqueue:for-empire:%s' % empire_key)

  for colony_pb in star_pb.colonies:
    if colony_pb.empire_key != empire_key:
      continue

    colony_pb.last_simulation = ctrl.dateTimeToEpoch(datetime.now())
    colony_model = mdl.Colony.get(colony_pb.key)
    ctrl.colonyPbToModel(colony_model, colony_pb)
    colony_model.put()
    keys_to_clear.append('colony:%s' % colony_pb.key)

  for empire_pb in star_pb.empires:
    if empire_pb.empire_key != empire_key:
      continue
    if empire_pb.key == '':
      empire_model = mdl.EmpirePresence()
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
    build_model.put()
  keys_to_clear.append('buildqueue:for-empire:%s' % empire_key)

  keys_to_clear.append('star:%s' % star_pb.key)
  ctrl.clearCached(keys_to_clear)


def simulate(star_pb, empire_key=None):
  '''Simulates the star and gets all of the colonies up to date.

  When simulating a star, we simulate all colonies in that star that belong to the given empire
  at once. This is because there are certain resources (particularly food & minerals) that get
  shared between all colonies in the starsystem.

  Args:
    star_pb: A star protobuf containing details of all the colonies, planets and whatnot in the
        star we're going to simulate.
    empire_key: The key of the empire we're going to simulate. If None, the default, we'll simulate
        all colonies in the star.
  '''
  if empire_key is None:
    # it's easier to do this empire-by-empire, rather then have special-cases
    # throughout the logic below....
    done_empires = set()
    for colony_pb in star_pb.colonies:
      if colony_pb.empire_key not in done_empires:
        done_empires.add(colony_pb.empire_key)
        simulate(star_pb, colony_pb.empire_key)
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

  while True:
    step_end_time = start_time + timedelta(minutes=15)
    if step_end_time < end_time:
      _simulateStep(timedelta(minutes=15), start_time, star_pb, empire_key)
      start_time = step_end_time
    else:
      break

  dt = end_time - start_time
  if dt.total_seconds() > 0:
    _simulateStep(dt, start_time, star_pb, empire_key)


def _simulateStep(dt, now, star_pb, empire_key):
  '''Simulates a single step of the colonies in the star.

  The order of simulation needs to be well-defined, so we define it here:
   1. Farming
   2. Mining
   3. Construction
   4. Population
  
  See comments in the code for the actual algorithm.
  
  Args:
    dt: A timedelta that represents the time of this step (usually 15 minutes for
        a complete step, but could be a partial step as well).
    now: A datetime representing the "current" time (that is, the start of the
        current step) which we can use to determine things like whether a particular
        build has actually started or not.
    star_pb: The star protocol buffer we're simulating.
    empire_key: The key of the empire we're simulating. If None, we'll simulate
        all empires in the starsystem.
  '''
  logging.debug("Simulation @ %s" % (now))
  total_goods = None
  total_minerals = None
  total_population = 0
  for empire in star_pb.empires:
    if empire_key != empire.empire_key:
      continue
    total_goods = empire.total_goods
    total_minerals = empire.total_minerals

  if total_goods is None and total_minerals is None:
    # This means we didn't find their entry... add it now
    empire_pb = star_pb.empires.add()
    empire_pb.key = ''
    empire_pb.empire_key = empire_key
    empire_pb.star_key = star_pb.key
    total_goods = 0
    total_minerals = 0

  dt_in_hours = dt.total_seconds() / 3600.0

  for colony_pb in star_pb.colonies:
    if colony_pb.empire_key != empire_key:
      continue

    planet_pb = None
    for pb in star_pb.planets:
      if pb.key == colony_pb.planet_key:
        planet_pb = pb
        break

    # calculate the output from farming this turn and add it to the star global
    goods = colony_pb.population * colony_pb.focus_farming * (planet_pb.farming_congeniality/100.0)
    total_goods += goods * dt_in_hours

    # calculate the output from mining this turn and add it to the star global
    minerals = colony_pb.population * colony_pb.focus_mining * (planet_pb.mining_congeniality/100.0)
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

    # If we have pending build requests, we'll have to update them as well
    if len(build_requests) > 0:
      total_workers = colony_pb.population * colony_pb.focus_construction
      workers_per_build_request = total_workers / len(build_requests)
      logging.debug("Total workers = %d, workers per build request = %d" % (total_workers, workers_per_build_request))

      for build_request in build_requests:
        design = Design.getDesign(build_request.build_kind, build_request.design_name)
        logging.debug("Building: %s" % build_request.design_name)

        # work out if the building is supposed to be started this timestep or not. Even if it's
        # scheduled to start this timestep, it may only be scheduled half-way through this time
        # step (for example) so we need to remember that...
        startTime = ctrl.epochToDateTime(build_request.start_time)
        if startTime > (now + dt):
          logging.debug("Building not scheduled to be started until %s, skipping" % (startTime))
          continue

        # So the build time the design specifies is the time to build the structure assigning
        # 100 workers are available. Double the workers and you halve the build time. Halve
        # the workers and you double the build time. 
        total_build_time = design.buildTimeSeconds / 3600.0
        logging.debug("total_build_time(1) = %.4f" % total_build_time)
        total_build_time *= (100.0 / workers_per_build_request)
        logging.debug("total_build_time(2) = %.4f" % total_build_time)

        # Work out how many hours we've spend so far
        time_spent = now - ctrl.epochToDateTime(build_request.start_time)
        time_spent = time_spent.total_seconds() / 3600.0
        logging.debug("time_spent = %.4f" % time_spent)

        dt_required = dt_in_hours
        if time_spent + dt_required > total_build_time:
          # If we're going to finish on this turn, we only need a fraction of the minerals we'd
          # otherwise use, so make sure dt_required is correct
          dt_required = total_build_time - time_spent
        logging.debug("dt_required = %.4f" % dt_required)

        # work out how many minerals we require for this turn
        minerals_required_per_hour = design.buildCostMinerals / total_build_time
        minerals_required = minerals_required_per_hour * dt_required
        logging.debug("minerals_required = %.4f" % minerals_required)

        if total_minerals > minerals_required:
          total_minerals -= minerals_required

          # adjust the end_time for this turn
          build_request.end_time = int(build_request.start_time + total_build_time * 3600.0)
          # note if the build request has already finished, we don't actually have to do
          # anything since it'll be fixed up by the tasks/empire/build-check task.
        else:
          # if we don't have enough minerals, the end time is essentially infinite
          build_request.end_time = 0

  # Finally, update the population. The first thing we need to do is evenly distribute goods
  # between all of the colonies.
  total_goods_per_hour = total_population / 10.0
  total_goods_required = total_goods_per_hour * dt_in_hours

  # If we have more than total_goods_required stored, then we're cool. Otherwise, our population
  # suffers...
  goods_efficiency = 1.0
  if total_goods_required > total_goods and total_goods > 0:
    goods_efficiency = total_goods_required / total_goods
  elif total_goods == 0:
    goods_efficiency = 0

  # subtract all the goods we'll need
  total_goods -= total_goods_required

  # now loop through the colonies and update the population/goods counter
  for colony_pb in star_pb.colonies:
    if colony_pb.empire_key != empire_key:
      continue

    population_increase = colony_pb.population * colony_pb.focus_population
    population_increase *= (goods_efficiency - 0.75) # that is, from -0.75 -> 0.25

    planet_pb = None
    for pb in star_pb.planets:
      if pb.key == colony_pb.planet_key:
        planet_pb = pb
        break
    congeniality_factor = 1.0 - (colony_pb.population / planet_pb.population_congeniality)
    population_increase *= congeniality_factor

    population_increase *= dt_in_hours
    colony_pb.population += int(population_increase)

  for empire in star_pb.empires:
    if empire_key != empire.empire_key:
      continue
    empire.total_goods = total_goods
    empire.total_minerals = total_minerals

  logging.debug("simulation step: empire=%s dt=%.2f (hrs), delta goods=%.2f, delta minerals=%.2f, population=%.2f" % (
      empire_key, dt_in_hours, total_goods, total_minerals, total_population))


def colonize(empire_pb, colonize_request):
  '''Colonizes the planet given in the colonize_request.

  Args:
    empire_pb: The empire protobuf
    colonize_request: a ColonizeRequest protobuf, containing the planet
        and star key of the planet we want to colonize.
  '''
  star_model = sector_mdl.SectorManager.getStar(colonize_request.star_key)
  if star_model is None:
    logging.warn("Could not find star with key: %s" % colonize_request.star_key)
    return None

  planet_model = None
  for planet in star_model.planets:
    if str(planet.key()) == colonize_request.planet_key:
      planet_model = planet
      break

  if planet_model is None:
    logging.warn("Found star, but not planet with key: %s" % colonize_request.planet_key)
    return None

  empire_model = mdl.Empire.get(empire_pb.key)
  colony_model = empire_model.colonize(planet_model)

  # clear the cache of the various bits and pieces who are now invalid
  keys = ['sector:%d,%d' % (star_model.sector.x, star_model.sector.y),
          'star:%s' % (star_model.key()),
          'colonies:for-empire:%s' % (empire_model.key())]
  ctrl.clearCached(keys)

  colony_pb = pb.Colony()
  ctrl.colonyModelToPb(colony_pb, colony_model)
  return colony_pb


def build(empire_pb, colony_pb, request_pb):
  '''Initiates a build operation at the given colony.

  Args:
    empire_pb: The empire that is requesting the build (we assume you've already validated
        the fact that this empire owns the colony)
    colony_pb: The colony where the request has been made.
    request_pb: A BuildRequest protobuf with details of the build request.
  '''
  design = Design.getDesign(request_pb.build_kind, request_pb.design_name)
  if not design:
    logging.warn("Asked to build design '%s', which does not exist." % (request_pb.design_name))
    return False

  # Save the initial build model. There's two writes here, once now and once after it
  build_model = mdl.BuildOperation()
  build_model.colony = db.Key(colony_pb.key)
  build_model.empire = db.Key(empire_pb.key)
  build_model.star = db.Key(colony_pb.star_key)
  build_model.designName = request_pb.design_name
  build_model.designKind = request_pb.build_kind
  build_model.startTime = datetime.now()
  build_model.endTime = build_model.startTime + timedelta(seconds=10) # - until we simulate (below)
  build_model.put()

  # make sure we clear the cache so we get the latest version with the new build
  keys = ['buildqueue:for-empire:%s' % empire_pb.key,
          'star:%s' % colony_pb.star_key]
  ctrl.clearCached(keys)

  # We'll need to re-simulate the star now since this new building will affect the ability to
  # build other things as well. It'll also let us calculate the exact end time of this build.
  star_pb = sector.getStar(colony_pb.star_key)
  simulate(star_pb, empire_pb.key)
  updateAfterSimulate(star_pb, empire_pb.key)

  # Schedule a build check so that we make sure we'll update everybody when this build completes
  scheduleBuildCheck()

  ctrl.buildRequestModelToPb(request_pb, build_model)
  return request_pb


def getBuildQueueForEmpire(empire_key):
  '''Gets the current build queue for the given empire.'''
  cache_key = 'buildqueue:for-empire:%s' % empire_key
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
  '''Gets the build queue for *multiple* empires, at the same time.'''
  build_queue_list = []
  for empire_key in empire_keys:
    build_queue_list.append(getBuildQueueForEmpire(empire_key))

  build_queues = pb.BuildQueue()
  for build_queue in build_queue_list:
    build_queues.requests.extend(build_queue.requests)
  return build_queues


def scheduleBuildCheck():
  '''Checks when the next build is due to complete and schedules a task to run at that time.

  Because of the way that tasks a scheduled, it's possible that multiple tasks can be scheduled
  at the same time. That's OK because the task itself is idempotent (its just a waste of resources)
  '''
  query = mdl.BuildOperation.all().order("endTime").fetch(1)
  for build in query:
    # The first one we fetch (because of the ordering) will be the next one. So we'll schedule
    # the build-check to run 5 seconds later (if there's a bunch scheduled at the same time,
    # it's more efficient that way...)
    time = build.endTime + timedelta(seconds=5)

    # TODO: for debugging only...
    in_two_minutes = datetime.now() + timedelta(minutes=2)
    if time > in_two_minutes:
      time = in_two_minutes

    logging.info("Scheduling next build-check at %s" % (time))
    taskqueue.add(queue_name="build",
                  url="/tasks/empire/build-check",
                  method="GET",
                  eta=time)


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
    '''Gets all of the building designs, which we populate from the data/buildings.xml file.'''
    if not BuildingDesign._parsedDesigns:
      BuildingDesign._parsedDesigns = _parseBuildingDesigns()
    return BuildingDesign._parsedDesigns

  @staticmethod
  def getDesign(designId):
    '''Gets the design with the given ID, or None if none exists.'''
    designs = BuildingDesign.getDesigns()
    if designId not in designs:
      return None
    return designs[designId]


class ShipDesign(Design):
  _parsedDesigns = None

  @staticmethod
  def getDesigns():
    '''Gets all of the ship designs, which we populate from the data/ships.xml file.'''
    if not ShipDesign._parsedDesigns:
      ShipDesign._parsedDesigns = _parseShipDesigns()
    return ShipDesign._parsedDesigns

  @staticmethod
  def getDesign(designId):
    '''Gets the design with the given ID, or None if none exists.'''
    designs = ShipDesign.getDesigns()
    if designId not in designs:
      return None
    return designs[designId]


def _parseBuildingDesigns():
  '''Parses the /data/buildings.xml file and returns a list of BuildingDesign objects.'''
  filename = os.path.join(os.path.dirname(__file__), "../data/buildings.xml")
  logging.debug("Parsing %s" % (filename))
  designs = {}
  xml = ET.parse(filename)
  for designXml in xml.iterfind("design"):
    design = _parseBuildingDesign(designXml)
    designs[design.id] = design
  return designs


def _parseShipDesigns():
  '''Parses the /data/ships.xml file and returns a list of ShipDesign objects.'''
  filename = os.path.join(os.path.dirname(__file__), "../data/ships.xml")
  logging.debug("Parsing %s" % (filename))
  designs = {}
  xml = ET.parse(filename)
  for designXml in xml.iterfind("design"):
    design = _parseShipDesign(designXml)
    designs[design.id] = design
  return designs


def _parseBuildingDesign(designXml):
  '''Parses a single <design> from the buildings.xml file.'''
  design = BuildingDesign()
  logging.debug("Parsing building <design id=\"%s\">" % (designXml.get("id")))
  _parseDesign(designXml, design)
  return design


def _parseShipDesign(designXml):
  '''Parses a single <design> from the ships.xml file.'''
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
