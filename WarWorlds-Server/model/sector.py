"""sector.py: Contains models relating to sectors, stars, planets, etc."""

import logging

from google.appengine.ext import db
from google.appengine.api import taskqueue

import protobufs.messages_pb2 as pb

class StarType:
  def __init__(self, colourName="", colourValue=[0xff, 0xff, 0xff]):
    self.colourName = colourName
    self.colourValue = colourValue


star_types = [StarType(colourName="Blue", colourValue="blue"),
              StarType(colourName="White", colourValue="white"),
              StarType(colourName="Yellow", colourValue="yellow"),
              StarType(colourName="Orange", colourValue="orange"),
              StarType(colourName="Red", colourValue="red"),
              StarType(colourName="Neutron", colourValue="neutron"),
              StarType(colourName="Blackhole", colourValue="back-hole")
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


class PlanetsProperty(db.Property):
  """This is a custom property for representing a collection of planets.

  Because the planets are only ever accessed from within the context of a star, there's no
  point storing them as separate entities, since that's a huge use of resources (i.e. one
  read/write per planet!).

  Instead, we store them as Planet protocol buffers, and you can look through them directly
  via this property."""

  def get_value_for_datastore(self, model_instance):
    planets = super(PlanetsProperty, self).get_value_for_datastore(model_instance)
    protobuf = pb.Planets()
    protobuf.planets.extend(planets)
    return db.Blob(protobuf.SerializeToString())

  def make_value_from_datastore(self, value):
    protobuf = pb.Planets()
    protobuf.ParseFromString(value)
    planets = []
    for planet in protobuf.planets:
      planets.append(planet)
    return planets

  def validate(self, value):
    value = super(PlanetsProperty, self).validate(value)
    if value is None:
      return value
    if isinstance(value, list):
      for elem in value:
        if not isinstance(elem, pb.Planet):
          raise db.BadValueError("All elements of %s must be of type pb.Planet" % self.name)
      return value

    raise db.BadValueError("Property %s must be a list of pb.Planet" % self.name)

class Sector(db.Model):
  x = db.IntegerProperty()
  y = db.IntegerProperty()
  numColonies = db.IntegerProperty()
  distanceToCentre = db.FloatProperty()


class Star(db.Model):
  sector = db.ReferenceProperty(Sector)
  name = db.StringProperty()
  starTypeID = db.IntegerProperty(name="starType")
  size = db.IntegerProperty()
  x = db.IntegerProperty()
  y = db.IntegerProperty()
  planets = PlanetsProperty()
  timeEmptied = db.DateTimeProperty()
  lastSimulation = db.DateTimeProperty()


class SectorManager:
  """'Manages' the sectors, stars, planets, etc. Lets you fetch them, update them, etc

  The most important job of the SectorManager is to maintain consistency of the database. It also
  helps with caching and building new sectors when they come into view for the first time."""

  @staticmethod
  def getSectorKey(x, y):
    """Gets the db.Key() we can use for the sector at the given (x,y) coordinates."""
    return db.Key.from_path('Sector', SectorManager._getSectorKey(x, y))

  @staticmethod
  def getSectors(coords, gen=True):
    """Gets all of the sectors with the given range of coordinates."""

    keys = []
    for coord in coords:
      keys.append(SectorManager._getSectorKey(coord.x, coord.y))

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
    if gen:
      for coord in coords:
        key = SectorManager._getSectorKey(coord.x, coord.y)
        if key not in sectors:
          taskqueue.add(url="/tasks/sector/generate/"+str(coord.x)+","+str(coord.y),
                        queue_name="sectors", method="GET")

    return sectors

  @staticmethod
  def _getSectorKey(x, y):
    return ("%d,%d" % (x, y))


