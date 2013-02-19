"""empire.py: Model for storing data about empires."""

from google.appengine.ext import db

import model.sector as sector_mdl

class Empire(db.Model):
  """Represents an empire, display name and whatnot."""

  displayName = db.StringProperty()
  user = db.UserProperty()
  state = db.IntegerProperty()
  cash = db.FloatProperty()

  class State:
    INITIAL = 1
    REGISTERED = 2
    BANNED = 3

  @staticmethod
  def getForUser(user):
    result = Empire.all().filter("user", user).fetch(1, 0)
    if len(result) != 1:
      return None
    return result[0]


class Colony(db.Model):
  """Represents a colony on a planet. A colony is owned by a single Empire.

  The colony is only "simulated" when a value actually changes. Normally, when we go to display
  a colony's data to the player, we take the value of lastSimulation, then run the simulation
  for all the time between lastSimulation and "now". (it's usually really basic stuff like
  "population = populationRate * (now - lastSimulation)" sort of thing). If you change a property
  on your colony, though, that'll change the various rates. So we need to run a simulation for
  eveything up to the point where the property changes, and save the new value."""

  planet_index = db.IntegerProperty()
  sector = db.ReferenceProperty(sector_mdl.Sector)
  empire = db.ReferenceProperty(Empire)
  population = db.FloatProperty()
  lastSimulation = db.DateTimeProperty()
  focusPopulation = db.FloatProperty()
  focusFarming = db.FloatProperty()
  focusMining = db.FloatProperty()
  focusConstruction = db.FloatProperty()
  uncollectedTaxes = db.FloatProperty()
  cooldownEndTime = db.DateTimeProperty()

  @staticmethod
  def getForEmpire(empire):
    if not isinstance(empire, Empire):
      empire = db.Key(empire)
    query = Colony.all().filter("empire", empire)
    return Colony._getForQuery(query)

  @staticmethod
  def getForSector(sector_model):
    query = Colony.all().filter("sector", sector_model)
    return Colony._getForQuery(query)

  @staticmethod
  def getForStar(star_model):
    query = Colony.all().ancestor(star_model.key())
    return Colony._getForQuery(query)

  @staticmethod
  def _getForQuery(query):
    colonies = []
    for colony in query:
      colonies.append(colony)
    return colonies


class Building(db.Model):
  """A building represents a structure on a colony that gives it certain bonuses and abilities."""

  colony = db.ReferenceProperty(Colony)
  empire = db.ReferenceProperty(Empire)
  designName = db.StringProperty()
  buildTime = db.DateTimeProperty()
  level = db.IntegerProperty()

  @staticmethod
  def getForStar(star_model):
    query = Building.all().ancestor(star_model.key())
    return Building._getForQuery(query)

  @staticmethod
  def _getForQuery(query):
    buildings = []
    for building in query:
      buildings.append(building)
    return buildings


class BuildOperation(db.Model):
  """Request a build operation that is currently in-progress."""

  colony = db.ReferenceProperty(Colony)
  empire = db.ReferenceProperty(Empire)
  designName = db.StringProperty()
  designKind = db.IntegerProperty()
  startTime = db.DateTimeProperty()
  endTime = db.DateTimeProperty()
  progress = db.FloatProperty()
  count = db.IntegerProperty()
  existingBuilding = db.ReferenceProperty(Building)

  @staticmethod
  def getForStar(star_model):
    query = BuildOperation.all().ancestor(star_model.key())
    return BuildOperation._getForQuery(query)

  @staticmethod
  def getForEmpire(empire):
    query = BuildOperation.all().filter("empire", empire)
    return BuildOperation._getForQuery(query)

  @staticmethod
  def _getForQuery(query):
    buildops = []
    for buildop in query:
      buildops.append(buildop)
    return buildops


