
from google.appengine.api import users
from google.appengine.ext import db


class Forum(db.Model):
  name = db.StringProperty(required=True)
  slug = db.StringProperty(required=True)
  description = db.TextProperty(required=True)
  alliance = db.StringProperty()

  # the list of moderators are people who can edit/delete other people's posts within this forum.
  moderators = db.ListProperty(users.User)

  # this is a list of users who get auto-subscribed to all posts in this forum.
  auto_subscribers = db.ListProperty(users.User)

  # this is the list of users who are allowed to post new topics. Anybody can still reply.
  allowed_posters = db.ListProperty(users.User)


class ForumThread(db.Model):
  forum = db.ReferenceProperty(Forum, required=True)
  posted = db.DateTimeProperty(required=True)
  last_post = db.DateTimeProperty()
  subject = db.StringProperty(required=True)
  slug = db.StringProperty(required=True)
  user = db.UserProperty(required=True)
  is_sticky = db.BooleanProperty()


class ForumPost(db.Model):
  forum = db.ReferenceProperty(Forum, required=True)
  user = db.UserProperty(required=True)
  posted = db.DateTimeProperty(required=True)
  updated = db.DateTimeProperty()
  content = db.TextProperty(required=True)
  edit_notes = db.TextProperty()


class ForumShardedCounter(db.Model):
  """This is a simple sharded counter that contains counts of various posts.

  We use it to count the number of posts in the forum, number of threads in
  a forum, number of replies to a post and so on."""
  name = db.StringProperty(required=True)
  count = db.IntegerProperty(required=True, default=0)


class ForumThreadSubscriber(db.Model):
  """This model contains subscribers to a thread.

  When a thread is posted to, all of the subscribers are notified by email."""
  user = db.UserProperty(required=True)
  forum_thread = db.ReferenceProperty(ForumThread, required=True)
  subscribed = db.DateTimeProperty(required=True)
