
import ctrl
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

  # Make sure we're going to 
  scheduleBuildCheck()

  ctrl.buildRequestModelToPb(request_pb, build_model)
  return request_pb


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
  return design
