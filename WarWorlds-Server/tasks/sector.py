'''
Created on 01/04/2012

@author: dean@codeka.com.au
'''

from google.appengine.api import taskqueue
from google.appengine.ext import db
from model import sector as mdl
from model import namegen
import collections
import tasks
import logging
import math
import random
import webapp2 as webapp
import os


# This is used to choose a star type at a given point in the map.
_starTypeBonuses = [30, 40, 50, 40, 30, 0, 0]

_starColours = {
    'min': [[0xd0, 0xf0, 0xf0],  # Blue
            [0xe0, 0xeb, 0xc8],  # White
            [0xef, 0xce, 0x59],  # Yellow
            [0xd9, 0xb2, 0x0d],  # Orange
            [0xd9, 0x74, 0x5f],  # Red
            [0x22, 0x91, 0xeb],  # Neutron
            [0x00, 0x43, 0x4f]], # Black Hole
    'max': [[0xdf, 0xff, 0xff],
            [0xff, 0xfb, 0xe0],
            [0xff, 0xe0, 0x6b],
            [0xeb, 0xa4, 0x1f],
            [0xeb, 0x86, 0x71],
            [0x34, 0xa3, 0xdd],
            [0x12, 0x55, 0x62]]
    }

# Bonuses for generating the number of planets around a star (0 & 1 are basically impossible)
_planetCountBonuses = [-9999, -9999, 0, 10, 20, 10, 5, 0]

# Planet type bonuses. The bonuses for each entry need to be added to get the "final" bonus
_planetTypeBonuses = {
              #GasGiant #Radiated #Inferno #Asteroids #Water #Toxic #Desert #Swamp #Terran
    'slot': [[-20,      10,       20,      -20,       -20,   0,     10,     0,     -10], # close to sun
             [-10,      0,        10,      -20,       0,     0,     0,      0,     -10],
             [0,        -10,      -10,     0,         0,     0,     0,      0,     10],
             [10,       -10,      -20,     0,         10,    0,     -10,    10,    10],
             [20,       -20,      -30,     -10,       10,    0,     -20,    10,    5],
             [20,       -20,      -40,     -10,       0,     0,     -30,    0,     5],
             [30,       -20,      -40,     -10,       0,     0,     -30,    0,     0] # far from sun
            ],
    'star': [[-10,      0,        0,       0,         0,     -10,   0,      0,     0], # blue
             [-10,      -5,       -5,      0,         0,     -10,   0,      0,     10], # white
             [-10,      -5,       -10,     0,         0,     -10,   0,      0,     20], # yellow
             [-10,      -5,       -20,     0,         0,     -5,    0,      0,     20], # orange
             [-10,      -5,       -30,     0,         0,     -5,    0,      0,     10], # red
             [-30,      20,       10,      -10,       -10,   0,     -10,    -10,   -30], # neutron
             [-30,      30,       20,      -10,       -20,   0,     -10,    -10,   -30], # black hole
            ]
    }

# Planet size is a normalized random number with the following bonus added. Each planet
# type has a different size "distribution"
_planetSizeBonuses = [
        #GasGiant #Radiated #Inferno #Asteroids #Water #Toxic #Desert #Swamp #Terran
        30,       -10,      0,       0,         10,    -10,   0,      0,     0
    ]

# Planet population is calculated based on the size of the planet (usually, the bigger
# the planet, the higher the potential population) but also the following bonuses are
# applied.
_planetPopulationBonuses = [
        #GasGiant #Radiated #Inferno #Asteroids #Water #Toxic #Desert #Swamp #Terran
        0.4,      0.4,      0.4,     0.0,       0.9,   0.6,   0.9,    0.6,   1.0
    ]

_planetFarmingBonuses = [
        #GasGiant #Radiated #Inferno #Asteroids #Water #Toxic #Desert #Swamp #Terran
        0.4,      0.2,      0.2,     0.0,       1.0,   0.4,   0.6,    0.8,   1.2
    ]

_planetMiningBonuses = [
        #GasGiant #Radiated #Inferno #Asteroids #Water #Toxic #Desert #Swamp #Terran
        0.8,      1.2,      1.0,     1.5,       0.8,   0.4,   0.6,    0.6,   0.8
    ]


