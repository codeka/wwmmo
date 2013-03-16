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

  alliance_pb.num_members = 1
  return alliance_pb