

from google.appengine.ext import db

class Ping(db.Model):
  date = db.DateTimeProperty()
  response_time = db.IntegerProperty()
  response_status = db.IntegerProperty()
  error = db.TextProperty()
