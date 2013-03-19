"""sector.py: Contains business logic for the sectors, stars etc."""

import collections
from datetime import datetime, timedelta
import logging
import math
import random

from google.appengine.api import memcache

import ctrl
from ctrl import designs
from model import empire as empire_mdl
from model import sector as mdl
from protobufs import messages_pb2 as pb

from google.appengine.ext import db


SectorCoord = collections.namedtuple("SectorCoord", ["x", "y"])

SECTOR_SIZE = 1024


def getSectors(coords, gen=True):
  """Fetches all sectors from in the given array (of SectorCoods).

  Args:
    coords: The list of coordinates to fetch.
    gen: If True, we'll generate missing sectors. If False we don't.
  """

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
    sectors_model = mdl.SectorManager.getSectors(missing_coords, gen)
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
    if star_pb.last_simulation == 0:
      star_pb.last_simulation = ctrl.dateTimeToEpoch(colony_model.lastSimulation)

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
    if fleet_model.numShips <= 0.0:
      continue
    fleet_pb = star_pb.fleets.add()
    ctrl.fleetModelToPb(fleet_pb, fleet_model)

  # set up the initial max_population on the colonies, based on their planet's
  # population congeniality
  for colony_pb in star_pb.colonies:
    planet_pb = star_pb.planets[colony_pb.planet_index - 1]
    colony_pb.max_population = int(planet_pb.population_congeniality)

  # apply affects of any buildings
  for building_pb in star_pb.buildings:
    building_empire_key = None
    design = designs.BuildingDesign.getDesign(building_pb.design_name)
    for effect in design.getEffects(level=building_pb.level):
      for colony_pb in star_pb.colonies:
        if building_pb.colony_key == colony_pb.key:
          effect.applyToColony(building_pb, colony_pb)
          building_empire_key = colony_pb.empire_key
      if building_empire_key:
        for empire_presence_pb in star_pb.empires:
          if empire_presence_pb.empire_key == building_empire_key:
            effect.applyToEmpirePresence(building_pb, empire_presence_pb)

  min_time_emptied = ctrl.dateTimeToEpoch(datetime.now() - timedelta(days=4))
  if not star_pb.colonies and star_pb.time_emptied < min_time_emptied and len(star_pb.planets) > 0:
    _addNativeColonies(star_pb)
    # call getStar again to make sure we get the latest of everything
    return getStar(star_pb.key)

  ctrl.setCached({cache_key: star_pb})
  return star_pb


def getStars(search_query):
  """Searches for stars based on the given query (by default, name of the star)."""
  stars_pb = pb.Stars()
  # try to get it by key first
  try:
    star_mdl = mdl.Star.get(search_query)
    star_pb = stars_pb.stars.add()
    ctrl.starModelToPb(star_pb, star_mdl)
  except:
    query = (mdl.Star.all().filter("name >=", search_query)
                           .filter("name <", search_query + u"\ufffd"))
    for star_mdl in query:
      star_pb = stars_pb.stars.add()
      ctrl.starModelToPb(star_pb, star_mdl)
  return stars_pb


def renameStar(star_pb, rename_request_pb):
  """Renames a star, the rename_request_pb is a pb.StarRenameRequest."""
  star_mdl = mdl.Star.get(db.Key(star_pb.key))
  star_mdl.name = rename_request_pb.new_name
  star_mdl.put()
  ctrl.clearCached(["star:%s" % (star_pb.key),
                    "sector:%d,%d" % (star_pb.sector_x, star_pb.sector_y)])

  star_rename_mdl = mdl.StarRename()
  star_rename_mdl.star = db.Key(star_pb.key)
  star_rename_mdl.oldName = rename_request_pb.old_name
  star_rename_mdl.newName = rename_request_pb.new_name
  star_rename_mdl.purchaseOrderId = rename_request_pb.purchase_order_id
  try:
    star_rename_mdl.purchaseTime = ctrl.epochToDateTime(rename_request_pb.purchase_time)
  except ValueError:
    pass # ignore errors here..?
  star_rename_mdl.purchaseDeveloperPayload = rename_request_pb.purchase_developer_payload
  star_rename_mdl.purchasePrice = rename_request_pb.purchase_price
  star_rename_mdl.put()


