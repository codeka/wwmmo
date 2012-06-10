"""api_v1.py: The handlers for the actual API that the client calls."""

import os
import logging

from google.appengine.api import channel
from google.appengine.api import users

import webapp2 as webapp

import import_fixer
import_fixer.FixImports("google", "protobuf")

import model
from model import c2dm
from ctrl import sector
from ctrl import empire
import ctrl

from protobufs import protobuf_json, warworlds_pb2 as pb
from google.protobuf import message



class ApiPage(webapp.RequestHandler):
  """This is the base class for pages in the API section."""

  def dispatch(self):
    """Checks that a user is logged in and such before we process the request."""

    self.user = users.get_current_user()
    if not self.user:
      # not logged in, return 403 Forbidden
      self.response.set_status(403)
      return

    return super(ApiPage, self).dispatch()

  def _isAdmin(self):
    # TODO: better version of "isAdmin!"
    return self.user.email() == "dean@codeka.com.au"

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
      device = None
    else:
      device = model.DeviceRegistration.get(deviceRegistrationKey)
      if device is None:
        # ERROR
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

    if empire_pb is not None:
      hello_pb.empire.MergeFrom(empire_pb)

    if colonies_pb is not None:
      hello_pb.colonies.MergeFrom(colonies_pb.colonies)

    # generate a chat client for them
    channelClientID = "device:" + device.deviceID
    chatClient = None
    for cc in model.ChatClient.all().filter("clientID", channelClientID):
      chatClient = cc
    if chatClient is None:
      chatClient = model.ChatClient()
      chatClient.user = user
      chatClient.device = device
      chatClient.clientID = channelClientID
      chatClient.put()
    hello_pb.channel_token = channel.create_channel(channelClientID)
    hello_pb.channel_client_id = channelClientID

    return hello_pb


class ChatPage(ApiPage):
  def post(self):
    msg_pb = self._getRequestBody(pb.ChatMessage)
    msg_model = model.ChatMessage()
    msg_model.user = self.user
    msg_model.message = msg_pb.message
    msg_model.put()

    # send the chat out to all connected clients (TODO: only some?)
    for cc in model.ChatClient.all():
      try:
        channel.send_message(cc.clientID, msg_pb.message)
      except:
        # TODO: handle errors?
        pass


class EmpiresPage(ApiPage):
  def get(self, empireKey=None):
    empire_pb = None

    if empireKey:
      empire_pb = empire.getEmpire(empireKey)
    elif self.request.get("email", "") != "":
      email = self.request.get("email")
      user = users.User(email)
      if user is None:
        logging.info("No user found with email address '" + email + "'")
        self.response.set_status(404)
        return

      empire_pb = empire.getEmpireForUser(user)

    if empire_pb is None:
      self.response.set_status(404)
      return

    return empire_pb

  def put(self):
    empire_pb = self._getRequestBody(pb.Empire)
    if empire_pb is None:
      self.response.set_status(400)
      return

    empire_pb.email = self.user.email()

    empire.createEmpire(empire_pb)


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


class DeviceMessagesPage(ApiPage):
  def put(self, email):
    msg = self._getRequestBody(pb.DeviceMessage)
    logging.info("Putting message to user with email: %s" % (email))
    logging.info("Message: %s" % (msg))

    user = users.User(email)
    if user is None:
      self.response.set_status(404)
      return

    s = c2dm.Sender()
    devices = ctrl.getDevicesForUser(user.email())
    for device in devices.registrations:
      s.sendMessage(device.device_registration_id, {"msg": msg.message})
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

      return sector.getSectors(coords)
    else:
      # TODO: other ways of querying for sectors?
      self.response.set_status(400)
      return


class StarPage(StarfieldPage):
  def get(self, key):
    star_pb = sector.getStar(key)
    empire.simulate(star_pb)

    empire_pb = empire.getEmpireForUser(self.user)

    # we only return fleets for the current empire -- we don't let you see
    # other empire's fleets.
    empire_fleets = []
    for fleet in star_pb.fleets:
      if fleet.empire_key == empire_pb.key:
        empire_fleets.append(fleet)

    del star_pb.fleets[:]
    star_pb.fleets.extend(empire_fleets)

    # similarly for build requests, only our own empire's build requests
    empire_build_requests = []
    for build_request in star_pb.build_requests:
      if build_request.empire_key == empire_pb.key:
        empire_build_requests.append(build_request)

    del star_pb.build_requests[:]
    star_pb.build_requests.extend(empire_build_requests)

    return star_pb


class StarSimulatePage(ApiPage):
  """This is a debugging page that lets us simulate a star on-demand."""

  def get(self, starKey):
    self._doSimulate(starKey, False)

  def post(self, starKey):
    self._doSimulate(starKey, self.request.get("update") == "1")

  def _doSimulate(self, starKey, doUpdate):
    msgs = []
    def dolog(msg):
      msgs.append(msg)

    star_pb = sector.getStar(starKey)
    if not star_pb:
      msgs.append("ERROR: No star with given key found!")
    else:
      msgs.append("---------- Simulating:")
      empire.simulate(star_pb, log=dolog)

    if doUpdate:
      msgs.append("")
      msgs.append("---------- Updating:")
      empire.updateAfterSimulate(star_pb, None)

    self.response.headers["Content-Type"] = "text/plain"
    self.response.out.write("\r\n".join(msgs))


class ColoniesPage(ApiPage):
  def post(self):
    # TODO: make sure they're actually allow to do this, have a free colonization
    # ship (or require that the pass in the colonization ship), etc etc etc
    empire_pb = empire.getEmpireForUser(self.user)
    colony_pb = empire.colonize(empire_pb, self._getRequestBody(pb.ColonizeRequest))
    if colony_pb is None:
      self.response.set_status(400)
    return colony_pb

  def put(self, colony_key):
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

    colony_pb = empire.updateColony(colony_key, self._getRequestBody(pb.Colony))
    return colony_pb
  

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
      self.response.set_status(403)
      return

    resp = empire.build(empire_pb, colony_pb, request_pb)
    if resp == False:
      self.response.set_status(400)
      return
    return resp

  def get(self):
    """Gets the build queue for the currently logged-in user."""

    empire_pb = empire.getEmpireForUser(self.user)
    return empire.getBuildQueueForEmpire(empire_pb.key)


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
                      ("/api/v1/devices", DevicesPage),
                      ("/api/v1/devices/([^/]+)", DevicesPage),
                      ("/api/v1/devices/user:([^/]+)/messages", DeviceMessagesPage),
                      ("/api/v1/sectors", SectorsPage),
                      ("/api/v1/stars/([^/]+)", StarPage),
                      ("/api/v1/stars/([^/]+)/simulate", StarSimulatePage),
                      ("/api/v1/colonies", ColoniesPage),
                      ("/api/v1/colonies/([^/]+)", ColoniesPage),
                      ("/api/v1/buildqueue", BuildQueuePage)],
                     debug=os.environ["SERVER_SOFTWARE"].startswith("Development"))
