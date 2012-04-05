'''
Created on 01/04/2012

@author: dean@codeka.com.au
'''

from google.appengine.api import taskqueue
from model import sector as mdl
from model import namegen
import collections
import tasks
import logging
import math
import random
import webapp2 as webapp

# This is used to choose a star type at a given point in the map.
_starTypeBonuses = [30, 40, 50, 40, 30, 0, 0]

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

    TODO: currently this could result in two sectors being generated at the same (x,y)
    coordinate. We need to do something to avoid that (e.g. specify the key ourselves,
    transactions, something like that).'''

    logging.info("Generating new sector ("+str(self.x)+","+str(self.y)+")...")
    sector = mdl.Sector()
    sector.x = self.x
    sector.y = self.y
    sector.numColonies = 0
    sector.stars = []
    sector.put()

    SECTOR_SIZE = 1024

    star_points = poisson(SECTOR_SIZE - 64, SECTOR_SIZE - 64, 160, 80)
    for point in star_points:
      star = mdl.Star()
      star.sector = sector.key()
      x, y = point
      star.x = int(x) + 32
      star.y = int(y) + 32

      starTypeIndex = self._select(_starTypeBonuses)
      starType = mdl.star_types[starTypeIndex]
      star.colour = starType.colourValue
      star.starTypeIndex = starTypeIndex

      star.name = namegen.generate(1)[0]
      star.size = random.randint(16, 24)

      #r = random.randint(100, 255)
      #g = random.randint(100, 255)
      #b = random.randint(100, 255)
      #star.colour = 0xff000000 | (r << 16) | (g << 8) | b

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
        planet.size = random.randint(planet.planetType.minSize, planet.planetType.maxSize)
        planet.put()

    return sector

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
                             debug=False)



