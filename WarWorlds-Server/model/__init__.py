'''
Created on 11/02/2012

@author: dean@codeka.com.au
'''

from google.appengine.ext import db
import logging


class MessageOfTheDay(db.Model):
    '''Message of the day is displayed to users when they first connect.
    '''
    message = db.TextProperty()
    date = db.DateTimeProperty(auto_now=True)

    @staticmethod
    def _getKey():
        return db.Key.from_path('MessageOfTheDay', 'default')

    @staticmethod
    def get():
        '''Gets the current message of the day (there's only ever one)
        '''
        return db.get(MessageOfTheDay._getKey())

    @staticmethod
    def save(msg):
        '''Saves the given message as the new message of the day.
        '''
        motd = MessageOfTheDay.get()
        if not motd:
            motd = MessageOfTheDay(key=MessageOfTheDay._getKey())
        motd.message = msg
        motd.put()


class DeviceRegistration(db.Model):
    """ Represents the details of a device registration.
    """
    deviceID = db.StringProperty()
    deviceRegistrationID = db.StringProperty()
    user = db.UserProperty()
    deviceModel = db.StringProperty()
    deviceManufacturer = db.StringProperty()
    deviceBuild = db.StringProperty()
    deviceVersion = db.StringProperty()

    @staticmethod
    def getByUser(user):
        """ Returns all device registrations that are registered to the given user.
        """
        query = DeviceRegistration.all().filter('user', user)
        return DeviceRegistration._getByQuery(query)

    @staticmethod
    def getByRegistrationID(deviceRegistrationID):
        """ Returns a device registration, given the deviceRegistrationID.
        """
        query = DeviceRegistration.all().filter('deviceRegistrationID', deviceRegistrationID)
        devices = DeviceRegistration._getByQuery(query)
        # there's only one so we just return that
        if len(devices) == 1:
            return devices[0]
        else:
            return None

    @staticmethod
    def _getByQuery(query):
        results = []
        for result in query:
            results.append(result)
        return results


class Empire(db.Model):
    """Represents an empire, display name and whatnot.
    """
    displayName = db.StringProperty()
    user = db.UserProperty()
    state = db.IntegerProperty()

    @staticmethod
    def getForUser(user):
        result = Empire.all().filter("user", user).fetch(1, 0)
        if len(result) != 1:
            return None
        return result[0]

    class State:
        INITIAL = 1
        REGISTERED = 2
        BANNED = 3
