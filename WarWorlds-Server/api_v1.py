"""api_v1.py: The handlers for the actual API that the client calls."""

from datetime import datetime, timedelta
import os
import logging

from google.appengine.ext import db
from google.appengine.api import users

import webapp2 as webapp

import import_fixer
import_fixer.FixImports("google", "protobuf")

import model
from model import gcm as gcm_mdl
from ctrl import sector
from ctrl import empire
from ctrl import simulation
from ctrl import chat
import ctrl

from protobufs import protobuf_json, messages_pb2 as pb
from google.protobuf import message



class ApiPage(webapp.RequestHandler):
  """This is the base class for pages in the API section."""

  def dispatch(self):
    """Checks that a user is logged in and such before we process the request."""

    self.real_user = users.get_current_user()
    if not self.real_user:
      # not logged in, return 403 Forbidden
      self.response.set_status(403)
      return

    # the admin user can beform requests on behalf of anyone
    self.user = self.real_user
    if self._isAdmin() and self.request.get("on_behalf_of") != "":
      self.user = users.User(self.request.get("on_behalf_of"))

    return super(ApiPage, self).dispatch()

  def _isAdmin(self):
    # TODO: better version of "isAdmin!"
    return self.real_user.email() == "dean@codeka.com.au"

  def _getRequestBody(self, ProtoBuffClass):
    """Gets the body of the request as a protocol buffer.

    We check whether the body is application/json or application/x-protobuf before
    decoding the message. They're the two main kinds of requests we accept.
    """

    pb = ProtoBuffClass()

    contentType = self.request.headers["Content-Type"]
    contentType = contentType.split(';')[0].strip()
    if contentType == "application/json":
      protobuf_json.json2pb(pb, self.request.body)
    elif contentType == "application/x-protobuf":
      pb.ParseFromString(self.request.body)
    return pb

  def raiseError(self, error_code, error_message):
    err = pb.GenericError()
    err.error_code = error_code
    err.error_message = error_message
    self.response.set_status(400)
    return err


class HelloPage(ApiPage):
  """The 'hello' page is what you request when you first connect."""

  def get(self):
    """This can be used by the administrator for testing only."""

    if not self._isAdmin():
      self.response.set_status(400)
      return
    return self.put("")

  def put(self, deviceRegistrationKey):
    user = self.user
    if self._isAdmin() and self.request.get("email", "") != "":
      # useful for testing as an administrator what this would return for another user...
      user = users.User(self.request.get("email"))
      device_mdl = None
    else:
      device_mdl = model.DeviceRegistration.get(deviceRegistrationKey)
      if device_mdl is None:
        # ERROR
        logging.error("No device with registration key [%s] found." % (deviceRegistrationKey))
        self.response.set_status(400)
        return

    motd_model = model.MessageOfTheDay.get()
    empire_pb = empire.getEmpireForUser(user)
    if empire_pb:
      colonies_pb = empire.getColoniesForEmpire(empire_pb)
    else:
      colonies_pb = None

    hello_pb = pb.Hello()
    if motd_model is not None:
      hello_pb.motd.message = motd_model.message
      hello_pb.motd.last_update = motd_model.date.isoformat()
    else:
      hello_pb.motd.message = ""
      hello_pb.motd.last_update = ""

    if device_mdl:
      hello_pb.require_gcm_register = (device_mdl.gcmRegistrationID == "")

    if empire_pb is not None:
      hello_pb.empire.MergeFrom(empire_pb)

    if colonies_pb is not None:
      hello_pb.colonies.MergeFrom(colonies_pb.colonies)

    return hello_pb


class ChatPage(ApiPage):
  def get(self):
    """Gets the latest chat messages based on various criteria."""
    since = self.request.get("since")
    if since:
      since = ctrl.epochToDateTime(int(since))
      seven_days_ago = datetime.now() - timedelta(days=7)
      if since < seven_days_ago:
        since = seven_days_ago
    else:
      since = datetime.now() - timedelta(hours=24)
    max_chats = self.request.get("max")
    if max_chats:
      max_chats = int(max_chats)
      if max_chats > 1000:
        max_chats = 1000
    else:
      max_chats = 100

    return chat.getLatestChats(since=since, max_chats=max_chats)

  def post(self):
    msg_pb = self._getRequestBody(pb.ChatMessage)
    empire_pb = empire.getEmpireForUser(self.user)
    if empire_pb and not self._isAdmin():
      msg_pb.empire_key = empire_pb.key
    else:
      msg_pb.empire_key = ""
    msg_pb.date_posted = ctrl.dateTimeToEpoch(datetime.now())

    chat.postMessage(self.user, msg_pb)


