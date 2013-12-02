
import os
from datetime import datetime
import re
import urllib
import webapp2 as webapp

from google.appengine.api import users
from google.appengine.api import memcache

import ctrl.forum
import ctrl.profile
import model.forum
import handlers


def _filter_forum_post_author(post):
  return post.user.email()
handlers.jinja.filters['forum_post_author'] = _filter_forum_post_author


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

    page_no = 0
    if self.request.get('page'):
      page_no = int(self.request.get('page'))
    if page_no < 0:
      page_no = 0

    threads = ctrl.forum.getThreads(forum, page_no, 50)
    if not threads and page_no > 0:
      self.redirect('/forum/%s?page=%d' % (forum.slug, page_no-1))

    post_counts = ctrl.forum.getThreadPostCounts(threads)
    first_posts = ctrl.forum.getFirstPostsByForumThread(threads)
    last_posts = ctrl.forum.getLastPostsByForumThread(threads)

    user_ids = []
    for thread in threads:
      if thread.user.user_id() not in user_ids:
        user_ids.append(thread.user.user_id())
      if last_posts[thread.key()].user.user_id() not in user_ids:
        user_ids.append(last_posts[thread.key()].user.user_id())
    profiles = ctrl.profile.getProfiles(user_ids)

    self.render("forum/thread_list.html", {"forum": forum,
                                           "threads": threads,
                                           "post_counts": post_counts,
                                           "profiles": profiles,
                                           "last_posts": last_posts,
                                           "first_posts": first_posts,
                                           "page_no": page_no})


class PostListPage(ForumPage):
  def get(self, forum_slug, forum_thread_slug):
    forum = ctrl.forum.getForumBySlug(forum_slug)
    if not forum:
      self.error(404)
      return

    forum_thread = ctrl.forum.getThreadBySlug(forum, forum_thread_slug)
    if not forum_thread:
      self.error(404)
      return

    page_no = 0
    if self.request.get('page'):
      page_no = int(self.request.get('page'))
    if page_no < 0:
      page_no = 0

    posts = ctrl.forum.getPosts(forum, forum_thread, page_no, 25)

    user_ids = []
    for post in posts:
      if post.user.user_id() not in user_ids:
        user_ids.append(post.user.user_id())
    profiles = ctrl.profile.getProfiles(user_ids)

    self.render("forum/post_list.html", {"forum": forum,
                                         "forum_thread": forum_thread,
                                         "posts": posts,
                                         "profiles": profiles,
                                         "page_no": page_no})


class EditPostPage(ForumPage):
  def get(self, forum_slug, forum_thread_slug=None):
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

    self.render("forum/post_edit.html", {"forum": forum,
                                         "forum_thread": forum_thread})

  def post(self, forum_slug, forum_thread_slug=None):
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
                                             posted = now,
                                             user = self.user)
      forum_thread.put()
      ctrl.forum.incrCount("forum:%s:threads" % (forum.slug))

    content = self.request.POST.get("post-content")
    forum_post = model.forum.ForumPost(parent = forum_thread,
                                       forum = forum,
                                       posted = now,
                                       user = self.user,
                                       content = content)
    forum_post.put()
    ctrl.forum.incrCount("forum:%s:posts" % (forum.slug))
    ctrl.forum.incrCount("thread:%s:%s:posts" % (forum.slug, forum_thread.slug), 1)

    memcache.flush_all()
    self.redirect("/forum/%s/%s" % (forum.slug, forum_thread.slug))


app = webapp.WSGIApplication([("/forum/?", ForumListPage),
                              ("/forum/([^/]+)/?", ThreadListPage),
                              ("/forum/([^/]+)/posts", EditPostPage),
                              ("/forum/([^/]+)/([^/]+)", PostListPage),
                              ("/forum/([^/]+)/([^/]+)/posts", EditPostPage)],
                             debug=os.environ["SERVER_SOFTWARE"].startswith("Development"))
