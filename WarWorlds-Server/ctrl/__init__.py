"""ctrl: The controller module contains the "business logic" for the game."""

import calendar
from datetime import datetime
import logging
import math
import time

from google.appengine.ext import db
from google.appengine.api import users
from google.appengine.api import memcache

import import_fixer
import_fixer.FixImports("google", "protobuf")

from google.protobuf.message import Message

import protobufs.messages_pb2 as pb
from model import empire as empire_mdl
import model as mdl


def getCached(keys, ProtoBuffClass):
  mc = memcache.Client()
  return mc.get_multi(keys, for_cas=True)


def setCached(mapping):
  mc = memcache.Client()
  mc.set_multi(mapping)


def clearCached(keys):
  mc = memcache.Client()
  mc.delete_multi(keys)


def deviceRegistrationPbToModel(model, pb):
  if pb.HasField("key"):
    model.key = pb.key
  model.deviceID = pb.device_id
  model.deviceRegistrationID = pb.device_registration_id
  model.deviceModel = pb.device_model
  model.deviceManufacturer = pb.device_manufacturer
  model.deviceBuild = pb.device_build
  model.deviceVersion = pb.device_version
  if pb.user:
    model.user = users.User(pb.user)


def deviceRegistrationModelToPb(pb, model):
  pb.key = str(model.key())
  pb.device_id = model.deviceID
  pb.device_registration_id = model.deviceRegistrationID
  pb.device_model = model.deviceModel
  pb.device_manufacturer = model.deviceManufacturer
  pb.device_build = model.deviceBuild
  pb.device_version = model.deviceVersion
  pb.user = model.user.email()


def empireModelToPb(empire_pb, empire_model):
  empire_pb.key = str(empire_model.key())
  empire_pb.display_name = empire_model.displayName
  empire_pb.user = empire_model.user.user_id()
  empire_pb.email = empire_model.user.email()
  empire_pb.state = empire_model.state
  if empire_model.cash:
    empire_pb.cash = empire_model.cash
  else:
    empire_pb.cash = 0


def empirePbToModel(empire_model, empire_pb):
  empire_model.displayName = empire_pb.display_name
  if empire_pb.HasField("email"):
    empire_model.user = users.User(empire_pb.email)
  empire_model.state = empire_pb.state


def colonyModelToPb(colony_pb, colony_model):
  colony_pb.key = str(colony_model.key())
  empire_key = empire_mdl.Colony.empire.get_value_for_datastore(colony_model)
  if empire_key:
    colony_pb.empire_key = str(empire_key)
  colony_pb.star_key = str(colony_model.key().parent())
  colony_pb.planet_index = colony_model.planet_index
  colony_pb.population = colony_model.population
  colony_pb.last_simulation = int(dateTimeToEpoch(colony_model.lastSimulation))
  if colony_model.focusPopulation:
    colony_pb.focus_population = colony_model.focusPopulation
  else:
    colony_pb.focus_population = 0.0
  if colony_model.focusFarming:
    colony_pb.focus_farming = colony_model.focusFarming
  else:
    colony_pb.focus_farming = 0.0
  if colony_model.focusMining:
    colony_pb.focus_mining = colony_model.focusMining
  else:
    colony_pb.focus_mining = 0.0
  if colony_model.focusConstruction:
    colony_pb.focus_construction = colony_model.focusConstruction
  else:
    colony_pb.focus_construction = 0.0
  if colony_model.uncollectedTaxes:
    colony_pb.uncollected_taxes = colony_model.uncollectedTaxes
  else:
    colony_pb.uncollected_taxes = 0
  colony_pb.defence_bonus = 1


def colonyPbToModel(colony_model, colony_pb):
  colony_model.population = colony_pb.population
  colony_model.lastSimulation = epochToDateTime(colony_pb.last_simulation)
  colony_model.focusPopulation = colony_pb.focus_population
  colony_model.focusFarming = colony_pb.focus_farming
  colony_model.focusMining = colony_pb.focus_mining
  colony_model.focusConstruction = colony_pb.focus_construction
  colony_model.uncollectedTaxes = colony_pb.uncollected_taxes


def sectorModelToPb(sector_pb, sector_model):
  sector_pb.x = sector_model.x
  sector_pb.y = sector_model.y
  if sector_model.numColonies:
    sector_pb.num_colonies = sector_model.numColonies
  else:
    sector_pb.num_colonies = 0

  for star_model in sector_model.stars:
    star_pb = sector_pb.stars.add()
    starModelToPb(star_pb, star_model)


