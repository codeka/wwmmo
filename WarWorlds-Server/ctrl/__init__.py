

from model import empire as empire_mdl
from google.appengine.api import users
from google.appengine.api import memcache
import calendar
import logging


class BaseController(object):

  def __init__(self):
    self.mc = memcache.Client()

  def _getCached(self, keys, ProtoBuffClass):
    values = self.mc.get_multi(keys, for_cas=True)
    real_values = {}
    for key in values:
      real_value = ProtoBuffClass()
      real_value.ParseFromString(values[key])
      real_values[key] = real_value
    return real_values

  def _setCached(self, mapping):
    serialized_mapping = {}
    for key in mapping:
      serialized_mapping[key] = mapping[key].SerializeToString()
    self.mc.set_multi(serialized_mapping)

  def _clearCached(self, keys):
    self.mc.delete_multi(keys)

  def _empireModelToPb(self, empire_pb, empire_model):
    empire_pb.key = str(empire_model.key())
    empire_pb.display_name = empire_model.displayName
    empire_pb.user = empire_model.user.user_id()
    empire_pb.email = empire_model.user.email()
    empire_pb.state = empire_model.state

  def _empirePbToModel(self, empire_model, empire_pb):
    empire_model.displayName = empire_pb.display_name
    if empire_pb.HasField('email'):
      empire_model.user = users.User(empire_pb.email)
    empire_model.state = empire_pb.state

  def _colonyModelToPb(self, colony_pb, colony_model):
    colony_pb.key = str(colony_model.key())
    colony_pb.empire_key = str(empire_mdl.Colony.empire.get_value_for_datastore(colony_model))
    colony_pb.star_key = str(empire_mdl.Colony.star.get_value_for_datastore(colony_model))
    colony_pb.planet_key = str(empire_mdl.Colony.planet.get_value_for_datastore(colony_model))
    colony_pb.population = colony_model.population
    colony_pb.last_simulation = int(self._dateTimeToEpoch(colony_model.lastSimulation))

  def _starModelToPb(self, star_pb, star_model):
    star_pb.key = str(star_model.key())
    star_pb.sector_x = star_model.sector.x
    star_pb.sector_y = star_model.sector.y
    star_pb.offset_x = star_model.x
    star_pb.offset_y = star_model.y
    star_pb.name = star_model.name
    star_pb.colour = star_model.colour
    star_pb.classification = star_model.starTypeIndex
    star_pb.size = star_model.size
    if star_model.planets is not None:
      for planet_model in star_model.planets:
        planet_pb = star_pb.planets.add()
        self._planetModelToPb(planet_pb, planet_model)
      star_pb.num_planets = len(star_model.planets)

  def _planetModelToPb(self, planet_pb, planet_model):
    planet_pb.key = str(planet_model.key())
    planet_pb.index = planet_model.index
    planet_pb.planet_type = planet_model.planetTypeID + 1
    planet_pb.size = planet_model.size
    planet_pb.population_congeniality = planet_model.populationCongeniality
    planet_pb.farming_congeniality = planet_model.farmingCongeniality
    planet_pb.mining_congeniality = planet_model.miningCongeniality

  def _dateTimeToEpoch(self, dt):
    return calendar.timegm(dt.timetuple())

