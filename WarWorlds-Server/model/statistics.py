"""statistics.py: Models that hold statistics information"""

from google.appengine.ext import db


class ActiveEmpires(db.Model):
  date = db.DateTimeProperty()
  numDays = db.IntegerProperty()
  actives = db.IntegerProperty()
