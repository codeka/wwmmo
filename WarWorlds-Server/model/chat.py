"""chat.py: Contains data definitions and methods for the chatting subsystem."""

from google.appengine.ext import db

from model import empire as empire_mdl

class ChatMessage(db.Model):
  """A chat message that was sent.

  Chat messages are saved to the data store for later querying and such. We keep them for a
  month before they're backed up and purged (to save data storage...)
  """

  user = db.UserProperty()
  message = db.StringProperty()
  postedDate = db.DateTimeProperty(auto_now=True)
  empire = db.ReferenceProperty(empire_mdl.Empire)
