"""empire.py: Empire-related tasks."""

from datetime import datetime, timedelta
import logging
import os

from google.appengine.ext import db

import webapp2 as webapp

import ctrl
from ctrl import empire as ctl
from ctrl import sector as sector_ctl
from model import empire as mdl
from model import c2dm
import protobufs.warworlds_pb2 as pb
import tasks


class BuildCheckPage(tasks.TaskPage):
  def get(self):
    """This page is called when a build operation is scheduled to finish.

    We need to confirm that the build is actually complete, set up the building or ship with the
    empire that built it, and then reschedule ourselves for the next build.
    """

    def _fetchOperationInTX(oper_key):
      """This is done in a transaction to make sure only one request processes the build."""

      oper_model = mdl.BuildOperation.get(oper_key)
      if not oper_model:
        return None

      # and now we're done with this operation
      oper_model.delete()
      return oper_model

    def _incrShipCountInTX(fleet_key):
      fleet_model = mdl.Fleet.get(fleet_key)
      if fleet_model.state == pb.Fleet.IDLE:
        fleet_model.numShips += 1
        fleet_model.put()
        return True
      return False

    complete_time = datetime.now() + timedelta(seconds=10)
    never_time = datetime(2000, 1, 1)

    # Fetch the keys outside of the transaction, cause we can't do that in a TX
    operations = []
    query = (mdl.BuildOperation.all().filter("endTime <", complete_time)
                                     .filter("endTime >", never_time))
    for oper in query:
      operation = db.run_in_transaction(_fetchOperationInTX, oper.key())
      if operation:
        operations.append(operation)

    star_pbs = []
    build_request_pbs = []
    # simulate the stars these operations are inside of, to make sure the build time
    # is still correct
    for oper_model in operations:
      star_key = mdl.BuildOperation.star.get_value_for_datastore(oper_model)
      already_done = False
      for star_pb in star_pbs:
        if star_pb.key == star_key:
          already_done = True
          continue
      if already_done:
        continue

      star_pb = sector_ctl.getStar(star_key, True) # force_nocache == True
      ctl.simulate(star_pb)
      ctl.updateAfterSimulate(star_pb)

      # any build requests that are still scheduled to be completed by now
      for build_request in star_pb.build_requests:
        if (ctrl.epochToDateTime(build_request.end_time) < complete_time and
            ctrl.epochToDateTime(build_request.end_time) > never_time):
          build_request_pbs.append(build_request)

      # remember that we've simulated this star so we don't do it again
      star_pbs.append(star_pb)

    keys_to_clear = []
    for build_request_pb in build_request_pbs:
      # OK, this build operation is complete (or will complete in the next 10 seconds -- close
      # enough) so we need to make sure the building/ship itself is added to the empire.
      logging.info("Build for empire \"%s\" complete." % (build_request_pb.colony_key))
      if build_request_pb.build_kind == pb.BuildRequest.BUILDING:
        model = mdl.Building()
        model.colony = build_request_pb.colony_key
        model.empire = build_request_pb.empire_key
        model.star = star_key
        model.designName = build_request_pb.design_name
        model.buildTime = datetime.now()
        model.put()
        design = ctl.BuildingDesign.getDesign(model.designName)
      else:
        # if it's not a building, it must be a ship. We'll try to find a fleet that'll
        # work, but if we can't it's not a big deal -- just create a new one. Duplicates
        # don't hurt all that much (TODO: confirm)
        query = mdl.Fleet.all().filter("star", star_key).filter("empire",
                                                                build_request_pb.empire_key)
        done = False
        for fleet_model in query:
          if (fleet_model.designName == build_request_pb.design_name and
              fleet_model.state == pb.Fleet.IDLE):
            if db.run_in_transaction(_incrShipCountInTX, fleet_model.key()):
              done = True
              model = fleet_model
              # it's an existing fleet, so make sure we clear it's cached value
              keys_to_clear.append("fleet:%s" % str(fleet_model.key()))
              break
        if not done:
          model = mdl.Fleet()
          model.empire = build_request_pb.empire_key
          model.star = star_key
          model.designName = build_request_pb.design_name
          model.numShips = 1
          model.state = pb.Fleet.IDLE
          model.stateStartTime = datetime.now()
          model.put()
        design = ctl.ShipDesign.getDesign(model.designName)

      # Figure out the name of the star the object was built on, for the notification
      star_pb = None
      for spb in star_pbs:
        if spb.key == star_key:
          star_pb = spb
          break

      # Send a notification to the player that construction of their building is complete
      msg = "Your %s on %s has been built." % (design.name, star_pb.name)
      logging.debug("Sending message to user [%s] indicating build complete." % (
          model.empire.user.email()))
      s = c2dm.Sender()
      devices = ctrl.getDevicesForUser(model.empire.user.email())
      for device in devices.registrations:
        s.sendMessage(device.device_registration_id, {"msg": msg})

      # clear the cached items that reference this building/fleet
      keys_to_clear.append("star:%s" % (star_key))
      keys_to_clear.append("fleet:for-empire:%s" % (build_request_pb.empire_key))
      keys_to_clear.append("colonies:for-empire:%s" % (build_request_pb.empire_key))

    ctrl.clearCached(keys_to_clear)
    ctl.scheduleBuildCheck()

    self.response.write("Success!")

app = webapp.WSGIApplication([("/tasks/empire/build-check", BuildCheckPage)],
                             debug=os.environ["SERVER_SOFTWARE"].startswith("Development"))

