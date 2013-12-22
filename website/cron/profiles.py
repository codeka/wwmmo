
import os
from datetime import datetime
import json
import logging
import re
import urllib
import webapp2 as webapp

from google.appengine.ext import deferred
from google.appengine.api import memcache
from google.appengine.api import urlfetch
from google.appengine.api import users

import cron
import ctrl.profile
import model.profile


class BasePage(cron.BaseHandler):
  def render(self, tmplName, args):
    if not args:
      args = {}
    super(BasePage, self).render(tmplName, args)

def SyncEmpire(realm_name, empire):
  model.profile.Empire.Save(realm_name, empire)


def SyncBatch(realm_name, url, start_rank, num_empires):
  result = urlfetch.fetch(url+"minRank="+str(start_rank)+"&maxRank="+str(start_rank+num_empires),
                          headers = {'Accept': 'text/json'})
  if result.status_code == 200:
    empire_names = []
    for empire in json.loads(result.content)['empires']:
      SyncEmpire(realm_name, empire)
      empire_names.append(empire["display_name"])
    logging.info("Empires synced: "+realm_name+" ["+", ".join(empire_names)+"]")
    if len(empire_names) < num_empires:
      logging.info("Sync complete.")
    else:
      # there's more to go, schedule another sync
      deferred.defer(SyncBatch, realm_name, url, start_rank+num_empires, num_empires, _queue="profilesync")


def SyncEmpiresForRealm(realm_name, base_url):
  url = base_url + "empires/search?"
  deferred.defer(SyncBatch, realm_name, url, 1, 20, _queue="profilesync")


class SyncEmpiresPage(BasePage):
  def get(self):
    """This is a cron job that runs to sync empire details from the server(s) to the local store."""

    for realm_name, base_url in ctrl.profile.REALMS.items():
      SyncEmpiresForRealm(realm_name, base_url)


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
