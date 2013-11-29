
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
    empire_id = None
    display_name = None
    linked_empire = self.request.POST.get("linked_empire")
    if linked_empire:
      empires = profile_ctrl.getEmpiresForUser(self.user.email())
      realm_name, empire_id = linked_empire.split(':')
      empire_id = int(empire_id)

      found = False
      for empire_realm_name, empire in empires.items():
        if empire_realm_name == realm_name and int(empire["empires"][0]["key"]) == empire_id:
          found = True
          display_name = empire["empires"][0]["display_name"]
      if not found:
        # error
        realm_name = None
        empire_id = None
    else:
      display_name = self.request.POST.get("display_name")
    profile_ctrl.saveProfile(self.user.user_id(), realm_name, empire_id, display_name)

    self.redirect("/profile")


app = webapp.WSGIApplication([("/profile/?", ProfilePage)],
                             debug=os.environ["SERVER_SOFTWARE"].startswith("Development"))
