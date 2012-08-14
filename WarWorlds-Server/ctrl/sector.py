"""sector.py: Contains business logic for the sectors, stars etc."""

import collections
import math

import ctrl
from model import empire as empire_mdl
from model import sector as mdl
from protobufs import warworlds_pb2 as pb


SectorCoord = collections.namedtuple("SectorCoord", ["x", "y"])

SECTOR_SIZE = 1024


def getSectors(coords):
  """Fetches all sectors from in the given array (of SectorCoods)."""

  keys = []
  for coord in coords:
    keys.append("sector:%d,%d" % (coord.x, coord.y))

  sectors = ctrl.getCached(keys, pb.Sector)

  # Figure out which sectors were not in the cache and fetch them from the model instead
  missing_coords = []
  for coord in coords:
    key = "sector:%d,%d" % (coord.x, coord.y)
    if key not in sectors:
      missing_coords.append(coord)
  if len(missing_coords) > 0:
    sectors_model = mdl.SectorManager.getSectors(missing_coords)
    sectors_to_cache = {}
    for key in sectors_model:
      sector_model = sectors_model[key]
      sector_pb = pb.Sector()
      ctrl.sectorModelToPb(sector_pb, sector_model)

      for colony_model in empire_mdl.Colony.getForSector(sector_model):
        colony_pb = sector_pb.colonies.add()
        ctrl.colonyModelToPb(colony_pb, colony_model)

      for fleet_model in empire_mdl.Fleet.getForSector(sector_model):
        fleet_pb = sector_pb.fleets.add()
        ctrl.fleetModelToPb(fleet_pb, fleet_model)

      cache_key = "sector:%d,%d" % (sector_model.x, sector_model.y)
      sectors[cache_key] = sector_pb
      sectors_to_cache[cache_key] = sector_pb

    if len(sectors_to_cache) > 0:
      ctrl.setCached(sectors_to_cache)

  sectors_pb = pb.Sectors()
  sectors_pb.sectors.extend(sectors.values())
  return sectors_pb


def getStar(star_key, force_nocache=False):
  """Gets a star, given it's key."""

  cache_key = "star:%s" % (star_key)
  if not force_nocache:
    values = ctrl.getCached([cache_key], pb.Star)
    if cache_key in values:
      return values[cache_key]

  star_model = mdl.Star.get(star_key)
  if star_model is None:
    return None

  star_pb = pb.Star()
  ctrl.starModelToPb(star_pb, star_model)

  for colony_model in empire_mdl.Colony.getForStar(star_model):
    colony_pb = star_pb.colonies.add()
    ctrl.colonyModelToPb(colony_pb, colony_model)

  for building_model in empire_mdl.Building.getForStar(star_model):
    building_pb = star_pb.buildings.add()
    ctrl.buildingModelToPb(building_pb, building_model)

  for presence_model in empire_mdl.EmpirePresence.getForStar(star_model):
    presence_pb = star_pb.empires.add()
    ctrl.empirePresenceModelToPb(presence_pb, presence_model)

  for build_model in empire_mdl.BuildOperation.getForStar(star_model):
    build_pb = star_pb.build_requests.add()
    ctrl.buildRequestModelToPb(build_pb, build_model)

  for fleet_model in empire_mdl.Fleet.getForStar(star_model):
    fleet_pb = star_pb.fleets.add()
    ctrl.fleetModelToPb(fleet_pb, fleet_model)

  ctrl.setCached({cache_key: star_pb})
  return star_pb


def get_distance_between_stars(star_1_pb, star_2_pb):
  """Returns the distance (in 'parsecs') between two stars.

  We have to do a bit of trigenometry to get the distance between two stars, because they could
  be in different sectors. We know the distance between two sectors (sector sizes are constant)
  so finding the distance between two points within two sectors is a matter of adding a vector
  from star_1 to the sector origin, a vector from the origin of sector_1 to sector_2 and then
  a vector from the origin of sector_2 to star_2. 
  """
  x = -star_1_pb.offset_x
  y = -star_1_pb.offset_y

  x += (star_2_pb.sector_x - star_1_pb.sector_x) * SECTOR_SIZE
  y += (star_2_pb.sector_y - star_1_pb.sector_y) * SECTOR_SIZE

  x += star_2_pb.offset_x
  y += star_2_pb.offset_y

  distance_in_pixels = math.sqrt((x*x) + (y*y))
  return distance_in_pixels / 10.0


def sumarize(star_pb):
  """Returns a new star_pb that contains only the star and planet details.

  We'll strip out colonies, fleets, builds, etc. To make sending this over the wire cheaper
  in cases where that level of detail is not required."""
  summary_pb = pb.Star()
  summary_pb.key = star_pb.key
  summary_pb.sector_x = star_pb.sector_x
  summary_pb.sector_y = star_pb.sector_y
  summary_pb.offset_x = star_pb.offset_x
  summary_pb.offset_y = star_pb.offset_y
  summary_pb.name = star_pb.name
  summary_pb.classification = star_pb.classification
  summary_pb.size = star_pb.size
  summary_pb.planets.MergeFrom(star_pb.planets)
  return summary_pb

