'''
Created on 11/02/2012

@author: dean@codeka.com.au
'''

from google.appengine.ext import db
import logging


class MessageOfTheDay(db.Model):
  '''Message of the day is displayed to users when they first connect.'''
  message = db.TextProperty()
  date = db.DateTimeProperty(auto_now=True)

  @staticmethod
  def _getKey():
    return db.Key.from_path('MessageOfTheDay', 'default')

  @staticmethod
  def get():
    '''Gets the current message of the day (there's only ever one)'''
    return db.get(MessageOfTheDay._getKey())

  @staticmethod
  def save(msg):
    '''Saves the given message as the new message of the day.'''
    motd = MessageOfTheDay.get()
    if not motd:
      motd = MessageOfTheDay(key=MessageOfTheDay._getKey())
    motd.message = msg
    motd.put()


class DeviceRegistration(db.Model):
  '''Represents the details of a device registration.'''
  deviceID = db.StringProperty()
  deviceRegistrationID = db.StringProperty()
  user = db.UserProperty()
  deviceModel = db.StringProperty()
  deviceManufacturer = db.StringProperty()
  deviceBuild = db.StringProperty()
  deviceVersion = db.StringProperty()

  @staticmethod
  def getByUser(user):
    '''Returns all device registrations that are registered to the given user.'''
    query = DeviceRegistration.all().filter('user', user)
    return DeviceRegistration._getByQuery(query)

  @staticmethod
  def _getByQuery(query):
    results = []
    for result in query:
      results.append(result)
    return results


class ChatClient(db.Model):
  '''A chat client is a user that's connected to the server *right now* and will receive chats.

  I'm not sure if this is the best model for this, after all, there might be more reasons why
  you'd wanty to store a list of "currently connected" clients than just chatting, but for now
  that's all I've got so that's what we'll go with.'''
  device = db.ReferenceProperty(DeviceRegistration)
  user = db.UserProperty()
  clientID = db.StringProperty()
  # TODO: more??


class ChatMessage(db.Model):
  '''A chat message that was sent.

  Chat messages are saved to the data store for later querying and such. We keep them for a
  month before they're backed up and purged (to save data storage...)'''
  user = db.UserProperty()
  message = db.StringProperty()
  postedDate = db.DateTimeProperty(auto_now=True)

