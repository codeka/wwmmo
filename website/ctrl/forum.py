

import datetime
import re
import logging
import random

from google.appengine.api import memcache
from google.appengine.api import mail
from google.appengine.api import search 
from google.appengine.api import users
from google.appengine.ext import db
from google.appengine.ext import deferred


import ctrl
import ctrl.tmpl
import ctrl.profile
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


def canViewAllianceForum(forum, profile):
  if not forum.alliance:
    return True
  if forum.alliance == profile.realm_name + ":" + str(profile.alliance_id):
    return True
  return False


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


def canPost(forum, user):
  """Determines whether the given user can post to this forum or not."""
  if not forum.allowed_posters:
    return True
  if not user:
    return True
  for poster in forum.allowed_posters:
    if poster.user_id() == user.user_id():
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
    n = 0
    for thread in it:
      n += 1
      if thread.is_sticky:
        # note: stickies will make the page slightly smaller, but it should
        # only ever be one or two anyway...
        continue
      threads.append(thread)
      if n >= page_size:
        break

    memcache.set(keyname, threads)

  return threads


def searchThreads(forum, query, profile):
  index = search.Index("forum")
  if forum:
    query = "forum:" + forum.slug.replace(":", "_") + " " + query
  if not profile or not profile.alliance_id:
    query = "alliance:NA " + query
  else:
    alliance = profile.realm_name+"_"+str(profile.alliance_id)
    query = "(alliance:NA OR alliance:"+alliance+") " + query
  logging.info("Query: "+query)
  results = index.search(search.Query(query_string=query,
        options=search.QueryOptions(ids_only=True)))

  threads = []
  for doc in results:
    threads.append(model.forum.ForumThread.get(doc.doc_id))
  return threads


def getStickyThreads(forum):
  keyname = 'forum:sticky-threads:%s' % (str(forum.key()))
  sticky_threads = memcache.get(keyname)
  if not sticky_threads:
    query = model.forum.ForumThread.all().filter("forum", forum)
    query = query.filter("is_sticky", True)
    query = query.order("-last_post")

    it = query.run()

    sticky_threads = []
    for thread in it:
      sticky_threads.append(thread)

    memcache.set(keyname, sticky_threads)

  return sticky_threads


def toggleSticky(forum, forum_thread):
  forum_thread.is_sticky = not forum_thread.is_sticky
  forum_thread.put()
  memcache.flush_all()


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


def getPostsForUser(profile, include_alliance_posts, page_no, page_size):
  """Gets the posts that the given user has made.

  Args:
    profile The profile of the user whose posts we're after.
    include_alliance_posts: If True, we'll include posts the user has made to their alliance forum."""
  if not profile.user:
    # The user will need to log in to the website in order to set the user.
    return []

  user_id = profile.key().name()
  keyname = 'forum:threads-per-user:%s:%d:%d:%d' % (str(user_id), int(include_alliance_posts), page_no, page_size)
  posts = memcache.get(keyname)
  if not posts:
    query = model.forum.ForumPost.all().filter("user", profile.user)
    if not include_alliance_posts and profile.alliance_id:
      alliance = model.profile.Alliance.Fetch(profile.realm_name, profile.alliance_id)
      if alliance:
        alliance_forum = getAllianceForum(profile.realm_name, alliance)
        query.filter("forum !=", alliance_forum).order("forum")
    query = query.order("-posted")

    if page_no == 0:
      it = query.run(limit=page_size)
    else:
      cursor = ctrl.findCursor(query, "forum-posts-for-user:%s:%d" % (str(user_id), int(include_alliance_posts)), page_no, page_size)
      it = query.with_cursor(cursor)

    posts = []
    for post in it:
      posts.append(post)

    memcache.set(keyname, posts)

  return posts


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


def subscribeToThread(user, forum_thread):
  """Subscribes the given user to the given forum thread so that they recieve updates via email."""
  # if they're already a subscriber, nothing to do!
  keyname = "thread:%s:subscribers" % (forum_thread.key())
  model_key_name = "%s:%s" % (user.user_id(), forum_thread.key())
  subscribers = memcache.get(keyname)
  if not subscribers:
    thread_subscription = model.forum.ForumThreadSubscriber.get_by_key_name(model_key_name)
    if thread_subscription:
      return
  elif user.user_id() in subscribers:
    return

  thread_subscription = model.forum.ForumThreadSubscriber(key_name=model_key_name,
                                                          user=user,
                                                          forum_thread=forum_thread,
                                                          subscribed=datetime.datetime.now())
  thread_subscription.put()

  # we manually re-cache the subscription with the new one, because it can take a while for
  # datastore indexes to update, but we want it to be instant!
  subscribers = getThreadSubscriptions(forum_thread, False)
  if user.user_id() not in subscribers:
    subscribers[user.user_id()] = thread_subscription
  memcache.set(keyname, subscribers)


