

from google.appengine.ext import db


class Writer(db.Model):
  userID = db.StringProperty()
  email = db.StringProperty()
  name = db.StringProperty()


class NewsletterSignup(db.Model):
  email = db.StringProperty()
  display_name = db.StringProperty()
  ip_address = db.StringProperty()
  signup_date = db.DateTimeProperty(auto_now=True)


class Download(db.Model):
  name = db.StringProperty()
  version = db.StringProperty()
  posted = db.DateTimeProperty(auto_now=True)
