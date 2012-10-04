"""empire.py: Empire-related tasks."""

from datetime import datetime, timedelta
import logging
import os

from google.appengine.ext import db

import import_fixer
import_fixer.FixImports("google", "protobuf")

import webapp2 as webapp

import ctrl
from ctrl import empire as ctl
from ctrl import sector as sector_ctl
from model import empire as mdl
from model import sector as sector_mdl
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

    def _incrShipCountInTX(fleet_key, n):
      fleet_model = mdl.Fleet.get(fleet_key)
      if fleet_model.state == pb.Fleet.IDLE:
        fleet_model.numShips += n
        fleet_model.put()
        return True
      return False

    complete_time = datetime.now() + timedelta(seconds=10)
    never_time = datetime(2000, 1, 1)

    # Fetch the keys outside of the transaction, cause we can't do that in a TX
    build_request_models = []
    query = (mdl.BuildOperation.all().filter("endTime <", complete_time)
                                     .filter("endTime >", never_time))
    for oper in query:
      build_request_model = db.run_in_transaction(_fetchOperationInTX, oper.key())
      if build_request_model:
        build_request_models.append(build_request_model)

    keys_to_clear = []
    for build_request_model in build_request_models:
      # OK, this build operation is complete (or will complete in the next 10 seconds -- close
      # enough) so we need to make sure the building/ship itself is added to the empire.
      colony_key = mdl.BuildOperation.colony.get_value_for_datastore(build_request_model)
      empire_key = mdl.BuildOperation.empire.get_value_for_datastore(build_request_model)

      # Figure out the name of the star the object was built on, for the notification
      star_key = build_request_model.key().parent()
      star_pb = sector_ctl.getStar(star_key)

      logging.info("Build for empire \"%s\", colony \"%s\" complete." % (empire_key, colony_key))
      if build_request_model.designKind == pb.BuildRequest.BUILDING:
        model = mdl.Building(parent=build_request_model.key().parent())
        model.colony = colony_key
        model.empire = empire_key
        model.designName = build_request_model.designName
        model.buildTime = datetime.now()
        model.put()
        design = ctl.BuildingDesign.getDesign(model.designName)
      else:
        # if it's not a building, it must be a ship. We'll try to find a fleet that'll
        # work, but if we can't it's not a big deal -- just create a new one. Duplicates
        # don't hurt all that much (TODO: confirm)
        query = (mdl.Fleet.all().ancestor(build_request_model.key().parent())
                                .filter("empire", build_request_model.empire))
        existing = False
        for fleet_model in query:
          if (fleet_model.designName == build_request_model.designName and
              fleet_model.state == pb.Fleet.IDLE):
            if db.run_in_transaction(_incrShipCountInTX,
                                     fleet_model.key(),
                                     build_request_model.count):
              existing = True
              model = fleet_model
              # it's an existing fleet, so make sure we clear it's cached value
              keys_to_clear.append("fleet:%s" % str(fleet_model.key()))
              break
        if not existing:
          model = mdl.Fleet(parent=star_key)
          model.empire = build_request_model.empire
          model.sector = sector_mdl.SectorManager.getSectorKey(star_pb.sector_x, star_pb.sector_y)
          model.designName = build_request_model.designName
          model.numShips = build_request_model.count
          model.state = pb.Fleet.IDLE
          model.stateStartTime = datetime.now()
          model.put()
        design = ctl.ShipDesign.getDesign(model.designName)

        # if you've built a colony ship, we need to decrease the colony population by
        # 100 (basically, those 100 people go into the colony ship, to be transported to
        # the destination colony).
        if build_request_model.designName == "colonyship": # TODO: hard-coded OK?
          star_pb = sector_ctl.getStar(star_key)
          ctl.simulate(star_pb, empire_key)
          for colony_pb in star_pb.colonies:
            if colony_pb.key == colony_key:
              colony_pb.population -= 100
          ctl.updateAfterSimulate(star_pb, empire_key)

      # Send a notification to the player that construction of their building is complete
      msg = "Your %d %s(s) on %s has been built." % (build_request_model.count,
                                                     design.name,
                                                     star_pb.name)
      logging.debug("Sending message to user [%s] indicating build complete." % (
          model.empire.user.email()))
      s = c2dm.Sender()
      devices = ctrl.getDevicesForUser(model.empire.user.email())
      for device in devices.registrations:
        s.sendMessage(device.device_registration_id, {"msg": msg})

      # clear the cached items that reference this building/fleet
      keys_to_clear.append("star:%s" % (star_pb.key))
      keys_to_clear.append("fleet:for-empire:%s" % (empire_key))
      keys_to_clear.append("colonies:for-empire:%s" % (empire_key))

    ctrl.clearCached(keys_to_clear)
    ctl.scheduleBuildCheck()

    self.response.write("Success!")


