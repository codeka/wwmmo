

import datetime
import re
import logging
import random

from google.appengine.api import memcache
from google.appengine.ext import db

import ctrl
import model.forum

def getForums():
  """Returns all of the non-alliance-specific forums."""
  keyname = "forums"
  forums = memcache.get(keyname)
  if not forums:
    forums = []
    for forum in model.forum.Forum.all():
      if forum.alliance:
        continue
      forums.append(forum)

    memcache.set(keyname, forums, time=3600)

  return forums


def getAllianceForum(realm_name, alliance):
  """Returns the forum for the given alliance."""
  keyname = "forums:alliance:%s-%d" % (realm_name, alliance.alliance_id)
  forum = memcache.get(keyname)
  if not forum:
    for f in model.forum.Forum.all().filter("alliance", realm_name+":"+str(alliance.alliance_id)).fetch(1):
      forum = f
      break
    if forum:
      memcache.set(keyname, forum, time=3600)
  if not forum:
    # if there's not one, we'll create one
    forum = model.forum.Forum(name=alliance.name, slug="alliance:"+realm_name.lower()+":"+ctrl.makeSlug(alliance.name),
                              description="Private alliance forum for "+alliance.name)
    forum.alliance = realm_name+":"+str(alliance.alliance_id)
    forum.put()
    memcache.set(keyname, forum, time=3600)
  return forum


def getForumBySlug(forum_slug):
  keyname = "forums:%s" % (forum_slug)
  forum = memcache.get(keyname)
  if not forum:
    for f in model.forum.Forum.all().filter("slug", forum_slug).fetch(1):
      forum = f
      break

    if forum:
      memcache.set(keyname, forum, time=3600)

  return forum


def isModerator(forum, user):
  """Determines whether the given user is a moderator of this forum or not."""
  if not forum.moderators:
    return False
  if not user:
    return False
  for mod in forum.moderators:
    if mod.user_id() == user.user_id():
      return True
  return False


def getThreads(forum, page_no, page_size):
  keyname = 'forum:threads:%s:%d:%d' % (str(forum.key()), page_no, page_size)
  threads = memcache.get(keyname)
  if not threads:
    query = model.forum.ForumThread.all().filter("forum", forum)
    query = query.order("-last_post")

    if page_no == 0:
      it = query.run(limit=page_size)
    else:
      cursor = ctrl.findCursor(query, "forum-threads:%s" % (str(forum.key())), page_no, page_size)
      it = query.with_cursor(cursor)

    threads = []
    for thread in it:
      threads.append(thread)
      if len(threads) >= page_size:
        break

    memcache.set(keyname, threads)

  return threads


def getTopThreadsPerForum(forums):
  """For each forum, returns the 'top' thread, which we'll display in the forum list page.

  The 'top' thread is the most-recently created thread. When you click through to the forum, the
  top thread will actually be the thread with the most recent reply, so it's slightly different."""
  keynames = []
  for forum in forums:
    keynames.append("forum:%s:top-thread" % (forum.slug))
  cache_mapping = memcache.get_multi(keynames)

  # fetch any from the data store that weren't cached
  for forum in forums:
    keyname = "forum:%s:top-thread" % (forum.slug)
    if keyname not in cache_mapping:
      query = model.forum.ForumThread.all().filter("forum", forum).order("-posted").fetch(1)
      for forum_thread in query:
        cache_mapping[keyname] = forum_thread
        break

  memcache.set_multi(cache_mapping)

  # convert from our (internal) memcache key names to a more reasonable key
  top_threads = {}
  for forum in forums:
    keyname = "forum:%s:top-thread" % (forum.slug)
    if keyname in cache_mapping:
      top_threads[forum.slug] = cache_mapping[keyname]

  return top_threads


def getLastPostsByForumThread(forum_threads):
  """For each thread in the given list, returns the most recent post in that thread."""
  keynames = []
  for forum_thread in forum_threads:
    keynames.append("forum:%s:%s:last-post" % (forum_thread.forum.slug, forum_thread.slug))
  cache_mapping = memcache.get_multi(keynames)

  for forum_thread in forum_threads:
    keyname = "forum:%s:%s:last-post" % (forum_thread.forum.slug, forum_thread.slug)
    if keyname not in cache_mapping:
      query = model.forum.ForumPost.all().ancestor(forum_thread).order("-posted").fetch(1)
      for post in query:
        cache_mapping[keyname] = post
        break

  memcache.set_multi(cache_mapping)

  last_posts = {}
  for forum_thread in forum_threads:
    keyname = "forum:%s:%s:last-post" % (forum_thread.forum.slug, forum_thread.slug)
    if keyname in cache_mapping:
      last_posts[forum_thread.key()] = cache_mapping[keyname]
  return last_posts


