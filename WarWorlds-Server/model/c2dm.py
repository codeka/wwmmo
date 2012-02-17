'''
Created on 17/02/2012

@author: dean@codeka.com.au
'''

import logging
import os
import urllib, urllib2

class ClientLoginTokenFactory:
    _token = None

    def __init__(self, email, passwd):
        self.url = 'https://www.google.com/accounts/ClientLogin'
        self.account_type = 'HOSTED_OR_GOOGLE'
        self.email = email
        self.password = passwd
        self.source = 'C2DMVALIDACCOUNT-C2DM-1'
        self.service = 'ac2dm'


    def getToken(self):
        if(self._token is None):
            values = {'accountType' : self.account_type,
                      'Email' : self.email,
                      'Passwd' : self.password,
                      'source' : self.source,
                      'service' : self.service}
            data = urllib.urlencode(values)
            request = urllib2.Request(self.url, data)
            response = urllib2.urlopen(request)
            responseAsString = response.read()
            responseAsList = responseAsString.split('\n')
            self._token = responseAsList[2].split('=')[1]
        return self._token


class Sender:
    def __init__(self):
        self.auth_email = 'warworlds.app-role@codeka.com.au'
        self.auth_passwd = 'adv18997' # TODO: store this in a more secure place!!

        self.url = 'https://android.apis.google.com/c2dm/send'
        self.token_factory = ClientLoginTokenFactory(self.auth_email, self.auth_passwd)
        self.collapse_key = ''

        if os.environ['SERVER_SOFTWARE'].startswith('Development'):
            # the development server doesn't like the SSL certificate returned from
            # android.apis.google.com. So we switch to http: instead. It's not as
            # secure, but for the dev server, who cares?
            logging.warn('Local development server, using http instead of https for C2DM')
            self.url = 'http://android.apis.google.com/c2dm/send'

    def sendMessage(self, deviceRegistrationID, msg):
        if self.collapse_key == '':
            self.collapse_key = 'default'
        values = {'collapse_key': self.collapse_key,
                  'registration_id': deviceRegistrationID}
        for key in msg:
            values["data."+key] = msg[key]

        body = urllib.urlencode(values)
        request = urllib2.Request(self.url, body)
        request.add_header('Authorization', 'GoogleLogin auth=' + self.token_factory.getToken())
        response = urllib2.urlopen(request)
        if(response.code == 200):
            logging.info('Sent message to device: '+response.read())
            return True
        return False