def starModelToPb(star_pb, star_model):
  """Converts the given star model to a protocol buffer.

  Args:
    star_pb: The protocol buffer you want to populate
    star_model: The model to fetch the data from
    include_planet: If True (the default), we'll also include the planets from the
        model in the protocol buffer. If False, we'll populate num_planets and leave
        the actual planet instances out of it.
  """
  star_pb.key = str(star_model.key())
  star_pb.sector_x = star_model.sector.x
  star_pb.sector_y = star_model.sector.y
  star_pb.offset_x = star_model.x
  star_pb.offset_y = star_model.y
  star_pb.name = star_model.name.title()
  star_pb.classification = star_model.starTypeID
  star_pb.size = star_model.size
  star_pb.planets.extend(star_model.planets)
  if not star_model.timeEmptied:
    star_pb.time_emptied = 0
  else:
    star_pb.time_emptied = dateTimeToEpoch(star_model.timeEmptied)


def empirePresenceModelToPb(presence_pb, presence_model):
  presence_pb.key = str(presence_model.key())
  empire_key = empire_mdl.EmpirePresence.empire.get_value_for_datastore(presence_model)
  if empire_key:
    presence_pb.empire_key = str(empire_key)
  presence_pb.star_key = str(presence_model.key().parent())
  presence_pb.total_goods = presence_model.totalGoods
  presence_pb.total_minerals = presence_model.totalMinerals
  presence_pb.max_goods = 500
  presence_pb.max_minerals = 500


def empirePresencePbToModel(presence_model, presence_pb):
  presence_model.empire = db.Key(presence_pb.empire_key)
  presence_model.totalGoods = float(presence_pb.total_goods)
  presence_model.totalMinerals = float(presence_pb.total_minerals)


def planetModelToPb(planet_pb, planet_model):
  planet_pb.key = str(planet_model.key())
  planet_pb.index = planet_model.index
  planet_pb.planet_type = planet_model.planetTypeID + 1
  planet_pb.size = planet_model.size
  planet_pb.population_congeniality = planet_model.populationCongeniality
  planet_pb.farming_congeniality = planet_model.farmingCongeniality
  planet_pb.mining_congeniality = planet_model.miningCongeniality


def buildingModelToPb(building_pb, building_model):
  building_pb.key = str(building_model.key())
  building_pb.colony_key = str(empire_mdl.Building.colony.get_value_for_datastore(building_model))
  building_pb.design_name = building_model.designName
  if building_model.level:
    building_pb.level = building_model.level
  else:
    building_pb.level = 1


def buildRequestModelToPb(build_pb, build_model):
  build_pb.key = str(build_model.key())
  build_pb.colony_key = str(empire_mdl.BuildOperation.colony.get_value_for_datastore(build_model))
  build_pb.empire_key = str(empire_mdl.BuildOperation.empire.get_value_for_datastore(build_model))
  build_pb.design_name = build_model.designName
  build_pb.start_time = dateTimeToEpoch(build_model.startTime)
  build_pb.end_time = dateTimeToEpoch(build_model.endTime)
  build_pb.build_kind = build_model.designKind
  build_pb.progress = build_model.progress
  build_pb.count = build_model.count


def buildRequestPbToModel(build_model, build_pb):
  build_model.startTime = epochToDateTime(build_pb.start_time)
  build_model.endTime = epochToDateTime(build_pb.end_time)
  build_model.progress = float(build_pb.progress)
  build_model.count = build_pb.count


def fleetModelToPb(fleet_pb, fleet_model):
  fleet_pb.key = str(fleet_model.key())
  empire_key = empire_mdl.Fleet.empire.get_value_for_datastore(fleet_model)
  if empire_key:
    fleet_pb.empire_key = str(empire_key)
  fleet_pb.design_name = fleet_model.designName
  fleet_pb.num_ships = int(math.ceil(fleet_model.numShips))
  fleet_pb.state = fleet_model.state
  fleet_pb.state_start_time = dateTimeToEpoch(fleet_model.stateStartTime)
  fleet_pb.star_key = str(fleet_model.key().parent())
  destination_star_key = empire_mdl.Fleet.destinationStar.get_value_for_datastore(fleet_model)
  if destination_star_key:
    fleet_pb.destination_star_key = str(destination_star_key)
  else:
    fleet_pb.destination_star_key = ""
  target_fleet_key = empire_mdl.Fleet.targetFleet.get_value_for_datastore(fleet_model)
  if target_fleet_key:
    fleet_pb.target_fleet_key = str(target_fleet_key)
  else:
    fleet_pb.target_fleet_key = ""
  target_colony_key = empire_mdl.Fleet.targetColony.get_value_for_datastore(fleet_model)
  if target_colony_key:
    fleet_pb.target_colony_key = str(target_colony_key)
  else:
    fleet_pb.target_colony_key = ""
  if fleet_model.stance:
    fleet_pb.stance = fleet_model.stance
  else:
    fleet_pb.stance = pb.Fleet.NEUTRAL
  if fleet_model.lastVictory:
    fleet_pb.last_victory = dateTimeToEpoch(fleet_model.lastVictory)