class EmpiresPage(ApiPage):
  def get(self, empireKey=None):
    empire_pb = None

    if empireKey:
      empire_pb = empire.getEmpire(empireKey)
    elif self.request.get("search", "") != "":
      search = self.request.get("search")
      if "@" in search:
        user = users.User(search)
        if user:
          empire_pb = empire.getEmpireForUser(user)
      if not empire_pb:
        empire_pb = empire.getEmpireByName(search)
      if not empire_pb:
        try:
          key = db.Key(search)
          empire_pb = empire.getEmpire(str(key))
        except:
          empire_pb = None

    if empire_pb is None:
      self.response.set_status(404)
      return

    if empire_pb.email != self.user.email():
      # it's not OUR empire, so block out a few details...
      empire_pb.cash = 0;

    return empire_pb

  def put(self):
    empire_pb = self._getRequestBody(pb.Empire)
    if empire_pb is None:
      self.response.set_status(400)
      return

    sim = simulation.Simulation()
    empire_pb.email = self.user.email()
    empire.createEmpire(empire_pb, sim)
    sim.update()


class EmpireDetailsPage(ApiPage):
  """The EmpireDetailsPage returns detailed information about the empire including all
  of the empire's fleets, colonies and so on. It's only available to the owner of the
  empire, and the administrators."""

  def get(self, empire_key):
    empire_pb = empire.getEmpire(empire_key)

    if self._isAdmin() or empire_pb.email == self.user.email():
      colonies_pb = empire.getColoniesForEmpire(empire_pb)

      fleets_pb = empire.getFleetsForEmpire(empire_pb)
      empire_pb.fleets.MergeFrom(fleets_pb.fleets)

      star_keys = []
      for colony_pb in colonies_pb.colonies:
        if colony_pb.star_key not in star_keys:
          star_keys.append(colony_pb.star_key)
      for fleet_pb in fleets_pb.fleets:
        if fleet_pb.star_key not in star_keys:
          star_keys.append(fleet_pb.star_key)

      sim = simulation.Simulation()
      colony_pbs = []
      star_pbs = []
      for star_key in star_keys:
        sim.simulate(star_key)
        star_pb = sim.getStar(star_key)

        for star_colony_pb in star_pb.colonies:
          for colony_pb in colonies_pb.colonies:
            if colony_pb.key == star_colony_pb.key:
              colony_pbs.append(star_colony_pb)

        star_pbs.append(sector.sumarize(star_pb))

      empire_pb.colonies.extend(colony_pbs)
      empire_pb.stars.extend(star_pbs)

    return empire_pb


class DevicesPage(ApiPage):
  def post(self):
    registration_pb = self._getRequestBody(pb.DeviceRegistration)
    if registration_pb == None:
      # handle errors...
      self.response.set_status(400)
      return

    return ctrl.updateDeviceRegistration(registration_pb, self.user)

  def get(self):
    if self.request.get("email") != '':
      # TODO: check that you're an administrator...
      user = users.User(self.request.get("email"))
      if user is None:
        self.response.set_status(404)
        return
      return ctrl.getDevicesForUser(user.email())
    else:
      # if you don't pass any parameters, it's like searching for your
      # own device registrations.
      return ctrl.getDevicesForUser(self.user.email())

  def delete(self, key):
    device = model.DeviceRegistration.get(key)
    if device != None:
      # make sure you only ever delete devices you own
      if device.user != self.user:
        self.response.set_status(403)
        return
      device.delete()

      # TODO: delete should be in the controller itself
      ctrl.clearCached(["devices:for-user:%s" % (self.user.email())])
    else:
      logging.warn("No device with key [%s] to delete." % (key))

  def put(self, key):
    if self.request.get("online_status") == "1":
      device_online_status_pb = self._getRequestBody(pb.DeviceOnlineStatus)

      query = model.OnlineDevice.all().filter("device", db.Key(key))
      for device in query:
        device.delete()

      if device_online_status_pb.is_online:
        online_device_mdl = model.OnlineDevice()
        online_device_mdl.device = db.Key(key)
        online_device_mdl.user = self.user
        online_device_mdl.onlineSince = datetime.now()
        online_device_mdl.put()
    else:
      device_registration_pb = self._getRequestBody(pb.DeviceRegistration)

      device_registration_mdl = model.DeviceRegistration.get(key)
      device_registration_mdl.gcmRegistrationID = device_registration_pb.gcm_registration_id
      device_registration_mdl.put()


