
import os
from datetime import datetime
import logging
import math
import re
import urllib
import webapp2 as webapp

from google.appengine.api import users
from google.appengine.api import memcache

import ctrl.forum
import ctrl.profile
import model.forum
import handlers

class ForumPage(handlers.BaseHandler):
  pass


class ForumListPage(ForumPage):
  def get(self):
    forums = ctrl.forum.getForums()

    if self.profile and self.profile.alliance_id:
      alliance = model.profile.Alliance.Fetch(self.profile.realm_name, self.profile.alliance_id)
      if alliance:
        alliance_forum = ctrl.forum.getAllianceForum(self.profile.realm_name, alliance)
        forums.insert(0, alliance_forum)

    post_counts = ctrl.forum.getForumThreadPostCounts()
    top_threads = ctrl.forum.getTopThreadsPerForum(forums)

    top_thread_user_ids = []
    for forum in forums:
      if forum.slug in top_threads:
        top_thread = top_threads[forum.slug]
        if top_thread.user.user_id() not in top_thread_user_ids:
          top_thread_user_ids.append(top_thread.user.user_id())
    top_thread_user_profiles = ctrl.profile.getProfiles(top_thread_user_ids)

    self.render("forum/forum_list.html", {"forums": forums,
                                          "post_counts": post_counts,
                                          "top_threads": top_threads,
                                          "top_thread_user_profiles": top_thread_user_profiles})


class ThreadListPage(ForumPage):
  def get(self, forum_slug):
    forum = ctrl.forum.getForumBySlug(forum_slug)
    if not forum:
      self.error(404)
      return

    data = {}
    data["forum"] = forum
    data["is_moderator"] = False
    if self.user:
      data["is_moderator"] = ctrl.forum.isModerator(forum, self.user)

    page_no = 1
    if self.request.get('page'):
      page_no = int(self.request.get('page'))
    if page_no <= 0:
      page_no = 1
    data["page_no"] = page_no

    threads = ctrl.forum.getThreads(forum, page_no - 1, 25)
    if not threads and page_no > 1:
      self.redirect('/forum/%s?page=%d' % (forum.slug, page_no-1))
    data["threads"] = threads

    thread_post_counts = ctrl.forum.getForumThreadPostCounts()
    if forum.slug in thread_post_counts:
      data["total_threads"] = thread_post_counts[forum.slug]['threads']
    else:
      data["total_threads"] = 0
    data["total_pages"] = int(math.ceil(data["total_threads"] / 25.0))
    data["post_counts"] = ctrl.forum.getThreadPostCounts(threads)
    data["first_posts"] = ctrl.forum.getFirstPostsByForumThread(threads)
    data["last_posts"] = ctrl.forum.getLastPostsByForumThread(threads)

    user_ids = []
    for thread in threads:
      if thread.user.user_id() not in user_ids:
        user_ids.append(thread.user.user_id())
      if thread.key() in data["last_posts"] and data["last_posts"][thread.key()].user.user_id() not in user_ids:
        user_ids.append(data["last_posts"][thread.key()].user.user_id())
    data["profiles"] = ctrl.profile.getProfiles(user_ids)

    self.render("forum/thread_list.html", data)


class PostListPage(ForumPage):
  def get(self, forum_slug, forum_thread_slug):
    forum = ctrl.forum.getForumBySlug(forum_slug)
    if not forum:
      self.error(404)
      return

    data = {}
    data["forum"] = forum

    is_moderator = False
    if self.user:
      is_moderator = ctrl.forum.isModerator(forum, self.user)
    data["is_moderator"] = is_moderator

    forum_thread = ctrl.forum.getThreadBySlug(forum, forum_thread_slug)
    if not forum_thread:
      self.error(404)
      return
    data["forum_thread"] = forum_thread

    page_no = 1
    if self.request.get('page'):
      if self.request.get('page') == 'last':
        page_no = 1
      else:
        page_no = int(self.request.get('page'))
    if page_no <= 0:
      page_no = 1
    data["page_no"] = page_no

    posts = ctrl.forum.getPosts(forum, forum_thread, page_no - 1, 10)
    data["posts"] = posts

    data["total_posts"] = ctrl.forum.getThreadPostCounts([forum_thread])["%s:%s" % (forum.slug, forum_thread.slug)]
    data["total_pages"] = int(math.ceil(data["total_posts"] / 10.0))

    # if they were actually asking for the "last" page, we can redirect there now
    if self.request.get('page') and self.request.get('page') == 'last':
      self.redirect(str("/forum/%s/%s?page=%d" % (forum.slug, forum_thread.slug, data["total_pages"])))
      return

    user_ids = []
    for post in posts:
      if post.user.user_id() not in user_ids:
        user_ids.append(post.user.user_id())
    profiles = ctrl.profile.getProfiles(user_ids)
    data["profiles"] = profiles

    subscriptions = ctrl.forum.getThreadSubscriptions(forum_thread)
    data["subscriptions"] = subscriptions
    data["is_subscribed"] = bool(self.user and self.user.user_id() in subscriptions)

    self.render("forum/post_list.html", data)


