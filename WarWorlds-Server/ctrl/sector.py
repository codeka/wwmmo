

import ctrl
from model import sector as mdl
from model import empire as empire_mdl
import collections
from protobufs import warworlds_pb2 as pb
import logging

SectorCoord = collections.namedtuple('SectorCoord', ['x', 'y'])


class SectorController(ctrl.BaseController):
  def getSectors(self, coords):
    '''Fetches all sectors from in the given array (of SectorCoods).'''
    keys = []
    for coord in coords:
      keys.append('sector:%d,%d' % (coord.x, coord.y))

    sectors = self._getCached(keys, pb.Sector)

    # Figure out which sectors were not in the cache and fetch them from the model instead
    missing_coords = []
    for coord in coords:
      key = 'sector:%d,%d' % (coord.x, coord.y)
      if key not in sectors:
        missing_coords.append(coord)
    if len(missing_coords) > 0:
      sectors_model = mdl.SectorManager.getSectors(missing_coords)
      sectors_to_cache = {}
      for key in sectors_model:
        sector_model = sectors_model[key]
        sector_pb = pb.Sector()
        self._sectorModelToPb(sector_pb, sector_model)

        cache_key = 'sector:%d,%d' % (sector_model.x, sector_model.y)
        sectors[cache_key] = sector_pb
        sectors_to_cache[cache_key] = sector_pb

      if len(sectors_to_cache) > 0:
        self._setCached(sectors_to_cache)

    sectors_pb = pb.Sectors()
    sectors_pb.sectors.extend(sectors.values())
    return sectors_pb

  def getStar(self, star_key):
    '''Gets a star, given it's key.'''
    cache_key = 'star:'+star_key
    values = self._getCached([cache_key], pb.Star)
    if cache_key in values:
      return values[cache_key]

    star_model = mdl.SectorManager.getStar(star_key)
    if star_model is None:
      return None

    star_pb = pb.Star()
    self._starModelToPb(star_pb, star_model)

    for colony_model in empire_mdl.Colony.getForStar(star_model):
      colony_pb = star_pb.colonies.add()
      self._colonyModelToPb(colony_pb, colony_model)

    self._setCached({cache_key: star_pb})
    return star_pb

  def _sectorModelToPb(self, sector_pb, sector_model):
    sector_pb.x = sector_model.x
    sector_pb.y = sector_model.y
    if sector_model.numColonies:
      sector_pb.num_colonies = sector_model.numColonies
    else:
      sector_pb.num_colonies = 0

    for star_model in sector_model.stars:
      star_pb = sector_pb.stars.add()
      self._starModelToPb(star_pb, star_model)

    # TODO: cache?
    for colony_model in empire_mdl.Colony.getForSector(sector_model):
      colony_pb = sector_pb.colonies.add()
      self._colonyModelToPb(colony_pb, colony_model)
