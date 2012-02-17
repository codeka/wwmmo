'''
Created on 12/02/2012

@author: dean@codeka.com.au
'''

import webapp2 as webapp
import model

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

        TODO: error checking, make sure it's a binary protocol buffer, etc etc
        """
        pb = ProtoBuffClass()
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
        data.deviceID = registration_info.device_id
        data.deviceRegistrationID = registration_info.device_registration_id
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

    def _getDevicesByEmail(self, email):
        data = model.DeviceRegistration.getByEmail(email)

        registrations = pb.DeviceRegistrations()
        for d in data:
            registration = registrations.registrations.add()
            registration.device_id = d.deviceID
            registration.device_registration_id = d.deviceRegistrationID
            registration.user = d.user
        return registrations

class ApiApplication(webapp.WSGIApplication):
    def __init__(self, *args, **kwargs):
        webapp.WSGIApplication.__init__(self, *args, **kwargs)
        self.router.set_dispatcher(self.__class__.api_dispatcher)

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
                      ('/api/v1/devices', DevicesPage)],
                     debug=True)
