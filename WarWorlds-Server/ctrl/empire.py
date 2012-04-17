
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

  # normalize the focus values so that they all add up to 1.0
  focus_total = (updated_colony_pb.focus_population +
                 updated_colony_pb.focus_farming +
                 updated_colony_pb.focus_mining +
                 updated_colony_pb.focus_construction)
  colony_pb.focus_population = updated_colony_pb.focus_population / focus_total
  colony_pb.focus_farming = updated_colony_pb.focus_farming / focus_total
  colony_pb.focus_mining = updated_colony_pb.focus_mining / focus_total
  colony_pb.focus_construction = updated_colony_pb.focus_construction / focus_total

  keys_to_clear = []
  keys_to_clear.append('buildqueue:for-empire:%s' % colony_pb.empire_key)

  # because of the simulation, we have to update all colonies.
  for colony_pb in star_pb.colonies:
    colony_pb.last_simulation = datetime.now()
    colony_model = mdl.Colony.get(colony_pb.key)
    ctrl.colonyPbToModel(colony_model, colony_pb)
    colony_model.put()
    keys_to_clear.append('colony:%s' % colony_pb.key)

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
  Returns:
    We save the new colonies to the data store, and also return the star_pb with updated data.
  '''
  # figure out the start time, which is the oldest last_simulation time
  start_time = 0
  empire_keys = []
  for colony_pb in star_pb.colonies:
    if empire_key and colony_pb.empire_key != empire_key:
      continue
    if colony_pb.empire_key not in empire_keys:
      empire_keys.append(colony_pb.empire_key)
    if start_time == 0 or colony_pb.last_simulation < start_time:
      start_time = colony_pb.last_simulation

  if start_time == 0:
    # No colonies worth simulation...
    return

  start_time = ctrl.epochToDateTime(start_time)
  end_time = datetime.now()

  # we may need to adjust the build queue
  build_queue = getBuildQueuesForEmpires(empire_keys)

  while True:
    step_end_time = start_time + timedelta(minutes=15)
    if step_end_time < end_time:
      _simulateStep(timedelta(minutes=15), star_pb, empire_key, build_queue)
      start_time = step_end_time
    else:
      break

  dt = end_time - start_time
  if dt.total_seconds() > 0:
    _simulateStep(dt, star_pb, empire_key, build_queue)


def _simulateStep(dt, star_pb, empire_key, build_queue):
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
    star_pb: The star protocol buffer we're simulating.
    emprire_key: The key of the empire we're simulating. If None, we'll simulate
        all empires in the starsystem.
    build_queue: The build queue for the empire (or empires).
  '''

  total_goods = 0 # todo: store these in the star
  total_minerals = 0

  dt_in_hours = dt.total_seconds() / 3600.0

  for colony_pb in star_pb.colonies:
    if empire_key and colony_pb.empire_key != empire_key:
      continue

    planet_pb = None
    for pb in star_pb.planets:
      if pb.key == colony_pb.planet_key:
        planet_pb = pb
        break

    # calculate the output from farming this turn and add it to the star global
    goods = colony_pb.population * colony_pb.focus_farming * planet_pb.farming_congeniality
    total_goods += goods * dt_in_hours

    # calculate the output from mining this turn and add it to the star global
    minerals = colony_pb.population * colony_pb.focus_mining * planet_pb.mining_congeniality
    total_minerals += minerals * dt_in_hours

  # As we go, we'll count up the total population of all colonies
  total_population = 0

  # A second loop though the colonies, once the goods/minerals have been calculated. This way,
  # goods minerals are shared between colonies
  for colony_pb in star_pb.colonies:
    if empire_key and colony_pb.empire_key != empire_key:
      continue

    total_population += colony_pb.population

    build_requests = []
    for build_req in build_queue.requests:
      if build_req.colony_key == colony_pb.key:
        build_requests.append(build_req)

    # If we have pending build requests, we'll have to update them as well
    if len(build_requests) > 0:
      total_workers = colony_pb.population * colony_pb.construction_focus
      workers_per_build_request = total_workers / len(build_requests)

      for build_request in build_requests:
        design = BuildingDesign.getDesign(build_request.design_name)

        # So the build time the design specifies is the time to build the structure assigning
        # 100 workers are available. Double the workers and you halve the build time. Halve
        # the workers and you double the build time. 
        total_build_time = design.buildTimeInSeconds / 3600.0
        total_build_time *= (100.0 / workers_per_build_request)

        # Work out how many hours we've spend so far (in hours)
        time_spent = datetime.now() - ctrl.epochToDateTime(build_request.start_time)
        time_spent = time_spent.total_seconds() / 3600.0

        dt_required = dt_in_hours
        if time_spent + dt_required > total_build_time:
          # If we're going to finish on this turn, we only need a fraction of the minerals we'd
          # otherwise use, so make sure dt_required is correct
          dt_required = total_build_time - time_spent

        # work out how many minerals we require for this turn
        minerals_required_per_hour = design.buildCostMinerals / total_build_time
        minerals_required = minerals_required_per_hour / dt_required

        if total_minerals > minerals_required:
          total_minerals -= minerals_required

          # adjust the end_time for this turn
          build_request.end_time = build_request.start_time + (time_spent + dt_required) * 3600.0
          # note if the build request has already finished, we don't actually have to do
          # anything since it'll be fixed up by the tasks/empire/build-check task.

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
      population_increase = colony_pb.population * colony_pb.focus_population
      population_increase *= (goods_efficiency - 0.75) # that is, from -0.75 -> 0.25
      population_increase *= dt_in_hours
      colony_pb.population += int(population_increase)

    # TODO:
    # star_pb.empire_store_pb.total_goods = total_goods
    # star_pb.empire_store_pb.total_minerals = total_minerals

  logging.debug("simulation step complete: dt=%.2f (hrs)" % (dt_in_hours))

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
  design = BuildingDesign.getDesign(request_pb.design_name)
  if not design:
    logging.warn("Asked to build design '%s', which does not exist." % (request_pb.design_name))
    return False

  build_model = mdl.BuildOperation()
  build_model.colony = db.Key(colony_pb.key)
  build_model.empire = db.Key(empire_pb.key)
  build_model.star = db.Key(colony_pb.star_key)
  build_model.designName = request_pb.design_name
  build_model.startTime = datetime.now()
  build_model.endTime = build_model.startTime + timedelta(seconds = design.buildTimeSeconds)
  build_model.put()

  keys = ['buildqueue:for-empire:%s' % empire_pb.key]
  ctrl.clearCached(keys)

  # Make sure we're going to 
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
    logging.info("Scheduling next build-check at %s" % (time))
    taskqueue.add(queue_name="default",
                  url="/tasks/empire/build-check",
                  method="GET",
                  eta=time)


