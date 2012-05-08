'''
Created on 14/04/2012

@author: dean@codeka.com.au
'''

import webapp2 as webapp
import tasks
from model import empire as mdl
from model import c2dm
from datetime import datetime, timedelta
import ctrl
from ctrl import empire as ctl
from google.appengine.ext import db
import logging
import protobufs.warworlds_pb2 as pb
import os


class BuildCheckPage(tasks.TaskPage):
  def get(self):
    '''This page is called when a build operation is scheduled to finish.

    We need to confirm that the build is actually complete, set up the building or ship with the
    empire that built it, and then reschedule ourselves for the next build.
    '''

    def _fetchOperationInTX(oper_key):
      '''This is done in a transaction to make sure only one request processes the build.'''
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

    # Fetch the keys outside of the transaction, cause we can't do that in a TX
    operations = []
    query = mdl.BuildOperation.all().filter("endTime <", datetime.now() + timedelta(seconds=10))
    for oper in query:
      operation = db.run_in_transaction(_fetchOperationInTX, oper.key())
      if operation:
        operations.append(operation)

    keys_to_clear = []
    for oper_model in operations:

      # OK, this build operation is complete (or will complete in the next 10 seconds -- close
      # enough) so we need to make sure the building/ship itself is added to the empire.
      colony_key = mdl.BuildOperation.colony.get_value_for_datastore(oper_model)
      empire_key = mdl.BuildOperation.empire.get_value_for_datastore(oper_model)
      star_key = mdl.BuildOperation.star.get_value_for_datastore(oper_model)
      logging.info("Build for empire \"%s\" complete." % (empire_key))
      if oper_model.designKind == pb.BuildRequest.BUILDING:
        model = mdl.Building()
        model.colony = colony_key
        model.empire = empire_key
        model.star = star_key
        model.designName = oper_model.designName
        model.buildTime = datetime.now()
        model.put()
        design = ctl.BuildingDesign.getDesign(model.designName)
      else:
        # if it's not a building, it must be a ship. We'll try to find a fleet that'll
        # work, but if we can't it's not a big deal -- just create a new one. Duplicates
        # don't hurt all that much (TODO: confirm)
        query = mdl.Fleet.all().filter("star", star_key).filter("empire", empire_key)
        done = False
        for fleet_model in query:
          if (fleet_model.designName == oper_model.designName and
              fleet_model.state == pb.Fleet.IDLE):
            if db.run_in_transaction(_incrShipCountInTX, fleet_model.key()):
              done = True
              model = fleet_model
              break
        if not done:
          model = mdl.Fleet()
          model.empire = empire_key
          model.star = star_key
          model.designName = oper_model.designName
          model.numShips = 1
          model.state = pb.Fleet.IDLE
          model.stateStartTime = datetime.now()
          model.put()
        design = ctl.ShipDesign.getDesign(model.designName)

      # Send a notification to the player that construction of their building is complete
      msg = 'Your %s has been built.' % (design.name)
      logging.debug('Sending message to user [%s] indicating build complete.' % (
          model.empire.user.email()))
      s = c2dm.Sender()
      devices = ctrl.getDevicesForUser(model.empire.user.email())
      for device in devices.registrations:
        s.sendMessage(device.device_registration_id, {"msg": msg})
      return None

      # clear the cached items that reference this building/fleet
      keys_to_clear.append('star:%s' % (star_key))
      keys_to_clear.append('colonies:for-empire:%s' % (empire_key))

    ctrl.clearCached(keys_to_clear)
    ctl.scheduleBuildCheck()

    self.response.write("Success!")

app = webapp.WSGIApplication([('/tasks/empire/build-check', BuildCheckPage)],
                             debug=os.environ['SERVER_SOFTWARE'].startswith('Development'))

