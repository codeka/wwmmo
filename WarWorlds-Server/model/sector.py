'''
Created on 18/02/2012

@author: dean@codeka.com.au
'''

from google.appengine.ext import db
from google.appengine.api import taskqueue

class StarType:
  def __init__(self, colourName="", colourValue=0x0):
    self.colourName = colourName
    self.colourValue = colourValue


star_types = [StarType(colourName="Blue", colourValue=0xffddffff),
              StarType(colourName="White", colourValue=0xfffffbd8),
              StarType(colourName="Yellow", colourValue=0xffffde69),
              StarType(colourName="Orange", colourValue=0xffe9a21d),
              StarType(colourName="Red", colourValue=0xffe9846f),
              StarType(colourName="Neutron", colourValue=0xff32a1db),
              StarType(colourName="Blackhole", colourValue=0xff10535f)
             ]


class PlanetType:
  def __init__(self, name="", minSize=5, maxSize=50):
    self.name = name
    self.minSize = minSize
    self.maxSize = maxSize


# This is the description of different types of planets. The order is important and
# must match what is defined for Planet.PLANET_TYPE in warworlds.proto
planet_types = [PlanetType(name="Gas Giant", minSize=40, maxSize=50),
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
          taskqueue.add(url="/tasks/sector/generate/"+str(x)+","+str(y),
                        queue_name="sectors", method="GET")

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


