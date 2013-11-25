
import os
from datetime import datetime
import re
import urllib
import webapp2 as webapp

from google.appengine.api import users
from google.appengine.api import memcache

import ctrl.forum
import model.forum
import handlers


def _filter_forum_post_author(post):
  return post.user.email()
handlers.jinja.filters['forum_post_author'] = _filter_forum_post_author


class ForumPage(handlers.BaseHandler):
  def _isLoggedIn(self):
    """For pages that require a logged-in user, this can be called to ensure you're logged in."""
    self.user = users.get_current_user()
    if not self.user:
      # not logged in, so redirect to the login page
      self.redirect("/forum/login?"+urllib.urlencode({"continue": self.request.path_qs}))
      return False

    return True

  def render(self, tmplName, args):
    if not args:
      args = {}

    if self.user and self.profile:
      args['user_profile'] = self.profile
    else:
      args['user_profile'] = None

    super(ForumPage, self).render(tmplName, args)

  def dispatch(self):
    """If the user is logged in, make sure they have a profile."""
    self.user = users.get_current_user()
    if self.user:
      # logged in, so try to fetch their profile
      self.profile = ctrl.forum.getUserProfile(self.user)
      if not self.profile:
        # if we're ON the profile page, don't redirect!
        if self.request.path_qs[0:14] != "/forum/profile":
          self.redirect("/forum/profile?"+urllib.urlencode({"continue": self.request.path_qs}))
          return

    super(ForumPage, self).dispatch()


class LoginPage(ForumPage):
  def get(self):
    self.render("forum/login.html", {"login_url": users.create_login_url(self.request.get("continue"))})


class ProfilePage(ForumPage):
  def get(self):
    self.render("forum/profile_edit.html", {"profile": self.profile})

  def post(self):

    display_name = self.request.POST.get("profile-displayname")

    if not self.profile:
      self.profile = model.forum.ForumUserProfile(display_name=display_name,
                                                  user = self.user)

    self.profile.display_name = display_name
    self.profile.put()
    memcache.set("forum:user:%s" % (self.user.user_id()), self.profile)

    if self.request.get("continue") != "":
      self.redirect(self.request.get("continue"))
    else:
      self.redirect(self.request.path_qs)

class ForumListPage(ForumPage):
  def get(self):
    forums = ctrl.forum.getForums()
    post_counts = ctrl.forum.getForumThreadPostCounts()
    top_threads = ctrl.forum.getTopThreadsPerForum(forums)

    top_thread_users = []
    for forum in forums:
      top_thread = top_threads[forum.slug]
      if top_thread.user not in top_thread_users:
        top_thread_users.append(top_thread.user)
    top_thread_user_profiles = ctrl.forum.getUserProfiles(top_thread_users)

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
    last_posts = ctrl.forum.getLastPostsByForumThread(threads)

    users = []
    for thread in threads:
      if thread.user not in users:
        users.append(thread.user)
      if last_posts[thread.key()].user not in users:
        users.append(last_posts[thread.key()].user)
    profiles = ctrl.forum.getUserProfiles(users)

    self.render("forum/thread_list.html", {"forum": forum,
                                           "threads": threads,
                                           "post_counts": post_counts,
                                           "profiles": profiles,
                                           "last_posts": last_posts,
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

    self.render("forum/post_list.html", {"forum": forum,
                                         "forum_thread": forum_thread,
                                         "posts": posts,
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
                              ("/forum/login", LoginPage),
                              ("/forum/profile", ProfilePage),
                              ("/forum/([^/]+)/?", ThreadListPage),
                              ("/forum/([^/]+)/posts", EditPostPage),
                              ("/forum/([^/]+)/([^/]+)", PostListPage),
                              ("/forum/([^/]+)/([^/]+)/posts", EditPostPage)],
                             debug=os.environ["SERVER_SOFTWARE"].startswith("Development"))