class EditPostPage(ForumPage):
  def get(self, forum_slug, forum_thread_slug=None, post_id=None):
    if not self._isLoggedIn():
      return

    forum = ctrl.forum.getForumBySlug(forum_slug)
    if not forum:
      self.error(404)
      return

    forum_thread = None
    if forum_thread_slug:
      forum_thread = ctrl.forum.getThreadBySlug(forum, forum_thread_slug)
      if not forum_thread:
        self.error(404)
        return

    post = None
    if post_id:
      post = model.forum.ForumPost.get(post_id)
      # if you're after a specific post, you either need to be a moderator or
      # you need to be the person who posted this post.
      if not ctrl.forum.isModerator(forum, self.user) and post.user.user_id() != self.user.user_id():
        self.error(404)
        return


    self.render("forum/post_edit.html", {"forum": forum,
                                         "forum_thread": forum_thread,
                                         "post": post})

  def post(self, forum_slug, forum_thread_slug=None, post_id=None):
    if not self._isLoggedIn():
      return

    forum = ctrl.forum.getForumBySlug(forum_slug)
    if not forum:
      self.error(404)
      return

    now = datetime.now()

    if forum_thread_slug:
      forum_thread = ctrl.forum.getThreadBySlug(forum, forum_thread_slug)
      if not forum_thread:
        self.error(404)
        return
    else:
      subject = self.request.POST.get("post-subject").strip()
      slug = subject.encode("ascii", "ignore").lower()
      slug = re.sub(r"[^a-z0-9]+", "-", slug).strip("-")

      num_existing_slugs = 0
      for existing_thread in (model.forum.ForumThread.all()
                                   .filter("slug >=", slug)
                                   .filter("slug <", slug+"\ufffd")):
        if re.match(slug+"(-[0-9]+)?", existing_thread.slug):
          num_existing_slugs += 1
      if num_existing_slugs > 0:
        slug += "-"+str(num_existing_slugs)

      forum_thread = model.forum.ForumThread(forum = forum,
                                             subject = subject,
                                             slug = slug,
                                             posted = now, last_post = now,
                                             user = self.user)
      forum_thread.put()
      ctrl.forum.incrCount("forum:%s:threads" % (forum.slug))

    content = ctrl.sanitizeHtml(self.request.POST.get("post-content"))
    if post_id:
      forum_post = model.forum.ForumPost.get(post_id)
      forum_post.content = content
      forum_post.updated = datetime.now()
      if forum_post.edit_notes:
        forum_post.edit_notes += "<br/>"
      else:
        forum_post.edit_notes = ""

      forum_post.edit_notes += ("<em>Edited by "+self.profile.display_name+', <time datetime="'+
          forum_post.updated.strftime('%Y-%m-%d %H:%M:%S')+'">'+forum_post.updated.strftime('%d %b %Y %H:%M')+"</time></em>")
    else:
      forum_post = model.forum.ForumPost(parent = forum_thread,
                                         forum = forum,
                                         posted = now,
                                         user = self.user,
                                         content = content)

    forum_thread.last_post = now
    forum_thread.put()

    forum_post.put()
    ctrl.forum.incrCount("forum:%s:posts" % (forum.slug))
    ctrl.forum.incrCount("thread:%s:%s:posts" % (forum.slug, forum_thread.slug), 1)

    memcache.flush_all()

    # if you checked the 'subscribe' checkbox, also subscribe you...
    if self.request.POST.get("post-subscribe"):
      ctrl.forum.subscribeToThread(self.user, forum_thread)

    # any users listed in the forum's auto-subscribe list get auto-subscribed...
    if forum.auto_subscribers:
      for subscriber in forum.auto_subscribers:
        ctrl.forum.subscribeToThread(subscriber, forum_thread)

    # go through all the subscribers and send them a notification about this post
    ctrl.forum.notifySubscribers(forum, forum_thread, forum_post, self.user, self.profile)

    total_posts = ctrl.forum.getThreadPostCounts([forum_thread])["%s:%s" % (forum.slug, forum_thread.slug)]
    total_pages = int(math.ceil(total_posts / 10.0))

    if total_pages <= 1:
      self.redirect("/forum/%s/%s" % (forum.slug, forum_thread.slug))
    else:
      self.redirect("/forum/%s/%s?page=%d" % (forum.slug, forum_thread.slug, total_pages))