class DeviceMessagesPage(ApiPage):
  def put(self, email):
    msg = self._getRequestBody(pb.DeviceMessage)
    logging.info("Putting message to user with email: %s" % (email))
    logging.info("Message: %s" % (msg))

    user = users.User(email)
    if user is None:
      self.response.set_status(404)
      return

    devices = ctrl.getDevicesForUser(email)
    registration_ids = []
    for device in devices.registrations:
      registration_ids.append(device.gcm_registration_id)
    gcm = gcm_mdl.GCM('AIzaSyADWOC-tWUbzj-SVW13Sz5UuUiGfcmHHDA')
    gcm.json_request(registration_ids=registration_ids,
                     data={"msg": "Hello World"})
    return None


class StarfieldPage(ApiPage):
  pass


class SectorsPage(StarfieldPage):
  def get(self):
    if self.request.get("coords") != "":
      coords = []
      for coord in self.request.get("coords").split('|'):
        (x, y) = coord.split(',', 2)
        x = int(x)
        y = int(y)
        coords.append(sector.SectorCoord(x, y))

      gen = True
      if self.request.get("gen") == "0":
        gen = False

      empire_pb = empire.getEmpireForUser(self.user)
      sectors_pb = sector.getSectors(coords, gen)
      # we only return fleets for the current empire -- we don't let you see
      # other empire's fleets. The exception is moving/attacking fleets, you
      # can see those
      for sector_pb in sectors_pb.sectors:
        visible_fleets = []
        for fleet_pb in sector_pb.fleets:
          if (self._isAdmin() or fleet_pb.empire_key == empire_pb.key or
              fleet_pb.state != pb.Fleet.IDLE):
            visible_fleets.append(fleet_pb)

        del sector_pb.fleets[:]
        sector_pb.fleets.extend(visible_fleets)

      # empty out the list of planets in the stars. it's not needed and saves
      # a bit of space in the response
      for sector_pb in sectors_pb.sectors:
        for star_pb in sector_pb.stars:
          del star_pb.planets[:]

      # empty out the list of fleets that belong to the native empire
      for sector_pb in sectors_pb.sectors:
        real_fleets = []
        for fleet_pb in sector_pb.fleets:
          if fleet_pb.empire_key:
            real_fleets.append(fleet_pb)
        del sector_pb.fleets[:]
        sector_pb.fleets.extend(real_fleets)

      return sectors_pb
    else:
      # TODO: other ways of querying for sectors?
      self.response.set_status(400)
      return


class StarPage(StarfieldPage):
  def get(self, star_key):
    sim = simulation.Simulation()
    sim.simulate(star_key)
    star_pb = sim.getStar(star_key)

    empire_pb = empire.getEmpireForUser(self.user)

    # if you don't have a fleet or a colony here, you don't get to see what other fleets might be
    # here, so we remove them
    has_fleet_or_colony = False
    for fleet_pb in star_pb.fleets:
      if empire_pb and fleet_pb.empire_key == empire_pb.key:
        has_fleet_or_colony = True
    for colony_pb in star_pb.colonies:
      if empire_pb and colony_pb.empire_key == empire_pb.key:
        has_fleet_or_colony = True

    if not self._isAdmin() and not has_fleet_or_colony:
      del star_pb.fleets[:]

    # similarly for build requests, only our own empire's build requests
    empire_build_requests = []
    for build_request in star_pb.build_requests:
      if self._isAdmin() or build_request.empire_key == empire_pb.key:
        empire_build_requests.append(build_request)

    del star_pb.build_requests[:]
    star_pb.build_requests.extend(empire_build_requests)

    return star_pb


