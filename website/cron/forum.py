
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


class SyncAlliancesPage(BasePage):
  def get(self):
    """This is a cron job that runs to sync alliance details from the server(s) to the local store."""

    for realm_name, base_url in ctrl.profile.REALMS.items():
      self.SyncAlliancesForRealm(realm_name, base_url)

  def SyncAlliancesForRealm(self, realm_name, base_url):
    url = base_url + "alliances"
    result = urlfetch.fetch(url, headers = {'Accept': 'text/json'})
    if result.status_code == 200:
      alliances = json.loads(result.content)
      if "alliances" in alliances:
        for alliance in alliances["alliances"]:
          self.SyncAlliance(realm_name, alliance)

  def SyncAlliance(self, realm_name, alliance):
    logging.info("Syncing alliance: "+realm_name+": "+alliance["name"])
    model.profile.Alliance.Save(realm_name, alliance)


app = webapp.WSGIApplication([("/cron/forum/refresh-post-counts", RefreshPostCounts)],
                             debug=os.environ["SERVER_SOFTWARE"].startswith("Development"))
