
import json
import logging

from google.appengine.api import memcache
from google.appengine.api import urlfetch

import ctrl
from model import profile


def getEmpiresForUser(user_email):
  keyname = 'profile:empires-for-user:'+user_email
  empires = memcache.get(keyname)
  if not empires:
    # we fire off an HTTP request to each of the realms to get empire details about this email address
    urls = {'Beta': 'https://game.war-worlds.com/realms/beta/empires/search?email=' + user_email,
            'Blitz': 'https://game.war-worlds.com/realms/blitz/empires/search?email=' + user_email}
    if ctrl.isDevelopmentServer():
      urls['Debug'] = 'http://localhost:8080/realms/beta/empires/search?email=' + user_email

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
          empires[realm_name] = empire

    memcache.set(keyname, empires, time=3600)

  return empires


def saveProfile(user_id, realm_name, empire_id, display_name):
  # TODO: cache
  profile.Profile.SaveProfile(user_id, realm_name, empire_id, display_name)


def getProfile(user_id):
  # TODO: cache
  return profile.Profile.GetProfile(user_id)
