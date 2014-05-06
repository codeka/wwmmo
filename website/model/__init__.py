

from google.appengine.ext import db


class Writer(db.Model):
  userID = db.StringProperty()
  email = db.StringProperty()
  name = db.StringProperty()
