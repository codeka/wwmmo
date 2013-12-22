
import os
from datetime import datetime
import json
import logging
import re
import urllib
import webapp2 as webapp

from google.appengine.ext import deferred
from google.appengine.api import memcache
from google.appengine.api import urlfetch
from google.appengine.api import users

import cron
import model.forum


class BasePage(cron.BaseHandler):
  def render(self, tmplName, args):
    if not args:
      args = {}
    super(BasePage, self).render(tmplName, args)


def CountPostsForForum(forum):
  counts = {"forum:"+forum.slug+":posts": 0,
            "forum:"+forum.slug+":threads": 0}
  for forum_thread in model.forum.ForumThread.all().filter("forum", forum):
    counts["forum:"+forum.slug+":threads"] += 1
    counts["thread:"+forum.slug+":"+forum_thread.slug+":posts"] = 0
    for forum_post in model.forum.ForumPost.all().ancestor(forum_thread):
      counts["forum:"+forum.slug+":posts"] += 1
      counts["thread:"+forum.slug+":"+forum_thread.slug+":posts"] += 1

  for name, count in counts.items():
    for shard_counter in model.forum.ForumShardedCounter.all().filter("name", name):
      shard_counter.delete()
    shard_counter = model.forum.ForumShardedCounter(key_name=name+":0", name=name, count=count)
    shard_counter.put()
    keyname = "counter:%s" % (name)
    memcache.set(keyname, count)


class RefreshPostCounts(BasePage):
  def get(self):
    """This is a cron job runs through the entire forum tables and refresh all the post counts etc.

    This cron is usually disabled and run on-demand, since it's quite resource intensive."""
    for forum in model.forum.Forum.all():
      deferred.defer(CountPostsForForum, forum, _queue="forumsync")


app = webapp.WSGIApplication([("/cron/forum/refresh-post-counts", RefreshPostCounts)],
                             debug=os.environ["SERVER_SOFTWARE"].startswith("Development"))
