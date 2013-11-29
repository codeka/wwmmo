
import jinja2
import json
import logging
import os
import time
import urllib
import webapp2 as webapp

from google.appengine.api import memcache
from google.appengine.api import users

from ctrl import profile as profile_ctrl

# This value gets incremented every time we deploy so that we can cache bust
# our static resources (css, js, etc)
RESOURCE_VERSION = 16


jinja = jinja2.Environment(loader=jinja2.FileSystemLoader(os.path.dirname(__file__)+'/../tmpl'))


def _filter_post_id(post):
  return post.key().id()
jinja.filters['post_id'] = _filter_post_id


def _filter_post_tags(post):
  return ', '.join(post.tags)
jinja.filters['post_tags'] = _filter_post_tags


def _filter_post_url(post):
  return '%04d/%02d/%s' % (post.posted.year, post.posted.month, post.slug)
jinja.filters['post_url'] = _filter_post_url


def _filter_post_full_url(post):
  return 'http://'+os.environ.get('HTTP_HOST')+'/blog/'+_filter_post_url(post)
jinja.filters['post_full_url'] = _filter_post_full_url


def _filter_post_date(post):
  return post.posted.strftime('%d %b %Y')
jinja.filters['post_date'] = _filter_post_date


def _filter_post_date_time(post):
  return post.posted.strftime('%d %b %Y %H:%M')
jinja.filters['post_date_time'] = _filter_post_date_time


def _filter_post_date_rss(post):
  return post.posted.strftime('%a, %d %b %Y %H:%M:%S GMT')
jinja.filters['post_date_rss'] = _filter_post_date_rss


def _filter_post_date_std(post):
  return post.posted.strftime('%Y-%m-%d %H:%M:%S')
jinja.filters['post_date_std'] = _filter_post_date_std


def _filter_post_extract(post):
  return post.html[0:500]+'...'
jinja.filters['post_extract'] = _filter_post_extract


def _filter_dump_json(obj):
  return json.dumps(obj)
jinja.filters['dump_json'] = _filter_dump_json


class BaseHandler(webapp.RequestHandler):
  def dispatch(self):
    """Dispatches the current request.

    Basically, we do some quick checks (e.g. to see whether the user is logged in, but hasn't yet set up
    a profile), then defer to the base class's method to do the actual dispatching."""
    self.user = users.get_current_user()
    if self.user:
      # they're logged in, so check to see whether they have a profile set up.
      self.profile = profile_ctrl.getProfile(self.user.user_id())
      if not self.profile and self.request.path != '/profile':
        self.redirect('/profile')
        return

    super(BaseHandler, self).dispatch()

  def render(self, tmplName, args):
    user = users.get_current_user()

    if not args:
      args = {}

    if user:
      args['is_logged_in'] = True
      args['logout_url'] = users.create_logout_url(self.request.uri)
      args['is_writer'] = (user.email() == 'dean@codeka.com.au' or user.email() == 'lam.h.emily@gmail.com')
      args['user_email'] = user.email()
    else:
      args['is_logged_in'] = False
      args['login_url'] = users.create_login_url(self.request.uri)

    if os.environ['SERVER_SOFTWARE'].startswith('Development'):
      args['is_development_server'] = True
      args['resource_version'] = int(time.time())
    else:
      args['is_development_server'] = False
      args['resource_version'] = RESOURCE_VERSION

    if self.user and self.profile:
      args['user_profile'] = self.profile
    else:
      args['user_profile'] = None


    tmpl = jinja.get_template(tmplName)
    self.response.out.write(tmpl.render(args))

  def error(self, code):
    super(BaseHandler, self).error(code)
    if code == 404:
      self.render("404.html", {})

  def _isLoggedIn(self):
    """For pages that require a logged-in user, this can be called to ensure you're logged in."""
    self.user = users.get_current_user()
    if not self.user:
      # not logged in, so redirect to the login page
      self.redirect(users.create_login_url(self.request.path_qs))
      return False

    return True


