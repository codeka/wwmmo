
import inspect

from google.appengine.api import users
from google.appengine.ext import db

class Type:
  Bug = 'B'
  FeatureRequest = 'FR'
  Process = 'P'

  @staticmethod
  def fromString(str):
    for m in inspect.getmembers(Type):
      if m[1] == str:
        return m[0]
    return Bug

class State:
  New = 'New'
  Open = 'Open'
  Closed = 'Closed'

class Resolution:
  Fixed = 'Fixed'
  WorksForMe = 'WorksForMe'
  Rejected = 'Rejected'
  Duplicate = 'Duplicate' 

class Action:
  Leave = 'Leave'
  Open = 'Open'
  CloseFixed = 'CloseFixed'
  CloseDupe = 'CloseDupe'
  CloseWorksForMe = 'CloseWorksForMe'
  CloseReject = 'CloseReject'

  @staticmethod
  def fromString(str):
    for m in inspect.getmembers(Action):
      if m[1] == str:
        return m[0]
    return Leave


class Issue(db.Model):
  summary = db.StringProperty()
  description = db.TextProperty()
  creator_user = db.UserProperty()
  state = db.StringProperty()
  resolution = db.StringProperty()
  priority = db.IntegerProperty()
  type = db.StringProperty()


class IssueUpdate(db.Model):
  """Every update to an issue is an IssueUpdate. Each time we change the description, summary and so on, add a
  comment, change the status, everything."""
  user = db.UserProperty()
  posted = db.DateTimeProperty()
  comment = db.TextProperty()
  action = db.StringProperty()
  new_priority = db.IntegerProperty()
  new_type = db.StringProperty()

