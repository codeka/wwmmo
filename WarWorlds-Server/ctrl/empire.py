
import ctrl
from model import sector as sector_mdl
from model import empire as mdl
from protobufs import warworlds_pb2 as pb
import logging


class EmpireController(ctrl.BaseController):
  def getEmpireForUser(self, user):
    cache_key = 'empire:for-user:%s' % (user.user_id())
    values = self._getCached([cache_key], pb.Empire)
    if cache_key in values:
      return values[cache_key]

    empire_model = mdl.Empire.getForUser(user)
    empire_pb = pb.Empire()
    self._empireModelToPb(empire_pb, empire_model)
    self._setCached({cache_key: empire_pb})
    return empire_pb

  def getEmpire(self, empire_key):
    cache_key = 'empire:%s' % (empire_key)
    values = self._getCached([cache_key], pb.Empire)
    if cache_key in values:
      return values[cache_key]

    empire_model = mdl.Empire.get(empire_key)
    empire_pb = pb.Empire()
    self._empireModelToPb(empire_pb, empire_model)
    self._setCached({cache_key: empire_pb})
    return empire_pb

  def createEmpire(self, empire_pb):
    empire_model = mdl.Empire()
    self._empirePbToModel(empire_model, empire_pb)
    empire_model.put()

  def getColoniesForEmpire(self, empire_pb):
    cache_key = 'colony:for-empire:%s' % empire_pb.key
    values = self._getCached([cache_key], pb.Colonies)
    if cache_key in values:
      return values[cache_key]

    colony_models = mdl.Colony.getForEmpire(empire_pb.key)
    colonies_pb = pb.Colonies()
    for colony_model in colony_models:
      colony_pb = colonies_pb.colonies.add()
      self._colonyModelToPb(colony_pb, colony_model)

    self._setCached({cache_key: colonies_pb})
    return colonies_pb

  def colonize(self, empire_pb, colonize_request):
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
    self._clearCached(keys)

    colony_pb = pb.Colony()
    self._colonyModelToPb(colony_pb, colony_model)
    return colony_pb