def poisson(width, height, min_distance, packing=30):
  '''Generates a poisson distribution of points in a grid.
  
  Args:
    width: The width of the grid
    height: The height of the grid
    min_distance: The minimum distance two points can be from each other
    packing: A value that describes how tightly "packed" the points should be. 30 is
      decent starting value.
  '''

  def generatePointAround(point, min_distance):
    '''Generates a new point around the given centre point and at least min_distance from it.'''
    radius = min_distance * (1+random.random())
    angle = 2*math.pi*random.random()
  
    x, y = point
    x = x + radius * math.cos(angle)
    y = y + radius * math.sin(angle)
  
    return x, y

  def inNeighbourhood(points, point, min_distance, cell_size):
    '''Checks whether any previous point is too close to this new one.'''
  
    def distance(p1, p2):
      x1, y1 = p1
      x2, y2 = p2
      return math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1))

    for _,this_point in enumerate(points):
      dist = distance(point, this_point)
      if dist < min_distance:
        return True
  
    return False

  cell_size= min_distance/math.sqrt(2)
  points = set() # actually a bunch of (x,y) tuple since we're so sparse...
  unprocessed = []

  first_point = random.randint(0, width), random.randint(0, height)
  unprocessed.append(first_point)
  points.add(first_point)

  while unprocessed:
    logging.debug("Unprocess list contains "+str(len(unprocessed))+" items")
    point = unprocessed.pop(random.randint(0, len(unprocessed)-1))
    for _ in range(packing):
      new_point = generatePointAround(point, min_distance)
      x, y = new_point
      if x < 0 or x >= width or y < 0 or y >= height:
        continue

      if inNeighbourhood(points, new_point, min_distance, cell_size):
        continue

      unprocessed.append(new_point)
      points.add(new_point)

  return list(points)


class SectorGenerator:
  '''This class generates a brand new sector. We give it some stars, planets and whatnot.'''

  def __init__(self, x, y):
    self.x = x
    self.y = y

  def generate(self):
    '''Generates a new sector at our (x,y) corrdinate

    Stars are generated within the sector using a Poisson distribution algorithm. For more
    details, see: http://devmag.org.za/2009/05/03/poisson-disk-sampling/
    '''

    # We do this first bit in a transaction to ensure only one sector at the given (x,y) location
    # is ever generated.
    key_name = str(self.x)+","+str(self.y)
    def _tx():
      sector = mdl.Sector.get_by_key_name(key_name)
      if sector is not None:
        # If a sector already exists, return None to indicate that we don't need to 
        # generate a new one.
        return None

      sector = mdl.Sector(key_name=key_name)
      sector.x = self.x
      sector.y = self.y
      sector.numColonies = 0
      sector.stars = []
      sector.put()
      return sector
    sector =  db.run_in_transaction(_tx)
    if sector is None:
      logging.warn("Sector ("+key_name+") already exists.")
      return
    else:
      logging.info("Generating new sector ("+key_name+")...")

    SECTOR_SIZE = 1024

    star_points = poisson(SECTOR_SIZE - 64, SECTOR_SIZE - 64, 160, 80)
    for point in star_points:
      star = mdl.Star()
      star.sector = sector.key()
      x, y = point
      star.x = int(x) + 32
      star.y = int(y) + 32

      starTypeIndex = self._select(_starTypeBonuses)
      star.starType = mdl.star_types[starTypeIndex]
      star.starTypeIndex = starTypeIndex

      star.name = namegen.generate(1)[0]
      star.size = random.randint(16, 24)

      def getColour(starTypeIndex, component):
        c1 = _starColours['min'][starTypeIndex][component]
        c2 = _starColours['max'][starTypeIndex][component]
        return int(c1 + ((c2 - c1) * random.random()))

      r = getColour(starTypeIndex, 0)
      g = getColour(starTypeIndex, 1)
      b = getColour(starTypeIndex, 2)
      star.colour = 0xff000000 | (r << 16) | (g << 8) | b

      sector.stars.append(star)
      star.put()

    for star in sector.stars:
      numPlanets = self._select(_planetCountBonuses)
      for index in range(numPlanets):
        type_bonuses = [sum(elements) for elements in zip(_planetTypeBonuses['slot'][index],
                                                          _planetTypeBonuses['star'][star.starTypeIndex])]
        planet = mdl.Planet()
        planet.star = star.key()
        planet.index = (index+1)
        planet.planetTypeID = self._select(type_bonuses)
        planet.planetType = mdl.planet_types[planet.planetTypeID]

        # A number from 0..100, we actually want it from 10...50 so we adjust
        planet.size = self._normalRandom(100) + _planetSizeBonuses[planet.planetTypeID]
        planet.size = int(10 + (planet.size / 2.5))

        # Population is affected by the size and type of the planet. We need to turn
        # the size into a multiplier
        populationMultiplier = _planetPopulationBonuses[planet.planetTypeID]
        populationMultiplier *= (planet.size * 2.0)/50.0
        planet.populationCongeniality = int(self._normalRandom(1000) * populationMultiplier)

        farmingMultipler = _planetFarmingBonuses[planet.planetTypeID]
        planet.farmingCongeniality = int(self._normalRandom(100) * farmingMultipler)

        miningMultipler = _planetMiningBonuses[planet.planetTypeID]
        planet.miningCongeniality = int(self._normalRandom(100) * miningMultipler)

        planet.put()

  def _select(self, bonuses):
    '''Selects an index from a list of bonuses.

    For example, if you pass in [0,0,0,0], then all four indices are equally like and
    we will return a value in the range [0,4) with equal probability. If you pass in something
    like [0,0,30] then the third item has a "bonus" of 30 and is hence 2 is a far more likely
    result than 0 or 1.'''
    values = []
    for i,bonus in enumerate(bonuses):
      values.append(bonus + self._normalRandom(100))

    maxIndex = 0
    maxValue = 0
    for i,v in enumerate(values):
      if v > maxValue:
        maxIndex = i
        maxValue = v

    return maxIndex

  def _normalRandom(self, maxValue, rounds=10):
    '''Generates a random number that has an approximate normal distribution around the midpoint.

    For example, is maxValue=100 then you'll most get values around 50 and only occasionally 0
    or 100.'''
    n = 0
    step = maxValue/rounds
    for _ in range(rounds):
      n += random.randint(0, step)
    return n