def swapStars(star1_pb, star2_pb):
  """Swaps the location of the two stars on the sector map."""
  star1_mdl = mdl.Star.get(db.Key(star1_pb.key))
  star2_mdl = mdl.Star.get(db.Key(star2_pb.key))
  tmp = star1_mdl.sector
  star1_mdl.sector = star2_mdl.sector
  star2_mdl.sector = tmp
  tmp = star1_mdl.x
  star1_mdl.x = star2_mdl.x
  star2_mdl.x = tmp
  tmp = star1_mdl.y
  star1_mdl.y = star2_mdl.y
  star2_mdl.y = tmp
  star1_mdl.put()
  star2_mdl.put()

  sector1_mdl = star1_mdl.sector
  sector2_mdl = star2_mdl.sector

  # if the sectors are changed, we have to update anything else that references the sectors
  if sector1_mdl.key() != sector2_mdl.key():
    logging.debug("Sectors have changed, moving fleets")
    for fleet_pb in star1_pb.fleets:
      fleet_mdl = empire_mdl.Fleet.get(db.Key(fleet_pb.key))
      fleet_mdl.sector = sector1_mdl
      fleet_mdl.put()
    for fleet_pb in star2_pb.fleets:
      fleet_mdl = empire_mdl.Fleet.get(db.Key(fleet_pb.key))
      fleet_mdl.sector = sector2_mdl
      fleet_mdl.put()
    logging.debug("... and colonies")
    for colony_pb in star1_pb.colonies:
      colony_mdl = empire_mdl.Colony.get(db.Key(colony_pb.key))
      colony_mdl.sector = sector1_mdl
      colony_mdl.put()
    for colony_pb in star2_pb.colonies:
      colony_mdl = empire_mdl.Colony.get(db.Key(colony_pb.key))
      colony_mdl.sector = sector2_mdl
      colony_mdl.put()

  # this may be a bit heavy-handed, but it's easier... hopefully we won't be swapping stars
  # all that often!
  mc = memcache.Client()
  mc.flush_all()


def _addNativeColonies(star_pb):
  """Add some 'native' colonies, which are basically passive NPCs that you can "practise" on."""
  from ctrl import empire as empire_ctl

  # first, rank the planets by how good they are for a colony
  def planetRank(a, b):
    apop = int(a.population_congeniality / 100)
    bpop = int(b.population_congeniality / 100)
    if apop != bpop:
      return int(apop - bpop)

    acong = a.population_congeniality + a.mining_congeniality + a.farming_congeniality
    bcong = b.population_congeniality + b.mining_congeniality + b.farming_congeniality
    return int(acong - bcong)

  planets = sorted(star_pb.planets, planetRank)

  sector_key = mdl.SectorManager.getSectorKey(star_pb.sector_x, star_pb.sector_y)

  if len(planets) <= 0:
    logging.warn("Star %s (%s) has no planets(!)" % (star_pb.name, star_pb.key))
    return
  else:
    num_colonies = random.randint(1, min(4, len(planets) - 1))
  for n in range(num_colonies):
    colony_model = empire_ctl._colonize(sector_key,
                                        None, star_pb, planets[n].index)
    colony_model.lastSimulation = datetime.now() - timedelta(hours=24)
    colony_model.focusPopulation = 0.5
    colony_model.focusFarming = 0.5
    colony_model.focusMining = 0.0
    colony_model.focusConstruction = 0.0
    colony_model.put()

  num_fleets = random.randint(1, 5)
  for n in range(num_fleets):
    fleet_model = empire_mdl.Fleet(parent=db.Key(star_pb.key))
    fleet_model.empire = None
    fleet_model.sector = sector_key
    fleet_model.designName = "fighter" # TODO
    fleet_model.numShips = random.randint(1, 5) * 5.0
    fleet_model.state = pb.Fleet.IDLE
    fleet_model.stance = pb.Fleet.AGGRESSIVE
    fleet_model.stateStartTime = datetime.now()
    fleet_model.put()

  ctrl.clearCached(["star:%s" % (star_pb.key)])


def getDistanceBetweenStars(star_1_pb, star_2_pb):
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


def sumarize(star_pb, summary_pb=None):
  """Returns a new star_pb that contains only the star and planet details.

  We'll strip out colonies, fleets, builds, etc. To make sending this over the wire cheaper
  in cases where that level of detail is not required."""
  if not summary_pb:
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

