"""model: Contains data definitions and methods to interact directly with the data store."""

import logging
import random

from google.appengine.ext import db

from model import empire as empire_mdl


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


class ShardedCounter(db.Model):
  count = db.IntegerProperty(default=0)
  name = db.StringProperty()

  DEF_SHARDS = 10

  @staticmethod
  def getCount(name):
    total = 0
    for counter in ShardedCounter.all().filter("name", name):
      total += counter.count
    return total

  @staticmethod
  def increment(name, delta=1, num_shards=DEF_SHARDS):
    key_name = name+":"+str(random.randint(1, num_shards))

    def do_incr(key_name, name):
      counter = ShardedCounter.get_by_key_name(key_name)
      if not counter:
        counter = ShardedCounter(key_name=key_name)
        counter.name = name
      counter.count += delta
      counter.put()
    db.run_in_transaction(do_incr, key_name, name)

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


class LoginHistory(db.Model):
  userEmail = db.StringProperty()
  userID = db.StringProperty()
  empire = db.ReferenceProperty(empire_mdl.Empire)
  empireName = db.StringProperty()
  date = db.DateTimeProperty()
  deviceModel = db.StringProperty()
  deviceManufacturer = db.StringProperty()
  deviceBuild = db.StringProperty()
  deviceVersion = db.StringProperty()
  memoryClass = db.IntegerProperty()


class OnlineDevice(db.Model):
  """An online device is a device that is currently online and active in the
     game. We use this to determine who to send chats to and so on."""
  device = db.ReferenceProperty(DeviceRegistration)
  user = db.UserProperty()
  onlineSince = db.DateTimeProperty()