class BuildingDesign(object):
  _parsedDesigns = None

  @staticmethod
  def getDesigns():
    '''Gets all of the building templates that are available to be built.

    We'll parse the /static/data/buildings.xml file, which contains all of the data about buildings
    that players can build.'''
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

def _parseBuildingDesigns():
  '''Parses the /static/data/buildings.xml file and returns a list of BuildingDesign objects.'''
  filename = os.path.join(os.path.dirname(__file__), "../data/buildings.xml")
  logging.debug("Parsing %s" % (filename))
  designs = {}
  xml = ET.parse(filename)
  for buildingXml in xml.iterfind("building"):
    design = _parseBuildingDesign(buildingXml)
    designs[design.id] = design
  return designs

def _parseBuildingDesign(buildingXml):
  '''Parses a single <building> from the buildings.xml file.'''
  design = BuildingDesign()
  logging.debug("Parsing <building id=\"%s\">" % (buildingXml.get("id")))
  design.id = buildingXml.get("id")
  design.name = buildingXml.findtext("name")
  design.description = buildingXml.findtext("description")
  design.icon = buildingXml.findtext("icon")
  costXml = buildingXml.find("cost")
  design.buildCost = costXml.get("credits")
  design.buildTimeSeconds = float(costXml.get("time")) * 3600
  design.buildCostMinerals = costXml.get("minerals")
  return design
