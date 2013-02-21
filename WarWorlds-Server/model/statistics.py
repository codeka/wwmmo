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