class StarsPage(ApiPage):
  def get(self):
    query = self.request.get("q")
    findForEmpire = self.request.get("find_for_empire")
    if query:
      return sector.getStars(query)
    elif findForEmpire == "1":
      (star_key, _) = empire.findStarForNewEmpire()
      return sector.getStar(star_key)
    else:
      self.response.set_status(404)


class StarSimulatePage(ApiPage):
  """This is a debugging page that lets us simulate a star on-demand."""

  def get(self, star_key):
    self._doSimulate(star_key, False)

  def post(self, star_key):
    self._doSimulate(star_key, self.request.get("update") == "1")

  def _doSimulate(self, star_key, do_update):
    msgs = []
    def dolog(msg):
      msgs.append(msg)

    sim = simulation.Simulation(log=dolog)
    star_pb = sim.getStar(star_key, True)
    if not star_pb:
      msgs.append("ERROR: No star with given key found!")
    else:
      msgs.append("---------- Simulating:")
      sim.simulate(star_key)

    if do_update:
      msgs.append("")
      msgs.append("---------- Updating:")
      sim.update()

    self.response.headers["Content-Type"] = "text/plain"
    self.response.out.write("\r\n".join(msgs))


class ColoniesPage(ApiPage):
  def post(self, star_key):
    # TODO: make sure they're actually allow to do this, have a free colonization
    # ship (or require that the pass in the colonization ship), etc etc etc
    empire_pb = empire.getEmpireForUser(self.user)
    colony_pb = empire.colonize(empire_pb, star_key, self._getRequestBody(pb.ColonizeRequest))
    if colony_pb is None:
      self.response.set_status(400)
    return colony_pb

  def put(self, star_key, colony_key):
    """Updates the given colony.

    When you update a colony, we need to simulate the current one first. Then we need to make
    sure the new parameters are valid (e.g. focus adds up to 1.0 etc).
    """

    # Make sure you have access to this colony!
    colony_pb = empire.getColony(colony_key)
    empire_pb = empire.getEmpireForUser(self.user)
    if colony_pb.empire_key != empire_pb.key:
      self.response.set_status(403)
      return

    sim = simulation.Simulation()
    colony_pb = empire.updateColony(colony_key, self._getRequestBody(pb.Colony), sim)
    sim.update()
    return colony_pb


class ColoniesTaxesPage(ApiPage):
  def post(self, star_key, colony_key):
    """Collect taxes from the current colony."""

    # Make sure you have access to this colony!
    colony_pb = empire.getColony(colony_key)
    empire_pb = empire.getEmpireForUser(self.user)
    if colony_pb.empire_key != empire_pb.key:
      self.response.set_status(403)
      return

    sim = simulation.Simulation()
    empire.collectTaxes(colony_pb.key, sim)
    sim.update()
    return colony_pb


class EmpireTaxesPage(ApiPage):
  def post(self, empire_key):
    empire_pb = empire.getEmpireForUser(self.user)
    if empire_pb.key != empire_key:
      self.set_status(403)
      return

    colonies_pb = empire.getColoniesForEmpire(empire_pb)
    sim = simulation.Simulation()
    for colony_pb in colonies_pb.colonies:
      empire.collectTaxes(colony_pb.key, sim)
      sim.update()


class ColoniesAttackPage(ApiPage):
  def post(self, star_key, colony_key):
    """This is called when you want to attack an enemy colony. We check that
       you have some troopcarrier ships in the star and if so, work out whether
       you destroy the colony or not."""
    colony_pb = empire.getColony(colony_key)
    empire_pb = empire.getEmpireForUser(self.user)
    if colony_pb.empire_key == empire_pb.key:
      return self.raiseError(pb.GenericError.CannotAttackOwnColony, "")
    sim = simulation.Simulation()
    empire.attackColony(empire_pb, colony_pb, sim)
    star_pb = sim.getStar(colony_pb.star_key)
    sim.update()
    return star_pb


