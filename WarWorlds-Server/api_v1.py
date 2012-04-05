'''
Created on 12/02/2012

@author: dean@codeka.com.au
'''

import webapp2 as webapp
import model
from model import c2dm, sector, empire

import import_fixer
import_fixer.FixImports('google', 'protobuf')

from protobufs import protobuf_json, warworlds_pb2 as pb
from google.protobuf import message

from google.appengine.api import channel
from google.appengine.api import users
import logging


class ApiPage(webapp.RequestHandler):
  '''This is the base class for pages in the API section.'''
  def dispatch(self):
    '''Checks that a user is logged in and such before we process the request.'''
    self.user = users.get_current_user()
    if not self.user:
      # not logged in, return 403 Forbidden
      self.response.set_status(403)
      return

    return super(ApiPage, self).dispatch()

  def _isAdmin(self):
    # TODO: better version of "isAdmin!"
    return self.user.email() == 'dean@codeka.com.au'

  def _getRequestBody(self, ProtoBuffClass):
    ''' Gets the body of the request as a protocol buffer.

    We check whether the body is application/json or application/x-protobuf before
    decoding the message. They're the two main kinds of requests we accept.
    '''
    pb = ProtoBuffClass()

    contentType = self.request.headers['Content-Type']
    contentType = contentType.split(';')[0].strip()
    if contentType == 'application/json':
      protobuf_json.json2pb(pb, self.request.body)
    elif contentType == 'application/x-protobuf':
      pb.ParseFromString(self.request.body)
    return pb

  def _getEmpire(self):
    '''Returns the empire for the current player.'''
    return empire.Empire.getForUser(self.user)

  def _empireModelToPb(self, empire_pb, empire_model):
    empire_pb.key = str(empire_model.key())
    empire_pb.display_name = empire_model.displayName
    empire_pb.user = empire_model.user.user_id()
    empire_pb.email = empire_model.user.email()
    empire_pb.state = empire_model.state

  def _empirePbToModel(self, empire_model, empire_pb):
    empire_model.displayName = empire_pb.display_name
    if empire_pb.HasField('email'):
      empire_model.user = users.User(empire_pb.email)
    empire_model.state = empire_pb.state

  def _colonyModelToPb(self, colony_pb, colony_model):
    colony_pb.key = str(colony_model.key())
    colony_pb.empire_key = str(empire.Colony.empire.get_value_for_datastore(colony_model))
    colony_pb.star_key = str(empire.Colony.star.get_value_for_datastore(colony_model))
    colony_pb.planet_key = str(empire.Colony.planet.get_value_for_datastore(colony_model))
    colony_pb.population = colony_model.population
    colony_pb.population_rate = colony_model.populationRate
    #colony_pb.last_simulation = colony_model.lastSimulation


class HelloPage(ApiPage):
  '''The 'hello' page is what you request when you first connect.'''
  def get(self):
    '''This can be used by the administrator for testing only...'''
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
    empire_model = empire.Empire.getForUser(user)
    colony_models = empire.Colony.getForEmpire(empire_model)

    hello_pb = pb.Hello()
    if motd_model is not None:
      hello_pb.motd.message = motd_model.message
      hello_pb.motd.last_update = motd_model.date.isoformat()
    else:
      hello_pb.motd.message = ""
      hello_pb.motd.last_update = ""

    if empire_model is not None:
      self._empireModelToPb(hello_pb.empire, empire_model)

    for colony_model in colony_models:
      colony_pb = hello_pb.colonies.add()
      self._colonyModelToPb(colony_pb, colony_model)

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
  def get(self):
    if self.request.get("email", "") != "":
      email = self.request.get("email")
      user = users.User(email)
      if user is None:
        logging.info("No user found with email address '" + email + "'")
        self.response.set_status(404)
        return

      empire_model = empire.Empire.getForUser(user)
      if empire_model is None:
        logging.info("No empire registered with email address '" + email + "'")
        self.response.set_status(404)
        return

      empire_pb = pb.Empire()
      self._empireModelToPb(empire_pb, empire_model)
      return empire_pb
    else:
      self.response.set_status(403)
      return

  def put(self):
    empire_pb = self._getRequestBody(pb.Empire)
    if empire_pb is None:
      self.response.set_status(400)
      return

    empire_model = empire.Empire()
    self._empirePbToModel(empire_model, empire_pb)
    empire_model.put()


