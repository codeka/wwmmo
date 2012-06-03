"""sector.py: Contains models relating to sectors, stars, planets, etc."""

from google.appengine.ext import db
from google.appengine.api import taskqueue
import logging


class StarType:
  def __init__(self, colourName="", colourValue=[0xff, 0xff, 0xff]):
    self.colourName = colourName
    self.colourValue = colourValue


star_types = [StarType(colourName="Blue", colourValue=[0xdd, 0xff, 0xff]),
              StarType(colourName="White", colourValue=[0xff, 0xfb, 0xd8]),
              StarType(colourName="Yellow", colourValue=[0xff, 0xde, 0x69]),
              StarType(colourName="Orange", colourValue=[0xe9, 0xa2, 0x1d]),
              StarType(colourName="Red", colourValue=[0xe9, 0x84, 0x6f]),
              StarType(colourName="Neutron", colourValue=[0x32, 0xa1, 0xdb]),
              StarType(colourName="Blackhole", colourValue=[0x10, 0x53, 0x5f])
             ]


class PlanetType:
  def __init__(self, name=""):
    self.name = name


# This is the description of different types of planets. The order is important and
# must match what is defined for Planet.PLANET_TYPE in warworlds.proto
planet_types = [PlanetType(name="Gas Giant"),
                PlanetType(name="Radiated"),
                PlanetType(name="Inferno"),
                PlanetType(name="Asteroids"),
                PlanetType(name="Water"),
                PlanetType(name="Toxic"),
                PlanetType(name="Desert"),
                PlanetType(name="Swamp"),
                PlanetType(name="Terran")
               ]


class Sector(db.Model):
  x = db.IntegerProperty()
  y = db.IntegerProperty()
  numColonies = db.IntegerProperty()


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
  populationCongeniality = db.IntegerProperty()
  farmingCongeniality = db.IntegerProperty()
  miningCongeniality = db.IntegerProperty()


class SectorManager:
  """'Manages' the sectors, stars, planets, etc. Lets you fetch them, update them, etc

  The most important job of the SectorManager is to maintain consistency of the database. It also
  helps with caching and building new sectors when they come into view for the first time."""

  @staticmethod
  def getSector(x, y):
    """Gets the sector at the given (x,y) coordinates."""

    sectors = SectorManager.getSectors(x, y, x+1, y+1)
    return sectors[SectorManager._getSectorKey(x, y)]

  @staticmethod
  def getSectors(coords):
    """Gets all of the sectors with the given range of coordinates."""

    keys = []
    for coord in coords:
      keys.append("%d,%d" % (coord.x, coord.y))

    sectors = {}
    for sector in Sector.get_by_key_name(keys):
      if sector is not None:
        sectors[SectorManager._getSectorKey(sector.x, sector.y)] = sector

    for key in sectors:
      sector = sectors[key]
      # fetch all of the sector's stars as well
      logging.debug("Fetching stars for sector [%s]" % (key))
      query = Star.all().filter("sector", sector)
      sector.stars = []
      for star in query:
        sector.stars.append(star)

    # now for any sectors which they asked for but which weren't in the data store
    # already, we'll need to generate them from scratch...
    for coord in coords:
      key = SectorManager._getSectorKey(coord.x, coord.y)
      if key not in sectors:
        taskqueue.add(url="/tasks/sector/generate/"+str(coord.x)+","+str(coord.y),
                      queue_name="sectors", method="GET")

    return sectors

  @staticmethod
  def getStar(starKey):
    """Gets all of the details about a single star, including planets colonies and so on.

    We don't worry about whether the sector has been generated. If not, we just return None.
    After all, you shouldn't be asking for a star in a sector you haven't visited yet."""

    star = Star.get(starKey)
    if star is None:
      return None

    planetQuery = Planet.all().filter("star", star)
    star.planets = []
    for planet in planetQuery:
      star.planets.append(planet)

    return star

  @staticmethod
  def _getSectorKey(x, y):
    return str(x)+","+str(y)


