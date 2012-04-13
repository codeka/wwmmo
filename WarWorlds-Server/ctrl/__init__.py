

from model import empire as empire_mdl
from google.appengine.api import users
from google.appengine.api import memcache
import calendar
import logging


def getCached(keys, ProtoBuffClass):
  mc = memcache.Client()
  values = mc.get_multi(keys, for_cas=True)
  real_values = {}
  for key in values:
    real_value = ProtoBuffClass()
    real_value.ParseFromString(values[key])
    real_values[key] = real_value
  return real_values


def setCached(mapping):
  serialized_mapping = {}
  for key in mapping:
    serialized_mapping[key] = mapping[key].SerializeToString()

  mc = memcache.Client()
  mc.set_multi(serialized_mapping)


def clearCached(keys):
  mc = memcache.Client()
  mc.delete_multi(keys)


def empireModelToPb(empire_pb, empire_model):
  empire_pb.key = str(empire_model.key())
  empire_pb.display_name = empire_model.displayName
  empire_pb.user = empire_model.user.user_id()
  empire_pb.email = empire_model.user.email()
  empire_pb.state = empire_model.state


def empirePbToModel(empire_model, empire_pb):
  empire_model.displayName = empire_pb.display_name
  if empire_pb.HasField('email'):
    empire_model.user = users.User(empire_pb.email)
  empire_model.state = empire_pb.state


def colonyModelToPb(colony_pb, colony_model):
  colony_pb.key = str(colony_model.key())
  colony_pb.empire_key = str(empire_mdl.Colony.empire.get_value_for_datastore(colony_model))
  colony_pb.star_key = str(empire_mdl.Colony.star.get_value_for_datastore(colony_model))
  colony_pb.planet_key = str(empire_mdl.Colony.planet.get_value_for_datastore(colony_model))
  colony_pb.population = colony_model.population
  colony_pb.last_simulation = int(dateTimeToEpoch(colony_model.lastSimulation))


def sectorModelToPb(sector_pb, sector_model):
  sector_pb.x = sector_model.x
  sector_pb.y = sector_model.y
  if sector_model.numColonies:
    sector_pb.num_colonies = sector_model.numColonies
  else:
    sector_pb.num_colonies = 0

  for star_model in sector_model.stars:
    star_pb = sector_pb.stars.add()
    starModelToPb(star_pb, star_model)

  # TODO: cache?
  for colony_model in empire_mdl.Colony.getForSector(sector_model):
    colony_pb = sector_pb.colonies.add()
    colonyModelToPb(colony_pb, colony_model)


def starModelToPb(star_pb, star_model):
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
      planetModelToPb(planet_pb, planet_model)
    star_pb.num_planets = len(star_model.planets)


def planetModelToPb(planet_pb, planet_model):
  planet_pb.key = str(planet_model.key())
  planet_pb.index = planet_model.index
  planet_pb.planet_type = planet_model.planetTypeID + 1
  planet_pb.size = planet_model.size
  planet_pb.population_congeniality = planet_model.populationCongeniality
  planet_pb.farming_congeniality = planet_model.farmingCongeniality
  planet_pb.mining_congeniality = planet_model.miningCongeniality


def dateTimeToEpoch(dt):
  return calendar.timegm(dt.timetuple())