class EmpirePresence(db.Model):
  """Represents the 'presence' of an empire in a star system."""

  empire = db.ReferenceProperty(Empire)
  totalGoods = db.FloatProperty()
  totalMinerals = db.FloatProperty()

  @staticmethod
  def getForStar(star_model):
    query = EmpirePresence.all().ancestor(star_model.key())
    return EmpirePresence._getForQuery(query)

  @staticmethod
  def _getForQuery(query):
    presences = []
    for presence in query:
      presences.append(presence)
    return presences


class Fleet(db.Model):
  """Represents a fleet of ships."""

  sector = db.ReferenceProperty(sector_mdl.Sector)
  empire = db.ReferenceProperty(Empire)
  designName = db.StringProperty()
  numShips = db.FloatProperty()
  state = db.IntegerProperty()
  stateStartTime = db.DateTimeProperty()
  destinationStar = db.ReferenceProperty(sector_mdl.Star, collection_name="incoming_fleet_set")
  targetFleet = db.SelfReferenceProperty()
  targetColony = db.ReferenceProperty(Colony)
  stance = db.IntegerProperty()
  timeDestroyed = db.DateTimeProperty()
  lastVictory = db.DateTimeProperty()
  eta = db.DateTimeProperty()

  @staticmethod
  def getForStar(star_model):
    query = Fleet.all().ancestor(star_model.key())
    return Fleet._getForQuery(query)

  @staticmethod
  def getForSector(sector_model):
    query = Fleet.all().filter("sector", sector_model)
    return Fleet._getForQuery(query)

  @staticmethod
  def getForEmpire(empire):
    if not isinstance(empire, Empire):
      empire = db.Key(empire)
    query = Fleet.all().filter("empire", empire)
    return Fleet._getForQuery(query)

  @staticmethod
  def _getForQuery(query):
    fleets = []
    for fleet in query:
      fleets.append(fleet)
    return fleets


class ScoutReport(db.Model):
  """A scout report is made by a fleet that has the "scout" effect whenever they land on a star.

  Basically, the report is just a serialized Star protocol buffer, including all colonies,
  fleets, and whatnot "frozen" in time -- at the point the report was made."""
  empire = db.ReferenceProperty(Empire)
  report = db.BlobProperty()
  date = db.DateTimeProperty()

  @staticmethod
  def getReports(star_key, empire_key):
    reports = []
    query = ScoutReport.all().ancestor(star_key).filter("empire", empire_key).order("-date")
    for mdl in query:
      reports.append(mdl)
    return reports


class CombatReport(db.Model):
  """A CombatReport represents the outcome of combat."""
  startTime = db.DateTimeProperty()
  endTime = db.DateTimeProperty()
  startEmpireKeys = db.StringListProperty()
  endEmpireKeys = db.StringListProperty()
  numDestroyed = db.IntegerProperty()
  rounds = db.BlobProperty()

  @staticmethod
  def getReports(star_key):
    reports = []
    query = CombatReport.all().ancestor(star_key).order("-startTime")
    for mdl in query:
      reports.append(mdl)
    return reports


class SituationReport(db.Model):
  """A SituationReport contains a summary of every event that is of interest.

  For example, when there's combat, when a building completes, when a movement complets, etc. This
  model basically just contains a serialized pb.SituationReport protobuf."""
  reportTime = db.DateTimeProperty()
  star = db.ReferenceProperty(sector_mdl.Star)
  report = db.BlobProperty()

  @staticmethod
  def getForEmpire(empire_key, cursor=None):
    query = SituationReport.all().ancestor(db.Key(empire_key)).order("-reportTime")
    if cursor:
      query = query.with_cursor(cursor)
    return SituationReport._getForQuery(query)

  @staticmethod
  def getForStar(empire_key, star_key, cursor=None):
    query = (SituationReport.all().ancestor(db.Key(empire_key))
                            .filter("star", db.Key(star_key)).order("-reportTime"))
    if cursor:
      query = query.with_cursor(cursor)
    return SituationReport._getForQuery(query)

  @staticmethod
  def _getForQuery(query, limit=100):
    sitrep_mdls = []
    for mdl in query:
      sitrep_mdls.append(mdl)
      if len(sitrep_mdls) >= limit:
        break
    return (sitrep_mdls, query.cursor())