def unsubscribeFromThread(user, forum_thread):
  """Unsubscribes the given user from the given forum thread."""
  keyname = "thread:%s:subscribers" % (forum_thread.key())
  model_key_name = "%s:%s" % (user.user_id(), forum_thread.key())
  thread_subscription = model.forum.ForumThreadSubscriber.get_by_key_name(model_key_name)
  if not thread_subscription:
    return

  thread_subscription.delete()

  # we manually re-cache the subscription with this one removed, because it can take a while for
  # datastore indexes to update, but we want it to be instant!
  subscribers = getThreadSubscriptions(forum_thread, False)
  if user.user_id() in subscribers:
    del subscribers[user.user_id()]
  memcache.set(keyname, subscribers)


def getThreadSubscriptions(forum_thread, doset=True):
  """Gets the list of ForumThreadSubscribers who are subscribed to the given thread."""
  keyname = "thread:%s:subscribers" % (forum_thread.key())
  subscribers = memcache.get(keyname)
  if subscribers is None: # note: empty list is OK, None is not...
    subscribers = {}
    query = model.forum.ForumThreadSubscriber.all().filter("forum_thread", forum_thread)
    for subscriber in query:
      subscribers[subscriber.user.user_id()] = subscriber
    if doset:
      memcache.set(keyname, subscribers)
  return subscribers


def notifySubscribers(forum, forum_thread, forum_post, poster_user, poster_profile):
  """Sends an email notification to all subscribers of the given thread.

  Arguments:
    forum: The forum that was posted to.
    forum_thread: The forum thread we posted to.
    forum_post: The post that was just made
    poster_user: The user who posted (we don't send notifications to this user).
    poster_profile: The profile of the user who posted.
  """
  subscriptions = getThreadSubscriptions(forum_thread)
  tmpl = ctrl.tmpl.getTemplate("email/forum_notification.txt")

  user_ids = []
  for user_id, subscription in subscriptions.items():
    user_ids.append(user_id)
  profiles = ctrl.profile.getProfiles(user_ids)

  for user_id, subscription in subscriptions.items():
    if user_id == poster_user.user_id():
      continue
    body = ctrl.tmpl.render(tmpl, {"forum": forum, "forum_thread": forum_thread, "forum_post": forum_post,
                                   "poster_user": poster_user, "poster_profile": poster_profile,
                                   "profile": profiles[user_id]})
    sender = "forums@warworldssite.appspotmail.com"
    recipient = subscription.user.email()
    if recipient:
      logging.info("Sending email: {from:"+sender+", recipient:"+recipient+", subject:[war-worlds.com forums] "+
                   forum_thread.subject+", body:"+str(len(body))+" bytes")
      mail.send_mail(sender, recipient, "[war-worlds.com forums] "+forum_thread.subject, body)


def _indexForumThread(forum_thread, new_forum_post = None):
  """Does the actual work of indexing the given forum thread. We expect to be called in a deferred handler."""
  forum= forum_thread.forum
  fields = [search.TextField(name="subject", value=forum_thread.subject),
            search.DateField(name="posted", value=forum_thread.posted),
            search.DateField(name="last_post", value=forum_thread.last_post),
            search.AtomField(name="forum", value=forum.slug.replace(":", "_"))]
  if forum.alliance:
    fields.append(search.AtomField(name="alliance", value=forum.alliance.replace(":", "_")))
  else:
    fields.append(search.AtomField(name="alliance", value="NA"))

  content = ""
  for forum_post in model.forum.ForumPost.all().ancestor(forum_thread).order("posted"):
    if new_forum_post and str(forum_post.key()) == str(new_forum_post.key()):
      new_forum_post = None
    content += "\r\n<hr />\r\n" + forum_post.content
  if new_forum_post:
    content = new_forum_post.content + content
  fields.append(search.HtmlField(name="content", value=content))

  doc = search.Document(
    doc_id = str(forum_thread.key()),
    fields = fields)

  index = search.Index(name="forum")
  index.put(doc)


def indexForumThread(forum_thread, forum_post = None):
  """Queues the given forum thread to be indexed."""
  deferred.defer(_indexForumThread, forum_thread, forum_post, _queue="forumsync")

