
import json
import logging

from google.appengine.api import memcache
from google.appengine.api import urlfetch

import ctrl
import model.profile


REALMS = {'Beta': 'https://game.war-worlds.com/realms/beta/',
          'Blitz': 'https://game.war-worlds.com/realms/blitz/'}
if ctrl.isDevelopmentServer():
  REALMS['Debug'] = 'http://localhost:8080/realms/beta/'


def getEmpiresForUser(user_email):
  """Fetches empires for the given user.

  Even though the empires should be in the data store already, we force fetch them from the server. This is
  because it could be a new user and it hasn't synced yet, but also this provides a way for the user to force
  their empire to update after changing names or shield (otherwise, they'd have to wait for ~3 hours when the
  cron job runs)."""
  keyname = 'profile:empires-for-user:'+user_email
  empires = memcache.get(keyname)
  if not empires:
    # we fire off an HTTP request to each of the realms to get empire details about this email address
    urls = {}
    for realm_name,base_url in REALMS.items():
      urls[realm_name] = base_url+'empires/search?email=' + user_email

    # make simultaneous calls to all the URLs
    rpcs = {}
    for realm_name,url in urls.items():
      rpc = urlfetch.create_rpc()
      urlfetch.make_fetch_call(rpc, url, headers = {'Accept': 'text/json'})
      rpcs[realm_name] = rpc

    empires = {}
    for realm_name, rpc in rpcs.items():
      result = rpc.get_result()
      if result.status_code == 200:
        empire = json.loads(result.content)
        if empire:
          empire = empire["empires"][0]
          empires[realm_name] = empire
          # while we're here, save it to the data store
          model.profile.Empire.Save(realm_name, empire)

    memcache.set(keyname, empires, time=3600)

  return empires


def saveProfile(user_id, realm_name, display_name, empire):
  profile = model.profile.Profile.SaveProfile(user_id, realm_name, display_name, empire)
  keyname = "profile:%s" % (user_id)
  memcache.set(keyname, profile)


def getProfile(user_id):
  keyname = "profile:%s" % (user_id)
  profile = memcache.get(keyname)
  if not profile:
    profile = model.profile.Profile.GetProfile(user_id)
    if profile:
      memcache.set(keyname, profile)
  return profile


def getProfiles(user_ids):
  """Fetches profiles of multiple users at once.

  Args:
    user_ids: A list of user_id objects we want to fetch profiles for.
  Returns
    A mapping of user_id > profile for each matched user.
  """
  keynames = []
  for user_id in user_ids:
    keynames.append("profile:%s" % (user_id))
  cache_mapping = memcache.get_multi(keynames)

  for user_id in user_ids:
    keyname = "profile:%s" % (user_id)
    if keyname not in cache_mapping:
      profile = model.profile.Profile.GetProfile(user_id)
      cache_mapping[keyname] = profile

      if profile:
        memcache.set(keyname, profile)

  memcache.set_multi(cache_mapping)

  profiles = {}
  for user_id in user_ids:
    profiles[user_id] = cache_mapping["profile:%s" % (user_id)]
  return profiles
