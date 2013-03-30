"""statistics.py: Models that hold statistics information"""

from google.appengine.ext import db

from model import empire as empire_mdl


class ActiveEmpires(db.Model):
  date = db.DateTimeProperty()
  numDays = db.IntegerProperty()
  actives = db.IntegerProperty()


class EmpireRank(db.Model):
  empire = db.ReferenceProperty(empire_mdl.Empire)
  totalShips = db.IntegerProperty()
  totalStars = db.IntegerProperty()
  totalColonies = db.IntegerProperty()
  totalBuildings = db.IntegerProperty()
  rank = db.IntegerProperty()
  lastRank = db.IntegerProperty()


class StandingQuery(db.Model):
  """A StandingQuery is generally some kind of counter that's updated in real-time.

  For example, we keep a StandingQuery that contains the counts of all the building types
  your empire has, which is used to determine whether you can build a HQ and stuff like that."""
  protobuf = db.BlobProperty()

  # group name is used for all queries of the same 'kind', which lets us more easily
  # delete them and re-create them as required.
  groupName = db.StringProperty()
