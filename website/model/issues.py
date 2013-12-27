
from google.appengine.api import users
from google.appengine.ext import db


class Issue(db.Model):
  summary = db.StringProperty()
  description = db.TextProperty()
  creator_user = db.UserProperty()


class IssueUpdate(db.Model):
  """Every update to an issue is an IssueUpdate. Each time we change the description, summary and so on, add a
  comment, change the status, everything."""
  user = db.UserProperty()
  posted = db.DateTimeProperty()
  comment = db.TextProperty()
