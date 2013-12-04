
from google.appengine.ext import db


class DocPage(db.Model):
  """The 'high level' document object."""
  title = db.StringProperty(required=True)
  slug = db.StringProperty(required=True)


class DocPageRevision(db.Model):
  """A revision of a document, store along with the document itself."""
  user = db.UserProperty(required=True)
  date = db.DateTimeProperty(required=True)
  content = db.TextProperty(required=True)