def fleetPbToModel(fleet_model, fleet_pb):
  fleet_model.state = fleet_pb.state
  fleet_model.designName = fleet_pb.design_name
  if fleet_pb.empire_key:
    fleet_model.empire = db.Key(fleet_pb.empire_key)
  fleet_model.stateStartTime = epochToDateTime(fleet_pb.state_start_time)
  if fleet_pb.destination_star_key:
    fleet_model.destinationStar = db.Key(fleet_pb.destination_star_key)
  else:
    fleet_model.destinationStar = None
  if fleet_pb.target_fleet_key:
    fleet_model.targetFleet = db.Key(fleet_pb.target_fleet_key)
  else:
    fleet_model.targetFleet = None
  if fleet_pb.target_colony_key:
    fleet_model.targetColony = db.Key(fleet_pb.target_coplony_key)
  else:
    fleet_model.targetColony = None
  fleet_model.stance = fleet_pb.stance
  if fleet_pb.time_destroyed:
    fleet_model.timeDestroyed = epochToDateTime(fleet_pb.time_destroyed)
  else:
    fleet_model.timeDestroyed = None
  if fleet_pb.last_victory:
    fleet_model.lastVictory = epochToDateTime(fleet_pb.last_victory)
  else:
    fleet_model.lastVictory = None
  fleet_model.numShips = float(fleet_pb.num_ships)


def scoutReportModelToPb(scout_report_pb, scout_report_mdl):
  scout_report_pb.key = str(scout_report_mdl.key())
  scout_report_pb.empire_key = str(empire_mdl.ScoutReport.empire.get_value_for_datastore(scout_report_mdl))
  scout_report_pb.star_key = str(scout_report_mdl.key().parent())
  scout_report_pb.date = dateTimeToEpoch(scout_report_mdl.date)
  scout_report_pb.star_pb = scout_report_mdl.report


def combatReportModelToPb(combat_report_pb, combat_report_mdl, summary=False):
  combat_report_pb.key = str(combat_report_mdl.key())
  combat_report_pb.star_key = str(combat_report_mdl.key().parent())
  combat_report_pb.start_time = dateTimeToEpoch(combat_report_mdl.startTime)
  combat_report_pb.end_time = dateTimeToEpoch(combat_report_mdl.endTime)
  for empire_key in combat_report_mdl.startEmpireKeys:
    combat_report_pb.start_empire_keys.append(empire_key)
  for empire_key in combat_report_mdl.endEmpireKeys:
    combat_report_pb.end_empire_keys.append(empire_key)
  if combat_report_mdl.numDestroyed:
    combat_report_pb.num_destroyed = combat_report_mdl.numDestroyed
  if not summary:
    combat_rounds_pb = pb.CombatReport()
    combat_rounds_pb.ParseFromString(combat_report_mdl.rounds)
    combat_rounds_pb.ParseFromString(combat_report_mdl.rounds)
    combat_report_pb.key =str(len(combat_rounds_pb.rounds))
    combat_report_pb.rounds.extend(combat_rounds_pb.rounds)


def dateTimeToEpoch(dt):
  return int(time.mktime(dt.timetuple()))


def epochToDateTime(epoch):
  return datetime.fromtimestamp(epoch)


def updateDeviceRegistration(registration_pb, user):
  registration_model = mdl.DeviceRegistration()
  deviceRegistrationPbToModel(registration_model, registration_pb)

  # ignore what they said in the PB, we'll set the user to their own user anyway
  registration_model.user = user
  registration_model.put()

  deviceRegistrationModelToPb(registration_pb, registration_model)
  clearCached(["devices:for-user:%s" % (user.user_id())])
  return registration_pb


def getDevicesForUser(user_email):
  cache_key = "devices:for-user:%s" % (user_email)
  devices = getCached([cache_key], pb.DeviceRegistrations)
  if cache_key in devices:
    return devices[cache_key]

  devices_mdl = mdl.DeviceRegistration.getByUser(users.User(user_email))
  devices_pb = pb.DeviceRegistrations()
  for device_mdl in devices_mdl:
    device_pb = devices_pb.registrations.add()
    deviceRegistrationModelToPb(device_pb, device_mdl)

  setCached({cache_key: devices_pb})
  return devices_pb