class BuildQueuePage(ApiPage):
  def post(self):
    """The buildqueue is where you post BuildRequest protobufs with requests to build stuff."""
    request_pb = self._getRequestBody(pb.BuildRequest)

    # make sure the colony is owned by the current player
    empire_pb = empire.getEmpireForUser(self.user)
    colony_pb = empire.getColony(request_pb.colony_key)
    if not colony_pb:
      self.response.set_status(404)
      return
    if colony_pb.empire_key != empire_pb.key:
      self.response.set_status(401)
      return

    sim = simulation.Simulation()
    resp = empire.build(empire_pb, colony_pb, request_pb, sim)
    if resp == False:
      self.response.set_status(400)
      return

    sim.update()
    return resp

  def get(self):
    """Gets the build queue for the currently logged-in user."""
    empire_pb = empire.getEmpireForUser(self.user)
    logging.debug("Getting buildqueue for empire [%s] (%s)" % (
                  empire_pb.key, self.user.email()))
    return empire.getBuildQueueForEmpire(empire_pb.key)


class BuildAcceleratePage(ApiPage):
  def post(self, star_key, build_request_key):
    sim = simulation.Simulation()
    sim.simulate(star_key)
    star_pb = sim.getStar(star_key)
    for build_request_pb in star_pb.build_requests:
      if build_request_pb.key == build_request_key:
        empire_pb = empire.getEmpireForUser(self.user)
        if build_request_pb.empire_key != empire_pb.key:
          self.response.set_status(403)
          return

        res = empire.accelerateBuild(empire_pb, star_pb, build_request_pb, sim)
        sim.update()
        empire.scheduleBuildCheck(sim)
        return res

    # if we couldn't find the build request, return 404
    self.response.set_status(404)


class BuildStopPage(ApiPage):
  def post(self, star_key, build_request_key):
    sim = simulation.Simulation()
    sim.simulate(star_key)
    star_pb = sim.getStar(star_key)
    for build_request_pb in star_pb.build_requests:
      if build_request_pb.key == build_request_key:
        empire_pb = empire.getEmpireForUser(self.user)
        if build_request_pb.empire_key != empire_pb.key:
          self.response.set_status(403)
          return

        if empire.stopBuild(empire_pb, star_pb, build_request_pb, sim):
          sim.update()
          empire.scheduleBuildCheck(sim)
        else:
          self.response.set_status(400) # todo: better errors

    # if we couldn't find the build request, return 404
    self.response.set_status(404)


class FleetOrdersPage(ApiPage):
  """This page is where we post orders that we issue to fleets."""
  def post(self, star_key, fleet_key):
    order_pb = self._getRequestBody(pb.FleetOrder)
    sim = simulation.Simulation()
    sim.simulate(star_key)
    star_pb = sim.getStar(star_key)

    for fleet_pb in star_pb.fleets:
      if fleet_pb.key == fleet_key:
        # Make sure the fleet is owned by the current user!
        curr_empire_pb = empire.getEmpireForUser(self.user)
        if curr_empire_pb.key != fleet_pb.empire_key:
          self.response.set_status(403)
          return

        if not empire.orderFleet(star_pb, fleet_pb, order_pb, sim):
          self.response.set_status(400)
        else:
          self.response.set_status(200)
        sim.update()
        return

    self.response.set_status(400)


class ScoutReportsPage(ApiPage):
  """This page returns a list of scout reports for the given star."""
  def get(self, star_key):
    curr_empire_pb = empire.getEmpireForUser(self.user)

    scout_reports_pb = empire.getScoutReports(star_key, curr_empire_pb.key)
    return scout_reports_pb


class CombatReportsPage(ApiPage):
  """This page returns a list of combat reports for the given star."""
  def get(self, star_key):
    combat_reports_pb = empire.getCombatReports(star_key)
    return combat_reports_pb


class CombatReportPage(ApiPage):
  """This page returns a list of combat reports for the given star."""
  def get(self, star_key, combat_report_key):
    combat_report_pb = empire.getCombatReport(star_key, combat_report_key)
    return combat_report_pb


