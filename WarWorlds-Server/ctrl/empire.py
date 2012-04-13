
import ctrl
from model import sector as sector_mdl
from model import empire as mdl
from protobufs import warworlds_pb2 as pb
import logging
from xml.etree import ElementTree as ET
from datetime import datetime


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
  build_model = mdl.BuildOperation()
  build_model.colony = colony_pb.key
  build_model.empire = empire_pb.key
  build_model.star = colony_pb.star_key
  build_model.templateName = request_pb.template_name
  build_model.startTime = datetime.now()
  
  

class BuildingTemplate(object):
  _parsedTemplates = None

  @staticmethod
  def getTemplates():
    '''Gets all of the building templates that are available to be built.

    We'll parse the /static/data/buildings.xml file, which contains all of the data about buildings
    that players can build.'''
    if not BuildingTemplate._parsedTemplates:
      BuildingTemplate._parsedTemplates = _parseBuildingTemplates()
    return BuildingTemplate._parsedTemplates

def _parseBuildingTemplates():
  '''Parses the /static/data/buildings.xml file and returns a list of BuildingTemplate objects.'''
  