class DevicesPage(ApiPage):
  def post(self):
    registration_pb = self._getRequestBody(pb.DeviceRegistration)
    if registration_pb == None:
      # handle errors...
      self.response.set_status(400)
      return

    registration_model = model.DeviceRegistration()
    self._pbToModel(registration_model, registration_pb)
    # ignore what they said in the PB, we'll set the user to their own user anyway
    registration_model.user = self.user
    registration_model.put()

    self._modelToPb(registration_pb, registration_model)
    return registration_pb

  def get(self):
    if self.request.get('email') != '':
      # TODO: check that you're an administrator...
      return self._getDevicesByEmail(int(self.request.get('email')))
    else:
      # if you don't pass any parameters, it's like searching for your
      # own device registrations.
      return self._getDevicesByUser(self.user)

  def delete(self, key):
    device = model.DeviceRegistration.get(key)
    if device != None:
      # make sure you only ever delete devices you own
      if device.user != self.user:
        self.response.set_status(403)
        return
      device.delete()
    else:
      logging.warn("No device with key [" + key + "] to delete.")

  def _getDevicesByEmail(self, email):
    user = users.User(email)
    return self._getDevicesByUser(user)

  def _getDevicesByUser(self, user):
    data = model.DeviceRegistration.getByUser(user)

    registrations = pb.DeviceRegistrations()
    for d in data:
      registration = registrations.registrations.add()
      self._modelToPb(registration, d)
    return registrations

  def _pbToModel(self, model, pb):
    if pb.HasField('key'):
      model.key = pb.key
    model.deviceID = pb.device_id
    model.deviceRegistrationID = pb.device_registration_id
    model.deviceModel = pb.device_model
    model.deviceManufacturer = pb.device_manufacturer
    model.deviceBuild = pb.device_build
    model.deviceVersion = pb.device_version
    if pb.user:
      model.user = users.User(pb.user)

  def _modelToPb(self, pb, model):
    pb.key = str(model.key())
    pb.device_id = model.deviceID
    pb.device_registration_id = model.deviceRegistrationID
    pb.device_model = model.deviceModel
    pb.device_manufacturer = model.deviceManufacturer
    pb.device_build = model.deviceBuild
    pb.device_version = model.deviceVersion
    pb.user = model.user.email()


class DeviceMessagesPage(ApiPage):
  def put(self, email):
    msg = self._getRequestBody(pb.DeviceMessage)
    logging.info('Putting message to user with email: ' + email)
    logging.info('Message: ' + str(msg))

    user = users.User(email)
    if user is None:
      self.response.set_status(404)
      return

    s = c2dm.Sender()
    devices = model.DeviceRegistration.getByUser(user)
    for device in devices:
      s.sendMessage(device.deviceRegistrationID, {"msg": msg.message})
    return None


class StarfieldPage(ApiPage):
  '''Base class for API pages that do things with the starfield.

  This really just include common methods for converting between the model and protocol buffers.
  '''
  def _starModelToPb(self, star_pb, star_model):
    star_pb.key = str(star_model.key())
    star_pb.offset_x = star_model.x
    star_pb.offset_y = star_model.y
    star_pb.name = star_model.name
    star_pb.colour = star_model.colour
    star_pb.classification = star_model.starTypeIndex
    star_pb.size = star_model.size
    if star_model.planets is not None:
      for planet_model in star_model.planets:
        planet_pb = star_pb.planets.add()
        self._planetModelToPb(planet_pb, planet_model)
      star_pb.num_planets = len(star_model.planets)
    else:
      star_pb.num_planets = 3 # TODO

  def _planetModelToPb(self, planet_pb, planet_model):
    planet_pb.key = str(planet_model.key())
    planet_pb.index = planet_model.index
    planet_pb.planet_type = planet_model.planetTypeID + 1
    planet_pb.size = planet_model.size


