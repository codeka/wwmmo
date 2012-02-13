'''
Created on 12/02/2012

@author: dean@codeka.com.au
'''
import webapp2 as webapp
import model

import import_fixer
import_fixer.FixImports('google', 'protobuf')

from protobufs import warworlds_pb2 as pb
from google.protobuf import message

from google.appengine.api import users


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



class MotdPage(ApiPage):
    def get(self):
        data = model.MessageOfTheDay.get()

        motd = pb.MessageOfTheDay()
        motd.message = data.message
        motd.last_update = data.date.isoformat()
        return motd


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
            preferred_types = ['text/plain', 'text/x-protobuf', 'application/x-protobuf']
            content_type = ApiApplication.get_preferred_content_type(request, preferred_types)
            if content_type.startswith('text/'):
                # they want a text-based format... we'll give it to them
                resp = webapp.Response(str(rv))
            else:
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


app = ApiApplication([('/api/v1/motd', MotdPage)],
                     debug=True)
