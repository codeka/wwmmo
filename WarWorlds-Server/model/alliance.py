"""alliance.py: Models relating to the alliance system."""

from google.appengine.ext import db

from model import empire as empire_mdl

class Alliance(db.Model):
  """The containing class for an alliance."""
  creator = db.ReferenceProperty(empire_mdl.Empire)
  createdDate = db.DateTimeProperty()
  name = db.StringProperty()
  numMembers = db.IntegerProperty()


class AllianceMember(db.Model):
  """Describes a member of an alliance."""
  empire = db.ReferenceProperty(empire_mdl.Empire)
  joinDate = db.DateTimeProperty()


class AllianceJoinRequest(db.Model):
  """Represents a request to join an alliance."""
  empire = db.ReferenceProperty(empire_mdl.Empire)
  message = db.StringProperty()
  requestDate = db.DateTimeProperty()
  status = db.IntegerProperty()
