
import os
from datetime import datetime
import json
import logging
import re
import urllib
import webapp2 as webapp

from google.appengine.api import users
from google.appengine.api import memcache
from google.appengine.api import urlfetch

import cron
import ctrl.profile
import model.profile


class BasePage(cron.BaseHandler):
  def render(self, tmplName, args):
    if not args:
      args = {}

    super(BasePage, self).render(tmplName, args)


class SyncEmpiresPage(BasePage):
  def get(self):
    """This is a cron job that runs to sync empire details from the server(s) to the local store."""

    for realm_name, base_url in ctrl.profile.REALMS.items():
      self.SyncEmpiresForRealm(realm_name, base_url)

  def SyncEmpiresForRealm(self, realm_name, base_url):
    url = base_url + "empires/search?"
    start_rank = 1
    total = 0
    while True:
      result = urlfetch.fetch(url+"minRank="+str(start_rank)+"&maxRank="+str(start_rank+20),
                              headers = {'Accept': 'text/json'})
      if result.status_code == 200:
        n = 0
        for empire in json.loads(result.content)['empires']:
          self.SyncEmpire(realm_name, empire)
          n += 1
        total += n
        if n < 20:
          logging.info("fetch complete, "+str(total)+" empires synced.")
          break
      else:
        logging.info("fetch returned HTTP "+str(result.status_code)+", aborting.")
        break
      start_rank += 20

  def SyncEmpire(self, realm_name, empire):
    logging.info("Syncing empire: "+realm_name+": "+empire["display_name"])
    model.profile.Empire.Save(realm_name, empire)


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


app = webapp.WSGIApplication([("/cron/profiles/sync-empires", SyncEmpiresPage),
                              ("/cron/profiles/sync-alliances", SyncAlliancesPage)],
                             debug=os.environ["SERVER_SOFTWARE"].startswith("Development"))
