
from google.appengine.ext import db



class Post(db.Model):
  '''An actual blog post.'''
  content = db.TextProperty()
  html = db.TextProperty()
  title = db.StringProperty()
  posted = db.DateTimeProperty(auto_now_add=True)
  updated = db.DateTimeProperty(auto_now=True)
  tags = db.StringListProperty()
  slug = db.StringProperty()
  blobs = db.StringListProperty()
  isPublished = db.BooleanProperty()


class Tag(db.Model):
  name = db.StringProperty()
  postCount = db.IntegerProperty()