class DeletePostPage(ForumPage):
  def _getDetails(self, forum_slug, forum_thread_slug, post_id):
    forum = ctrl.forum.getForumBySlug(forum_slug)
    if not forum:
      return

    forum_thread = ctrl.forum.getThreadBySlug(forum, forum_thread_slug)
    if not forum_thread:
      return

    post = model.forum.ForumPost.get(post_id)
    # you either need to be a moderator or you need to be the person who posted this post.
    if not ctrl.forum.isModerator(forum, self.user) and post.user.user_id() != self.user.user_id():
      return

    return (forum, forum_thread, post)
    
  def get(self, forum_slug, forum_thread_slug, post_id):
    if not self._isLoggedIn():
      return

    forum, forum_thread, post = self._getDetails(forum_slug, forum_thread_slug, post_id)
    if not forum or not forum_thread or not post:
      self.error(404)
      return

    self.render("forum/post_delete.html", {"forum": forum,
                                           "forum_thread": forum_thread,
                                           "post": post})

  def post(self, forum_slug, forum_thread_slug, post_id):
    if not self._isLoggedIn():
      return

    forum, forum_thread, post = self._getDetails(forum_slug, forum_thread_slug, post_id)
    if not forum or not forum_thread or not post:
      self.error(404)
      return

    post.delete()

    # if it's the last post in this thread, delete the thread as well
    num_posts_left = 0
    for p in model.forum.ForumPost.all().ancestor(forum_thread):
      if p.key == post.key:
        continue
      num_posts_left += 1
    if num_posts_left == 0:
      forum_thread.delete()

    ctrl.forum.incrCount("forum:%s:posts" % (forum.slug), amount=-1)
    ctrl.forum.incrCount("thread:%s:%s:posts" % (forum.slug, forum_thread.slug), 1, amount=-1)

    memcache.flush_all()
    if num_posts_left == 0:
      self.redirect("/forum/%s" % (forum.slug))
    else:
      self.redirect("/forum/%s/%s" % (forum.slug, forum_thread.slug))


class SubscriptionPage(ForumPage):
  def post(self, forum_slug, forum_thread_slug):
    if not self._isLoggedIn():
      return

    forum = ctrl.forum.getForumBySlug(forum_slug)
    if not forum:
      self.error(404)
      return

    forum_thread = ctrl.forum.getThreadBySlug(forum, forum_thread_slug)
    if not forum_thread:
      self.error(404)
      return

    if self.request.POST.get("action") == "subscribe":
      ctrl.forum.subscribeToThread(self.user, forum_thread)
    elif self.request.POST.get("action") == "unsubscribe":
      ctrl.forum.unsubscribeFromThread(self.user, forum_thread)
    self.redirect("/forum/%s/%s#post-reply" % (forum.slug, forum_thread.slug))


app = webapp.WSGIApplication([("/forum/?", ForumListPage),
                              ("/forum/([^/]+)/?", ThreadListPage),
                              ("/forum/([^/]+)/posts", EditPostPage),
                              ("/forum/([^/]+)/([^/]+)", PostListPage),
                              ("/forum/([^/]+)/([^/]+)/posts", EditPostPage),
                              ("/forum/([^/]+)/([^/]+)/subscription", SubscriptionPage),
                              ("/forum/([^/]+)/([^/]+)/posts/([^/]+)", EditPostPage),
                              ("/forum/([^/]+)/([^/]+)/posts/([^/]+)/delete", DeletePostPage)],
                             debug=os.environ["SERVER_SOFTWARE"].startswith("Development"))
