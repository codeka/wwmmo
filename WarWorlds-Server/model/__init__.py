"""model: Contains data definitions and methods to interact directly with the data store."""

import logging

from google.appengine.ext import db


class MessageOfTheDay(db.Model):
  """Message of the day is displayed to users when they first connect."""

  message = db.TextProperty()
  date = db.DateTimeProperty(auto_now=True)

  @staticmethod
  def _getKey():
    return db.Key.from_path("MessageOfTheDay", "default")

  @staticmethod
  def get():
    """Gets the current message of the day (there's only ever one)."""

    return db.get(MessageOfTheDay._getKey())

  @staticmethod
  def save(msg):
    """Saves the given message as the new message of the day."""

    motd = MessageOfTheDay.get()
    if not motd:
      motd = MessageOfTheDay(key=MessageOfTheDay._getKey())
    motd.message = msg
    motd.put()


class DeviceRegistration(db.Model):
  """Represents the details of a device registration."""

  deviceID = db.StringProperty()
  gcmRegistrationID = db.StringProperty()
  user = db.UserProperty()
  deviceModel = db.StringProperty()
  deviceManufacturer = db.StringProperty()
  deviceBuild = db.StringProperty()
  deviceVersion = db.StringProperty()

  @staticmethod
  def getByUser(user):
    """Returns all device registrations that are registered to the given user."""

    query = DeviceRegistration.all().filter('user', user)
    return DeviceRegistration._getByQuery(query)

  @staticmethod
  def _getByQuery(query):
    results = []
    for result in query:
      results.append(result)
    return results


class OnlineDevice(db.Model):
  """An online device is a device that is currently online and active in the
     game. We use this to determine who to send chats to and so on."""
  device = db.ReferenceProperty(DeviceRegistration)
  user = db.UserProperty()
  onlineSince = db.DateTimeProperty()


class ChatMessage(db.Model):
  """A chat message that was sent.

  Chat messages are saved to the data store for later querying and such. We keep them for a
  month before they're backed up and purged (to save data storage...)
  """

  user = db.UserProperty()
  message = db.StringProperty()
  postedDate = db.DateTimeProperty(auto_now=True)

