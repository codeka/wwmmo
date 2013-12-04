
import os
from datetime import datetime
import re
import urllib
import webapp2 as webapp

from google.appengine.api import users
from google.appengine.api import memcache

import handlers
from ctrl import profile as profile_ctrl


class BasePage(handlers.BaseHandler):
  def render(self, tmplName, args):
    if not args:
      args = {}

    super(BasePage, self).render(tmplName, args)


class ProfilePage(BasePage):
  def get(self):
    if not self._isLoggedIn():
      return

    data = {}
    data['empires'] = profile_ctrl.getEmpiresForUser(self.user.email())

    self.render("profile/profile_edit.html", data)

  def post(self):
    if not self._isLoggedIn():
      return

    realm_name = None
    empire = None
    display_name = None
    linked_empire = self.request.POST.get("linked_empire")
    if linked_empire:
      empires = profile_ctrl.getEmpiresForUser(self.user.email())
      realm_name, empire_id = linked_empire.split(':')
      empire_id = int(empire_id)

      empire = None
      for empire_realm_name, this_empire in empires.items():
        if empire_realm_name == realm_name and int(this_empire["key"]) == empire_id:
          empire = this_empire
          display_name = empire["display_name"]
      if not empire:
        # actually an error
        realm_name = None
    else:
      display_name = self.request.POST.get("display_name")
    profile_ctrl.saveProfile(self.user.user_id(), realm_name, display_name, empire)

    self.redirect("/profile")


app = webapp.WSGIApplication([("/profile/?", ProfilePage)],
                             debug=os.environ["SERVER_SOFTWARE"].startswith("Development"))