def getFirstPostsByForumThread(forum_threads):
  """For each thread in the given list, returns the first post in that thread (i.e. the one that was originally posted by the created of the thread)."""
  keynames = []
  for forum_thread in forum_threads:
    keynames.append("forum:%s:%s:first-post" % (forum_thread.forum.slug, forum_thread.slug))
  cache_mapping = memcache.get_multi(keynames)

  for forum_thread in forum_threads:
    keyname = "forum:%s:%s:first-post" % (forum_thread.forum.slug, forum_thread.slug)
    if keyname not in cache_mapping:
      query = model.forum.ForumPost.all().ancestor(forum_thread).order("posted").fetch(1)
      for post in query:
        cache_mapping[keyname] = post
        break

  memcache.set_multi(cache_mapping)

  first_posts = {}
  for forum_thread in forum_threads:
    keyname = "forum:%s:%s:first-post" % (forum_thread.forum.slug, forum_thread.slug)
    if keyname in cache_mapping:
      first_posts[forum_thread.key()] = cache_mapping[keyname]
  return first_posts


def getThreadBySlug(forum, forum_thread_slug):
  keyname = "forum:thread:%s:%s" % (forum.slug, forum_thread_slug)
  forum_thread = memcache.get(keyname)
  if not forum_thread:
    for ft in model.forum.ForumThread.all().filter("forum", forum).filter("slug", forum_thread_slug).fetch(1):
      forum_thread = ft
      break

    if forum_thread:
      memcache.set(keyname, forum_thread, time=3600)

  return forum_thread


def getPosts(forum, forum_thread, page_no, page_size):
  keyname = 'forum:posts:%s:%d:%d' % (str(forum_thread.key()), page_no, page_size)
  posts = memcache.get(keyname)
  if not posts:
    query = model.forum.ForumPost.all().ancestor(forum_thread)
    query = query.order("posted")

    if page_no == 0:
      it = query.run(limit=page_size)
    else:
      cursor = ctrl.findCursor(query, "forum-posts:%s" % (str(forum_thread.key())), page_no, page_size)
      it = query.with_cursor(cursor)

    posts = []
    for post in it:
      posts.append(post)
      if len(posts) >= page_size:
        break

    memcache.set(keyname, posts)

  return posts


def getForumThreadPostCounts():
  """Helper method that returns a mapping of all the forum thread/post counts.

  This is more efficient than calling getCount() for each one individually. It's
  used by the "forum list" page to display the list of forums."""
  keyname = "counter:forum-thread-post-counts"
  counts = memcache.get(keyname)
  if not counts:
    counts = {}
    for counter in (model.forum.ForumShardedCounter.all().filter("name >=", "forum")
                                                         .filter("name <", "forum\ufffd")):
      first_colon = counter.name.find(":")
      last_colon = counter.name.rfind(":")                                    
      parts = [counter.name[:first_colon], counter.name[first_colon+1:last_colon], counter.name[last_colon+1:]]
      if parts[1] not in counts:
        counts[parts[1]] = {}
      if parts[2] not in counts[parts[1]]:
        counts[parts[1]][parts[2]] = counter.count
      else:
        counts[parts[1]][parts[2]] += counter.count

    memcache.set(keyname, counts, 3600)

  return counts


def getThreadPostCounts(forum_threads):
  """Helper for retrieving the post count of a list of threads.

  This is more efficient than calling getCounter() on each on individually."""
  keynames = []
  for forum_thread in forum_threads:
    keynames.append("counter:thread:%s:%s:posts" % (forum_thread.forum.slug, forum_thread.slug))
  counts = memcache.get_multi(keynames)

  for forum_thread in forum_threads:
    counter_name = "thread:%s:%s:posts" % (forum_thread.forum.slug, forum_thread.slug)
    keyname = "counter:%s" % (counter_name)
    if keyname not in counts:
      count = 0
      for counter in model.forum.ForumShardedCounter.all().filter("name", counter_name):
        count += counter.count

      counts[keyname] = count
      memcache.set(keyname, count)

  post_counts = {}
  for forum_thread in forum_threads:
    keyname = "counter:thread:%s:%s:posts" % (forum_thread.forum.slug, forum_thread.slug)
    post_counts["%s:%s" % (forum_thread.forum.slug, forum_thread.slug)] = counts[keyname]
  return post_counts


def getCount(counter_name):
  """Gets the value of the given counter.

  For example, "forum:slug:posts" gives the number of posts in the forum with the slug
  "slug". This uses the sharded counter to store the count more efficiently."""
  keyname = "counter:%s" % (counter_name)
  count = memcache.get(keyname)
  if not count:
    count = 0
    for counter in model.forum.ForumShardedCounter.all().filter("name", counter_name):
      count += counter.count

    memcache.set(keyname, count)

  return count


def incrCount(counter_name, num_shards=20, amount=1):
  """Increments the given counter by one.

  See getCount() for example of the counter_names."""
  def _tx():
    if num_shards == 1:
      index = 0
    else:
      index = random.randint(0, num_shards - 1)
    shard_name = counter_name+":"+str(index)
    counter = model.forum.ForumShardedCounter.get_by_key_name(shard_name)
    if not counter:
      counter = model.forum.ForumShardedCounter(key_name=shard_name,
                                                name=counter_name)

    counter.count += amount
    counter.put()

  db.run_in_transaction(_tx)
  keyname = "counter:%s" % (counter_name)
  memcache.incr(keyname)