class GeneratePage(tasks.TaskPage):
  '''Simple page that just delegates to SectorGenerator to do the work.'''
  def get(self, sectorX, sectorY):
    sectorX = int(sectorX)
    sectorY = int(sectorY)
    generator = SectorGenerator(sectorX, sectorY)
    generator.generate()


class ExpandUniversePage(tasks.TaskPage):
  '''This is a cron job that is called once per day to "expand" the universe.

  We look at all the sectors which currently have at least one colonized star, then make sure
  the universe has all sectors around the centre generated. We expand the range of the universe
  until we've generated 50 sectors -- that's enough for one day.'''
  def get(self):
    SectorCoords = collections.namedtuple("SectorCoords", ("x", "y"))
    existing_sectors = set()

    # First, we need to figure out the min/max coordinates of all the colonies, which marks
    # the boundary of the "known" universe
    min_x = max_x = min_y = max_y = 0
    for sector in mdl.Sector.all():
      existing_sectors.add(SectorCoords(sector.x, sector.y))
      if sector.numColonies > 0:
        if sector.x < min_x:
          min_x = sector.x
        if sector.x > max_x:
          max_x = sector.x
        if sector.y < min_y:
          min_y = sector.y
        if sector.y > max_y:
          max_y = sector.y

    logging.info("Bounds of known universe: (%d,%d) - (%d,%d)" % (min_x, min_y, max_x, max_y))

    num_generated = 0
    while num_generated < 50:
      for y in range(min_y, max_y):
        for x in range(min_x, max_x):
          coord = SectorCoords(x, y)
          if coord not in existing_sectors:
            logging.info("Generating new sector at (%d,%d)" % (x, y))
            taskqueue.add(url="/tasks/sector/generate/"+str(x)+","+str(y),
                          queue_name="sectors", method="GET")
            num_generated += 1
      min_x -= 1
      min_y -= 1
      max_x += 1
      max_y += 1


app = webapp.WSGIApplication([('/tasks/sector/generate/([0-9-]+),([0-9-]+)', GeneratePage),
                              ('/tasks/sector/expand-universe', ExpandUniversePage)],
                             debug=os.environ['SERVER_SOFTWARE'].startswith('Development'))