class StarSimulatePage(tasks.TaskPage):
  """Simulates all stars that have not been simulated for more than 18 hours.

  This is a scheduled task that runs every 6 hours. It looks for all stars that have not been
  simulated in more than 18 hours and simulates them now. By running every 6 hours, we ensure
  no star is more than 24 hours out of date."""
  def get(self):
    # find all colonies where last_simulation is at least 18 hours ago
    last_simulation = datetime.now() - timedelta(hours=18)

    star_keys = []
    for colony_mdl in mdl.Colony.all().filter("lastSimulation <", last_simulation):
      star_key = str(colony_mdl.key().parent())
      if star_key not in star_keys:
        star_keys.append(star_key)

    logging.debug("%d stars to simulate" % (len(star_keys)))
    for star_key in star_keys:
      star_pb = sector_ctl.getStar(star_key)
      ctl.simulate(star_pb)
      ctl.updateAfterSimulate(star_pb)

    self.response.write("Success!")


class FleetMoveCompletePage(tasks.TaskPage):
  """Called when a fleet completes it's move operation.

  We need to transfer the fleet so that it's orbiting the new star."""
  def get(self, fleet_key):
    fleet_mdl = mdl.Fleet.get(fleet_key)
    if fleet_mdl.state != pb.Fleet.MOVING:
      logging.warn("Fleet [%s] is not moving, as expected." % (fleet_key))
      self.set_response(400)
    else:
      new_star_key = mdl.Fleet.destinationStar.get_value_for_datastore(fleet_mdl)
      new_star_pb = sector_ctl.getStar(new_star_key)

      if str(fleet_mdl.key().parent()) == new_star_key:
        old_star_pb = new_star_pb
      else:
        old_star_pb = sector_ctl.getStar(fleet_mdl.key().parent())

      new_fleet_mdl = mdl.Fleet(parent=fleet_mdl.destinationStar)
      new_fleet_mdl.sector = sector_mdl.SectorManager.getSectorKey(new_star_pb.sector_x,
                                                                   new_star_pb.sector_y)
      new_fleet_mdl.empire = mdl.Fleet.empire.get_value_for_datastore(fleet_mdl)
      new_fleet_mdl.designName = fleet_mdl.designName
      new_fleet_mdl.numShips = fleet_mdl.numShips
      new_fleet_mdl.stance = fleet_mdl.stance

      # new fleet is now idle
      new_fleet_mdl.state = pb.Fleet.IDLE
      new_fleet_mdl.stateStartTime = datetime.now()

      fleet_mdl.delete()
      new_fleet_mdl.put()

      new_fleet_pb = pb.Fleet()
      ctrl.fleetModelToPb(new_fleet_pb, new_fleet_mdl)

      empire = fleet_mdl.empire
      ctrl.clearCached(["fleet:for-empire:%s" % (empire.key()),
                        "star:%s" % (fleet_mdl.key().parent()),
                        "star:%s" % (new_star_key),
                        "sector:%d,%d" % (new_star_pb.sector_x, new_star_pb.sector_y),
                        "sector:%d,%d" % (old_star_pb.sector_x, old_star_pb.sector_y)])

      design = ctl.ShipDesign.getDesign(fleet_mdl.designName)

      # Send a notification to the player that construction of their building is complete
      msg = "Your %s fleet of %d ships has arrived on %s." % (design.name,
                                                              fleet_mdl.numShips,
                                                              new_star_pb.name)
      logging.debug("Sending message to user [%s] indicating fleet move complete." % (
          empire.user.email()))
      s = c2dm.Sender()
      devices = ctrl.getDevicesForUser(empire.user.email())
      for device in devices.registrations:
        s.sendMessage(device.device_registration_id, {"msg": msg})

      # apply any "star landed" effects
      for effect in design.getEffects():
        effect.onStarLanded(new_fleet_pb, new_star_pb)

      # if there's any ships already there, apply their onFleetArrived effects
      for fleet_pb in new_star_pb.fleets:
        design = ctl.ShipDesign.getDesign(fleet_pb.design_name)
        for effect in design.getEffects():
          effect.onFleetArrived(fleet_pb, new_star_pb, new_fleet_pb)


class FleetDestroyedPage(tasks.TaskPage):
  def get(self, fleet_key):
    fleet_mdl = mdl.Fleet.get(fleet_key)
    if fleet_mdl and fleet_mdl.timeDestroyed < (datetime.now() + timedelta(seconds=5)):
      empire_mdl = fleet_mdl.empire
      star_pb = sector_ctl.getStar(str(fleet_mdl.key().parent()))
      ctl.simulate(star_pb)
      ctl.updateAfterSimulate(star_pb)
      keys_to_clear = ["fleet:for-empire:%s" % (empire_mdl.key()),
                       "star:%s" % (star_pb.key),
                       "sector:%d,%d" % (star_pb.sector_x, star_pb.sector_y)]
      fleet_mdl.delete()
      ctrl.clearCached(keys_to_clear)


app = webapp.WSGIApplication([("/tasks/empire/build-check", BuildCheckPage),
                              ("/tasks/empire/star-simulate", StarSimulatePage),
                              ("/tasks/empire/fleet/([^/]+)/move-complete", FleetMoveCompletePage),
                              ("/tasks/empire/fleet/([^/]+)/destroy", FleetDestroyedPage)],
                             debug=os.environ["SERVER_SOFTWARE"].startswith("Development"))