class SectorsPage(StarfieldPage):
  def get(self):
    if self.request.get('coords') != '':
      (x1y1, x2y2) = self.request.get('coords').split(':', 2)
      (x1, y1) = x1y1.split(',', 2)
      (x2, y2) = x2y2.split(',', 2)

      x1 = int(x1)
      y1 = int(y1)
      x2 = int(x2)
      y2 = int(y2)

      sector_model = sector.SectorManager.getSectors(x1, y1, x2, y2)
      sectors_pb = pb.Sectors()
      self._modelToPb(sectors_pb, sector_model)
      return sectors_pb
    else:
      # TODO: other ways of querying for sectors?
      self.response.set_status(400)
      return

  def _modelToPb(self, sectors_pb, sectors_model):
    for key in sectors_model:
      sector_model = sectors_model[key]
      sector_pb = sectors_pb.sectors.add()
      sector_pb.x = sector_model.x
      sector_pb.y = sector_model.y
      if sector_model.numColonies:
        sector_pb.num_colonies = sector_model.numColonies
      else:
        sector_pb.num_colonies = 0

      for star_model in sector_model.stars:
        star_pb = sector_pb.stars.add()
        self._starModelToPb(star_pb, star_model)

      for colony_model in empire.Colony.getForSector(sector_model):
        colony_pb = sector_pb.colonies.add()
        self._colonyModelToPb(colony_pb, colony_model)


class StarPage(StarfieldPage):
  def get(self, key):
    star_model = sector.SectorManager.getStar(key)
    if star_model is None:
      self.response.set_status(404)
      return

    star_pb = pb.Star()
    self._starModelToPb(star_pb, star_model)

    for colony_model in empire.Colony.getForStar(star_model):
      colony_pb = star_pb.colonies.add()
      self._colonyModelToPb(colony_pb, colony_model)

    return star_pb


class ColoniesPage(ApiPage):
  def post(self):
    # TODO: make sure they're actually allow to do this, have a free colonization
    # ship (or require that the pass in the colonization ship), etc etc etc
    req = self._getRequestBody(pb.ColonizeRequest)
    planet_model = sector.Planet.get(req.planet_key)
    if planet_model is None:
      self.response.set_status(400)
      return

    empire_model = self._getEmpire()
    colony_model = empire_model.colonize(planet_model)

    colony_pb = pb.Colony()
    self._colonyModelToPb(colony_pb, colony_model)
    return colony_pb

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

    response.headers['Content-Type'] = 'text/plain'
    response.write(e)

  @staticmethod
  def api_dispatcher(router, request, response):
    rv = router.default_dispatcher(request, response)

    if isinstance(rv, message.Message):
      # if it's a protocol buffer, then we'll want to return either the
      # binary serialization, or the text serialization (depending on
      # the value of the "Accept" header)
      preferred_types = ['text/plain', 'application/json', 'text/json',
                         'text/x-protobuf', 'application/x-protobuf']
      content_type = ApiApplication.get_preferred_content_type(request, preferred_types)
      if content_type.endswith('/json'):
        # they want JSON
        resp = webapp.Response(protobuf_json.pb2json(rv))
      elif content_type.startswith('text/'):
        # they want a text-based format... we'll give it to them
        resp = webapp.Response(str(rv))
      else:
        # otherwise, binary protocol buffer serialization
        resp = webapp.Response(rv.SerializeToString())
      resp.headers['Content-Type'] = content_type
      return resp

    return rv

  @staticmethod
  def get_preferred_content_type(request, possible_types):
    accept = request.headers.get('Accept', '')
    content_types = [content_type.split(';')[0] for content_type in accept.split(',')]
    for content_type in content_types:
      if content_type.lower() in possible_types:
        return content_type

    # None found, just return the first one we're allowing...
    return possible_types[0]


app = ApiApplication([('/api/v1/hello/([^/]+)', HelloPage),
                      ('/api/v1/chat', ChatPage),
                      ('/api/v1/empires', EmpiresPage),
                      ('/api/v1/empires/([^/]+)', EmpiresPage),
                      ('/api/v1/devices', DevicesPage),
                      ('/api/v1/devices/([^/]+)', DevicesPage),
                      ('/api/v1/devices/user:([^/]+)/messages', DeviceMessagesPage),
                      ('/api/v1/sectors', SectorsPage),
                      ('/api/v1/stars/([^/]+)', StarPage),
                      ('/api/v1/colonies', ColoniesPage)],
                     debug=True)
