'''
Created on 18/02/2012

@author: dean@codeka.com.au
'''

from google.appengine.ext import db
import logging
import random
import math
import namegen


class StarType:
  def __init__(self, colourName="", colourValue=0x0, bonus=0):
    self.colourName = colourName
    self.colourValue = colourValue
    self.bonus = bonus


_starTypes = [StarType(colourName="Blue", colourValue=0xffddffff, bonus=30),
              StarType(colourName="White", colourValue=0xfffffbd8, bonus=40),
              StarType(colourName="Yellow", colourValue=0xffffde69, bonus=50),
              StarType(colourName="Orange", colourValue=0xffe9a21d, bonus=40),
              StarType(colourName="Red", colourValue=0xffe9846f, bonus=30),
              StarType(colourName="Neutron", colourValue=0xff32a1db, bonus=0),
              StarType(colourName="Blackhole", colourValue=0xff10535f, bonus=0)
             ]


class PlanetType:
  def __init__(self, name="", minSize=5, maxSize=50):
    self.name = name
    self.minSize = minSize
    self.maxSize = maxSize


# This is the description of different types of planets. The order is important and
# must match what is defined for Planet.PLANET_TYPE in warworlds.proto
_planetTypes = [PlanetType(name="Gas Giant", minSize=40, maxSize=50),
                PlanetType(name="Radiated", minSize=5, maxSize=30),
                PlanetType(name="Inferno", minSize=5, maxSize=30),
                PlanetType(name="Asteroids", minSize=50, maxSize=50),
                PlanetType(name="Water", minSize=20, maxSize=40),
                PlanetType(name="Toxic", minSize=10, maxSize=20),
                PlanetType(name="Desert", minSize=10, maxSize=30),
                PlanetType(name="Swamp", minSize=10, maxSize=30),
                PlanetType(name="Terran", minSize=10, maxSize=30)
               ]


class Sector(db.Model):
  x = db.IntegerProperty()
  y = db.IntegerProperty()


class Star(db.Model):
  sector = db.ReferenceProperty(Sector)
  name = db.StringProperty()
  starTypeIndex = db.IntegerProperty()
  colour = db.IntegerProperty()
  size = db.IntegerProperty()
  x = db.IntegerProperty()
  y = db.IntegerProperty()
  planets = None


class Planet(db.Model):
  star = db.ReferenceProperty(Star)
  index = db.IntegerProperty()
  planetTypeID = db.IntegerProperty(name="planetType")
  planetType = None # Will be filled in with a PlanetType instance
  size = db.IntegerProperty()


class SectorManager:
  ''''Manages' the sectors, stars, planets, etc. Lets you fetch them, update them, etc

  The most important job of the SectorManager is to maintain consistency of the database. It also
  helps with caching and building new sectors when they come into view for the first time.'''
  @staticmethod
  def getSector(x, y):
    '''Gets the sector at the given (x,y) coordinates.'''
    sectors = SectorManager.getSectors(x, y, x+1, y+1)
    return sectors[SectorManager._getSectorKey(x, y)]

  @staticmethod
  def getSectors(x1, y1, x2, y2):
    '''Gets all of the sectors in the rectangle defined by (x1, y1)(x2,y2)

    Sectors are return inclusive of (x1,y1) and exclusive of (x2,y2).'''

    sectors = {}
    for x in range(x1, x2):
      # Because of limitation in App Engine's filters, we can't search for all sectors
      # where x between x1,x2 AND y between y1,y2 in the same query. So we need to run
      # multiple queries like this....
      query = Sector.all()
      query = query.filter("y >=", y1).filter("y <", y2)
      query = query.filter("x =", x)
      for sector in query:
        sectors[SectorManager._getSectorKey(sector.x, sector.y)] = sector

    for key in sectors:
      sector = sectors[key]
      # fetch all of the sector's stars as well
      query = Star.all().filter("sector", sector)
      sector.stars = []
      for star in query:
        sector.stars.append(star)

    # now for any sectors which they asked for but which weren't in the data store
    # already, we'll need to generate them from scratch...
    for y in range(y1, y2):
      for x in range(x1, x2):
        key = SectorManager._getSectorKey(x, y)
        if key not in sectors:
          generator = SectorGenerator(x, y)
          sectors[key] = generator.generate()

    return sectors

  @staticmethod
  def getStar(starKey):
    '''Gets all of the details about a single star, including planets colonies and so on.

    We don't worry about whether the sector has been generated. If not, we just return None.
    After all, you shouldn't be asking for a star in a sector you haven't visited yet.'''
    star = Star.get(starKey)
    if star is None:
      return None

    planetQuery = Planet.all().filter("star", star.key())
    star.planets = []
    for planet in planetQuery:
      star.planets.append(planet)

    return star

  @staticmethod
  def _getSectorKey(x, y):
    return str(x)+","+str(y)


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
    sector = Sector()
    sector.x = self.x
    sector.y = self.y
    sector.stars = []
    sector.put()

    SECTOR_SIZE = 1024

    star_points = poisson(SECTOR_SIZE, SECTOR_SIZE, SECTOR_SIZE / 8, 60)
    for point in star_points:
      star = Star()
      star.sector = sector.key()
      x, y = point
      star.x = int(x)
      star.y = int(y)

      starTypeIndex = self._select(st.bonus for st in _starTypes)
      starType = _starTypes[starTypeIndex]
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
      numPlanets = random.randint(2, 5)
      for index in range(numPlanets):
        planet = Planet()
        planet.star = star.key()
        planet.index = (index+1)
        planet.planetTypeID = random.randint(0, len(_planetTypes) - 1)
        planet.planetType = _planetTypes[planet.planetTypeID]
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
