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


def requestJoin(alliance_pb, alliance_join_request_pb):
  """Adds a request to join an alliance to the queue of join requests."""
  alliance_join_request_mdl = mdl.AllianceJoinRequest(parent=db.Key(alliance_pb.key))
  alliance_join_request_mdl.empire = db.Key(alliance_join_request_pb.empire_key)
  alliance_join_request_mdl.message = alliance_join_request_pb.message
  alliance_join_request_mdl.requestDate = ctrl.epochToDateTime(alliance_join_request_pb.time_requested)
  alliance_join_request_mdl.status = pb.AllianceJoinRequest.PENDING
  alliance_join_request_mdl.put()
  alliance_join_request_pb.key = str(alliance_join_request_mdl.key())

  ctrl.clearCached(["alliance-join-requests:%s" % (alliance_pb.key)])


def getJoinRequest(alliance_join_request_key):
  cache_key = "alliance-join-request:%s" % (alliance_join_request_key)
  values = ctrl.getCached([cache_key], pb.AllianceJoinRequest)
  if cache_key in values:
    return values[cache_key]

  alliance_join_request_mdl = mdl.AllianceJoinRequest.get(alliance_join_request_key)
  if not alliance_join_request_mdl:
    return None
  alliance_join_request_pb = pb.AllianceJoinRequest()
  ctrl.allianceJoinRequestModelToPb(alliance_join_request_pb, alliance_join_request_mdl)
  ctrl.setCached({cache_key: alliance_join_request_pb})
  return alliance_join_request_pb


def getPendingJoinRequestsForEmpire(empire_key):
  """Gets all PENDING join requests for the given empire."""
  cache_key = "alliance-join-requests-for-empire:%s" % empire_key
  values = ctrl.getCached([cache_key], pb.AllianceJoinRequests)
  if cache_key in values:
    return values[cache_key]

  query = (mdl.AllianceJoinRequest.all().filter("empire", db.Key(empire_key))
                                        .filter("status", pb.AllianceJoinRequest.PENDING)
                                        .order("-requestDate"))
  return _getJoinRequestsForQuery(cache_key, query)


def getJoinRequests(alliance_pb):
  """Fetches alliance join requests for the given alliance."""
  cache_key = "alliance-join-requests:%s" % (alliance_pb.key)
  values = ctrl.getCached([cache_key], pb.AllianceJoinRequests)
  if cache_key in values:
    return values[cache_key]

  query = (mdl.AllianceJoinRequest.all().ancestor(db.Key(alliance_pb.key))
                                        .order("-requestDate"))
  return _getJoinRequestsForQuery(cache_key, query)


def _getJoinRequestsForQuery(cache_key, query):
  alliance_join_requests_pb = pb.AllianceJoinRequests()
  for alliance_join_request_mdl in query:
    alliance_join_request_pb = alliance_join_requests_pb.join_requests.add()
    ctrl.allianceJoinRequestModelToPb(alliance_join_request_pb, alliance_join_request_mdl)

  ctrl.setCached({cache_key: alliance_join_requests_pb})
  return alliance_join_requests_pb


def updateJoinRequest(alliance_join_request_pb):
  alliance_join_request_mdl = mdl.AllianceJoinRequest.get(alliance_join_request_pb.key)
  # note: we largely trust the PBs at this point, security is taken care of in the ApiPages.

  if (alliance_join_request_mdl.status == pb.AllianceJoinRequest.PENDING and
      alliance_join_request_pb.state == pb.AllianceJoinRequest.ACCEPTED):
    # first, all other pending requests for this empire must become 'rejected'
    alliance_join_requests_pb = getPendingJoinRequestsForEmpire(alliance_join_request_pb.empire_key)
    for this_alliance_join_request_pb in alliance_join_requests_pb.join_requests:
      if this_alliance_join_request_pb.key == alliance_join_request_pb.key:
        continue
      this_alliance_join_request_pb.state = pb.AllianceJoinRequest.REJECTED
      updateJoinRequest(this_alliance_join_request_pb)

    # if we've gone from PENDING to ACCEPTED, we need to actually add them as a member now
    # TODO: transaction...
    alliance_mdl = mdl.Alliance.get(alliance_join_request_pb.alliance_key)
    alliance_mdl.numMembers += 1
    alliance_mdl.put()

    alliance_member_mdl = mdl.AllianceMember(parent=alliance_mdl)
    alliance_member_mdl.empire = db.Key(alliance_join_request_pb.empire_key)
    alliance_member_mdl.joinDate = datetime.now()
    alliance_member_mdl.put()

  alliance_join_request_mdl.status = alliance_join_request_pb.state
  alliance_join_request_mdl.message = alliance_join_request_pb.message
  alliance_join_request_mdl.put()

  empire_pb = empire_ctl.getEmpire(alliance_join_request_pb.empire_key)
  ctrl.clearCached(["alliance-join-requests:%s" % (alliance_join_request_pb.alliance_key),
                    "alliances:all",
                    "alliances:%s" % alliance_join_request_pb.alliance_key,
                    "empire:%s" % alliance_join_request_pb.empire_key,
                    "empire-for-user:%s" % empire_pb.email])


def leaveAlliance(alliance_leave_request_pb):
  alliance_mdl = mdl.Alliance.get(db.Key(alliance_leave_request_pb.alliance_key))
  if not alliance_mdl:
    return # todo: error?
  for alliance_member_mdl in (mdl.AllianceMember.all()
                .filter("empire", db.Key(alliance_leave_request_pb.empire_key))):
    alliance_member_mdl.delete()
    alliance_mdl.numMembers -= 1
  alliance_mdl.put()

  empire_pb = empire_ctl.getEmpire(alliance_leave_request_pb.empire_key)
  ctrl.clearCached(["alliances:all",
                    "alliances:%s" % alliance_leave_request_pb.alliance_key,
                    "empire:%s" % alliance_leave_request_pb.empire_key,
                    "empire-for-user:%s" % empire_pb.email])
