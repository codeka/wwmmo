"""alliance.py: Controllers relating to the alliance system."""

from datetime import datetime
import logging

import ctrl
from ctrl import empire as empire_ctl
from model import alliance as mdl

from protobufs import messages_pb2 as pb

from google.appengine.ext import db


def createAlliance(alliance_pb, empire_pb):
  """Creates a new alliance, with the given empire as the 'creator'."""
  if not empire_ctl.subtractCash(empire_pb.key, 250000,
                                 ("Created alliance '%s'" % (alliance_pb.name))):
    raise ctrl.ApiError(pb.GenericError.InsufficientCash,
                        "You don't have enough cash to create a new alliance.")

  logging.info("Creating alliance '%s'" % (alliance_pb.name))
  alliance_mdl = mdl.Alliance()
  alliance_mdl.name = alliance_pb.name
  alliance_mdl.creator = db.Key(empire_pb.key)
  alliance_mdl.createdDate = ctrl.epochToDateTime(alliance_pb.time_created)
  alliance_mdl.numMembers = 1
  alliance_mdl.put()

  # also add this guy as a member
  alliance_member_mdl = mdl.AllianceMember(parent=alliance_mdl)
  alliance_member_mdl.empire = db.Key(empire_pb.key)
  alliance_member_mdl.joinDate = datetime.now()
  alliance_member_mdl.put()

  ctrl.clearCached(["alliances:all"])

  alliance_pb.num_members = 1
  return alliance_pb


def getAlliance(alliance_key):
  """Fetches details of the given alliance, including members."""
  cache_key = "alliances:%s" % alliance_key
  values = ctrl.getCached([cache_key], pb.Alliance)
  if cache_key in values:
    return values[cache_key]

  alliance_mdl = mdl.Alliance.get(db.Key(alliance_key))
  if not alliance_mdl:
    return None
  alliance_pb = pb.Alliance()
  ctrl.allianceModelToPb(alliance_pb, alliance_mdl)

  # fetch the members for this alliance, too
  for alliance_member_mdl in mdl.AllianceMember.all().ancestor(alliance_mdl):
    alliance_member_pb = alliance_pb.members.add()
    ctrl.allianceMemberModelToPb(alliance_member_pb, alliance_member_mdl)

  ctrl.setCached({cache_key: alliance_pb})
  return alliance_pb


def getAlliances():
  """Fetches all the alliances we have on the server."""
  cache_key = "alliances:all"
  values = ctrl.getCached([cache_key], pb.Alliances)
  if cache_key in values:
    return values[cache_key]

  alliances_pb = pb.Alliances()
  for alliance_mdl in mdl.Alliance.all():
    alliance_pb = alliances_pb.alliances.add()
    ctrl.allianceModelToPb(alliance_pb, alliance_mdl)

  ctrl.setCached({cache_key: alliances_pb})
  return alliances_pb
