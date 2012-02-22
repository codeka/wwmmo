'''
Created on 12/02/2012

@author: dean@codeka.com.au
'''

import webapp2 as webapp
import model
from model import c2dm, sector

import import_fixer
import_fixer.FixImports('google', 'protobuf')

from protobufs import protobuf_json, warworlds_pb2 as pb
from google.protobuf import message

from google.appengine.api import users
import logging


class ApiPage(webapp.RequestHandler):
    '''This is the base class for pages in the API section.
    '''
    def dispatch(self):
        '''Checks that a user is logged in and such before we process the request.
        '''
        self.user = users.get_current_user()
        if not self.user:
            # not logged in, return 403 Forbidden
            self.response.set_status(403)
            return

        return super(ApiPage, self).dispatch()

    def _getRequestBody(self, ProtoBuffClass):
        """ Gets the body of the request as a protocol buffer.

        We check whether the body is application/json or application/x-protobuf before
        decoding the message. They're the two main kinds of requests we accept.
        """
        pb = ProtoBuffClass()

        contentType = self.request.headers['Content-Type']
        contentType = contentType.split(';')[0].strip()
        if contentType == 'application/json':
            protobuf_json.json2pb(pb, self.request.body)
        elif contentType == 'application/x-protobuf':
            pb.ParseFromString(self.request.body)
        return pb


class MotdPage(ApiPage):
    def get(self):
        data = model.MessageOfTheDay.get()

        motd = pb.MessageOfTheDay()
        motd.message = data.message
        motd.last_update = data.date.isoformat()
        return motd


class DevicesPage(ApiPage):
    def put(self):
        registration_info = self._getRequestBody(pb.DeviceRegistration)
        if registration_info == None:
            # handle errors...
            self.response.set_status(400)
            return

        data = model.DeviceRegistration()
        self._pbToModel(data, registration_info)
        # ignore what they said in the PB, we'll set the user to their own user anyway
        data.user = self.user.email()
        data.put()

    def get(self):
        if self.request.get('user') != '':
            # TODO: check that you're an administrator...
            return self._getDevicesByEmail(self.request.get('user'))
        else:
            # if you don't pass any parameters, it's like searching for your
            # own device registrations.
            return self._getDevicesByEmail(self.user.email())

    def delete(self, deviceRegistrationID):
        device = model.DeviceRegistration.getByRegistrationID(deviceRegistrationID)
        if device != None:
            device.delete()

    def _getDevicesByEmail(self, email):
        data = model.DeviceRegistration.getByEmail(email)

        registrations = pb.DeviceRegistrations()
        for d in data:
            registration = registrations.registrations.add()
            self._modelToPb(registration, d)
        return registrations

    def _pbToModel(self, model, pb):
        model.deviceID = pb.device_id
        model.deviceRegistrationID = pb.device_registration_id
        model.deviceModel = pb.device_model
        model.deviceManufacturer = pb.device_manufacturer
        model.deviceBuild = pb.device_build
        model.deviceVersion = pb.device_version
        if pb.user:
            model.user = pb.user

    def _modelToPb(self, pb, model):
        pb.device_id = model.deviceID
        pb.device_registration_id = model.deviceRegistrationID
        pb.device_model = model.deviceModel
        pb.device_manufacturer = model.deviceManufacturer
        pb.device_build = model.deviceBuild
        pb.device_version = model.deviceVersion
        pb.user = model.user


class DeviceMessagesPage(ApiPage):
    def put(self, user):
        msg = self._getRequestBody(pb.DeviceMessage)
        logging.info('Putting message to user: '+user)
        logging.info('Message: '+str(msg))

        s = c2dm.Sender()
        devices = model.DeviceRegistration.getByEmail(user)
        for device in devices:
            s.sendMessage(device.deviceRegistrationID, {"msg": msg.message})
        return None


class StarfieldPage(ApiPage):
    """Base class for API pages that do things with the starfield.

    This really just include common methods for converting between the model and protocol buffers.
    """
    def _starModelToPb(self, star_pb, star_model):
        star_pb.id = star_model.starID
        star_pb.offset_x = star_model.x
        star_pb.offset_y = star_model.y
        star_pb.name = star_model.name
        star_pb.colour = star_model.colour
        star_pb.size = star_model.size
        if star_model.planets is not None:
            for planet_model in star_model.planets:
                planet_pb = star_pb.planets.add()
                self._planetModelToPb(planet_pb, planet_model)
            star_pb.num_planets = len(star_model.planets)
        else:
            star_pb.num_planets = 3 # TODO

    def _planetModelToPb(self, planet_pb, planet_model):
        planet_pb.index = planet_model.index
        planet_pb.planet_type = planet_model.planetTypeID+1
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

            for star_model in sector_model.stars:
                star_pb = sector_pb.stars.add()
                self._starModelToPb(star_pb, star_model)


class StarPage(StarfieldPage):
    def get(self, sectorX, sectorY, starID):
        star_model = sector.SectorManager.getStar(int(sectorX), int(sectorY), int(starID))
        if star_model is None:
            self.response.set_status(404)
            return

        star_pb = pb.Star()
        self._starModelToPb(star_pb, star_model)
        return star_pb


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


app = ApiApplication([('/api/v1/motd', MotdPage),
                      ('/api/v1/devices', DevicesPage),
                      ('/api/v1/devices/registration:(.+)', DevicesPage),
                      ('/api/v1/devices/user:([^/]+)/messages', DeviceMessagesPage),
                      ('/api/v1/sectors', SectorsPage),
                      ('/api/v1/sectors/([0-9-]+),([0-9-]+)/stars/([0-9]+)', StarPage)],
                     debug=True)
