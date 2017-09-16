
import json
import logging
import os
import time
import urllib
import webapp2 as webapp

from google.appengine.api import memcache
from google.appengine.api import users

from ctrl import profile as profile_ctrl
import ctrl.tmpl

# This value gets incremented every time we deploy so that we can cache bust
# our static resources (css, js, etc)
RESOURCE_VERSION = 86


class BaseHandler(webapp.RequestHandler):
  def dispatch(self):
    """Dispatches the current request.

    Basically, we do some quick checks (e.g. to see whether the user is logged in, but hasn't yet set up
    a profile), then defer to the base class's method to do the actual dispatching."""
    self.user = users.get_current_user()
    self.profile = None
    if self.user:
      # they're logged in, so check to see whether they have a profile set up.
      self.profile = profile_ctrl.getProfile(self.user.user_id())
      if not self.profile and self.request.path != '/profile':
        self.redirect('/profile')
        return
      if self.profile and not self.profile.user:
        self.profile.user = self.user
        self.profile.put()
      if self.profile and self.profile.empire_id and not self.profile.realm_name:
        self.profile.realm_name = "Beta"
        self.profile.put()

    super(BaseHandler, self).dispatch()

  def render(self, tmplName, args):
    user = users.get_current_user()

    if not args:
      args = {}

    if user:
      args['is_logged_in'] = True
      args['logout_url'] = users.create_logout_url(self.request.uri)
      args['is_admin'] = (user.email() == 'dean@codeka.com.au')
      args['user'] = user
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


    if tmplName[-4:] == ".txt":
      self.response.content_type = "text/plain"
    elif tmplName[-4:] == ".rss":
      self.response.content_type = "application/rss+xml"
    else:
      self.response.content_type = "text/html"

    tmpl = ctrl.tmpl.getTemplate(tmplName)
    self.response.out.write(ctrl.tmpl.render(tmpl, args))

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