class SitrepPage(ApiPage):
  def get(self, star_key=None):
    empire_pb = empire.getEmpireForUser(self.user)
    return empire.getSituationReports(empire_pb.key, star_key)


class ApiApplication(webapp.WSGIApplication):
  def __init__(self, *args, **kwargs):
    webapp.WSGIApplication.__init__(self, *args, **kwargs)
    self.router.set_dispatcher(self.__class__.api_dispatcher)

  def handle_exception(self, request, response, e):
    logging.exception(e)
    if isinstance(e, webapp.HTTPException):
      response.set_status(e.code)
    else:
      response.set_status(500)

    response.headers["Content-Type"] = "text/plain"
    response.write(e)

  @staticmethod
  def api_dispatcher(router, request, response):
    rv = router.default_dispatcher(request, response)

    if isinstance(rv, message.Message):
      # if it's a protocol buffer, then we'll want to return either the
      # binary serialization, the text serialization, or a JSON
      # serialization (depending on the value of the "Accept" header)
      preferred_types = ["text/plain", "application/json", "text/json",
                         "text/x-protobuf", "application/x-protobuf"]
      content_type = ApiApplication.get_preferred_content_type(request, preferred_types)
      if content_type.endswith("/json"):
        # they want JSON
        resp = webapp.Response(protobuf_json.pb2json(rv))
      elif content_type.startswith("text/"):
        # they want a text-based format... we'll give it to them
        resp = webapp.Response(str(rv))
      else:
        # otherwise, binary protocol buffer serialization
        resp = webapp.Response(rv.SerializeToString())
      resp.headers["Content-Type"] = content_type
      return resp

    return rv

  @staticmethod
  def get_preferred_content_type(request, possible_types):
    accept = request.headers.get("Accept", "")
    content_types = [content_type.split(';')[0] for content_type in accept.split(',')]
    for content_type in content_types:
      if content_type.lower() in possible_types:
        return content_type

    # None found, just return the first one we're allowing...
    return possible_types[0]


app = ApiApplication([("/api/v1/hello/([^/]+)", HelloPage),
                      ("/api/v1/chat", ChatPage),
                      ("/api/v1/empires", EmpiresPage),
                      ("/api/v1/empires/([^/]+)", EmpiresPage),
                      ("/api/v1/empires/([^/]+)/details", EmpireDetailsPage),
                      ("/api/v1/empires/([^/]+)/taxes", EmpireTaxesPage),
                      ("/api/v1/devices", DevicesPage),
                      ("/api/v1/devices/([^/]+)", DevicesPage),
                      ("/api/v1/devices/user:([^/]+)/messages", DeviceMessagesPage),
                      ("/api/v1/buildqueue", BuildQueuePage),
                      ("/api/v1/sectors", SectorsPage),
                      ("/api/v1/stars", StarsPage),
                      ("/api/v1/stars/([^/]+)", StarPage),
                      ("/api/v1/stars/([^/]+)/simulate", StarSimulatePage),
                      ("/api/v1/stars/([^/]+)/build/([^/]+)/accelerate", BuildAcceleratePage),
                      ("/api/v1/stars/([^/]+)/build/([^/]+)/stop", BuildStopPage),
                      ("/api/v1/stars/([^/]+)/colonies", ColoniesPage),
                      ("/api/v1/stars/([^/]+)/colonies/([^/]+)", ColoniesPage),
                      ("/api/v1/stars/([^/]+)/colonies/([^/]+)/taxes", ColoniesTaxesPage),
                      ("/api/v1/stars/([^/]+)/colonies/([^/]+)/attack", ColoniesAttackPage),
                      ("/api/v1/stars/([^/]+)/fleets/([^/]+)/orders", FleetOrdersPage),
                      ("/api/v1/stars/([^/]+)/scout-reports", ScoutReportsPage),
                      ("/api/v1/stars/([^/]+)/combat-reports", CombatReportsPage),
                      ("/api/v1/stars/([^/]+)/combat-reports/([^/]+)", CombatReportPage),
                      ("/api/v1/stars/([^/]+)/sit-reports", SitrepPage),
                      ("/api/v1/sit-reports", SitrepPage)],
                     debug=os.environ["SERVER_SOFTWARE"].startswith("Development"))
