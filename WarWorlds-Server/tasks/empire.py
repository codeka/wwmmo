"""empire.py: Empire-related tasks."""

from datetime import datetime, timedelta
import logging
import os

from google.appengine.ext import db
from google.appengine.api import taskqueue

import import_fixer
import_fixer.FixImports("google", "protobuf")

import webapp2 as webapp

import ctrl
from ctrl import empire as ctl
from ctrl import simulation as simulation_ctl
from model import empire as mdl
from model import sector as sector_mdl
import protobufs.messages_pb2 as pb
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

    complete_time = datetime.now() + timedelta(seconds=10)
    never_time = datetime(2000, 1, 1)

    sim = simulation_ctl.Simulation()

    # Fetch the keys outside of the transaction, cause we can't do that in a TX
    build_request_models = []
    query = (mdl.BuildOperation.all().filter("endTime <", complete_time)
                                     .filter("endTime >", never_time))
    for oper in query:
      build_request_model = db.run_in_transaction(_fetchOperationInTX, oper.key())
      if build_request_model:
        star_key = str(build_request_model.key().parent())
        # simulate the star up to this point, to make sure that any resources and whatnot
        # removed before we complete the build.
        if not sim.getStar(star_key):
          sim.simulate(star_key)
        build_request_models.append(build_request_model)

    keys_to_clear = []
    for build_request_model in build_request_models:
      # OK, this build operation is complete (or will complete in the next 10 seconds -- close
      # enough) so we need to make sure the building/ship itself is added to the empire.
      colony_key = mdl.BuildOperation.colony.get_value_for_datastore(build_request_model)
      empire_key = mdl.BuildOperation.empire.get_value_for_datastore(build_request_model)

      # Figure out the name of the star the object was built on, for the notification
      star_key = build_request_model.key().parent()
      star_pb = sim.getStar(str(star_key))

      new_fleet_key = None

      logging.info("Build for empire \"%s\", colony \"%s\" complete." % (empire_key, colony_key))
      if build_request_model.designKind == pb.BuildRequest.BUILDING:
        # if it's an upgrade of an existing building, then just upgrade that building...
        existing_building_key = mdl.BuildOperation.existingBuilding.get_value_for_datastore(build_request_model)
        if existing_building_key:
          model = mdl.Building.get(existing_building_key)
          if not model:
            logging.error("Could not find building %s to upgrade!" % (str(existing_building_key)))
          else:
            # TODO: check the star, empire, colony and so on?
            if not model.level:
              model.level = 1
            model.level += 1
            model.put()
        else:
          model = mdl.Building(parent=build_request_model.key().parent())
          model.colony = colony_key
          model.empire = empire_key
          model.designName = build_request_model.designName
          model.buildTime = datetime.now()
          model.put()
      else:
        # if it's not a building, it must be a ship. We'll try to find a fleet that'll
        # work, but if we can't it's not a big deal -- just create a new one. Duplicates
        # don't hurt all that much
        existing = False;
        for fleet_pb in star_pb.fleets:
          if fleet_pb.design_name == build_request_model.designName:
            if (fleet_pb.state == pb.Fleet.IDLE and fleet_pb.empire_key and
                fleet_pb.empire_key == str(empire_key)):
              fleet_pb.num_ships += float(build_request_model.count)
              existing = True
        if not existing:
          model = mdl.Fleet(parent=star_key)
          model.empire = build_request_model.empire
          model.sector = sector_mdl.SectorManager.getSectorKey(star_pb.sector_x, star_pb.sector_y)
          model.designName = build_request_model.designName
          model.numShips = float(build_request_model.count)
          model.state = pb.Fleet.IDLE
          model.stance = pb.Fleet.AGGRESSIVE
          model.stateStartTime = datetime.now()
          model.put()
          new_fleet_key = str(model.key())

        # if you've built a colony ship, we need to decrease the colony population by
        # 100 (basically, those 100 people go into the colony ship, to be transported to
        # the destination colony).
        if build_request_model.designName == "colonyship": # TODO: hard-coded OK?P
          for colony_pb in star_pb.colonies:
            if colony_pb.key == colony_key:
              colony_pb.population -= 100

      sim.update()
      sitrep_pb = pb.SituationReport()

      if new_fleet_key:
        # if new fleets have been added, we'll need re-simulate
        # the star, and call onFleetArrived so that it can start
        # attacking any enemies present
        sim = simulation_ctl.Simulation()
        sim.onFleetArrived(new_fleet_key, str(star_key))
        sim.simulate(str(star_key))
        sim.update()
        combat_report_pb = sim.getCombatReport(str(star_key))
        if combat_report_pb and combat_report_pb.rounds:
          # if there's a combat report it means we also entered a battle (possibly), so we might
          # have to update the sit.rep. to cover that. We might also have to generate a sit.rep.
          # for any other empires that are already here...
          round_1 = combat_report_pb.rounds[0]
          for combat_fleet_pb in round_1.fleets:
            if combat_fleet_pb.fleet_key == new_fleet_key:
              sitrep_pb.fleet_under_attack_record.fleet_key = new_fleet_key
              sitrep_pb.fleet_under_attack_record.fleet_design_id = combat_fleet_pb.design_id
              sitrep_pb.fleet_under_attack_record.num_ships = combat_fleet_pb.num_ships
              sitrep_pb.fleet_under_attack_record.combat_report_key = combat_report_pb.key

      # Save a sitrep for this situation
      sitrep_pb.empire_key = str(empire_key)
      sitrep_pb.report_time = ctrl.dateTimeToEpoch(sim.now)
      sitrep_pb.star_key = str(star_key)
      sitrep_pb.planet_index = build_request_model.colony.planet_index
      sitrep_pb.build_complete_record.build_kind = build_request_model.designKind
      sitrep_pb.build_complete_record.design_id = build_request_model.designName
      sitrep_pb.build_complete_record.count = build_request_model.count

      ctl.saveSituationReport(sitrep_pb)

      # clear the cached items that reference this building/fleet
      keys_to_clear.append("star:%s" % (star_pb.key))
      keys_to_clear.append("fleet:for-empire:%s" % (empire_key))
      keys_to_clear.append("colonies:for-empire:%s" % (empire_key))

    ctrl.clearCached(keys_to_clear)

    force_reschedule = False
    if self.request.get("auto") != "1":
      force_reschedule = True
    ctl.scheduleBuildCheck(force_reschedule=force_reschedule)

    self.response.write("Success!")


class StarSimulatePage(tasks.TaskPage):
  """Simulates all stars that have not been simulated for more than 18 hours.

  This is a scheduled task that runs every 6 hours. It looks for all stars that have not been
  simulated in more than 18 hours and simulates them now. By running every 6 hours, we ensure
  no star is more than 24 hours out of date."""
  def get(self):
    # find all stars where last_simulation is at least 48 hours ago
    last_simulation = datetime.now() - timedelta(hours=48)

    MAX_STARS = 10

    star_keys = []
    for star_mdl in sector_mdl.Star.all().filter("lastSimulation <", last_simulation).fetch(MAX_STARS + 1):
      star_key = str(star_mdl.key())
      if star_key not in star_keys:
        star_keys.append(star_key)
    for colony_mdl in mdl.Colony.all().filter("lastSimulation <", last_simulation).fetch(MAX_STARS + 1):
      star_key = str(colony_mdl.key().parent())
      if star_key not in star_keys:
        star_keys.append(star_key)

    logging.debug("%d stars to simulate" % (len(star_keys)))
    sim = simulation_ctl.Simulation()
    n = 0
    finished_all = True
    for star_key in star_keys:
      star_pb = sim.getStar(star_key, True)
      if star_pb.last_simulation > ctrl.dateTimeToEpoch(last_simulation):
        continue
      sim.simulate(star_key)
      if n > MAX_STARS:
        finished_all = False
        break
      n += 1
    sim.update()

    if not finished_all:
      logging.debug("Not all stars finished, queueing up another one.")
      taskqueue.add(queue_name="sectors",
                    url="/tasks/empire/star-simulate",
                    method="GET")

    self.response.write("Success!")


class FleetMoveCompletePage(tasks.TaskPage):
  """Called when a fleet completes it's move operation.

  We need to transfer the fleet so that it's orbiting the new star."""
  def get(self, fleet_key):
    fleet_mdl = mdl.Fleet.get(fleet_key)
    if not fleet_mdl or fleet_mdl.state != pb.Fleet.MOVING:
      logging.warn("Fleet [%s] is not moving, as expected." % (fleet_key))
      self.response.set_status(400)
    else:
      new_star_key = str(mdl.Fleet.destinationStar.get_value_for_datastore(fleet_mdl))
      sim = simulation_ctl.Simulation()
      sim.simulate(new_star_key)
      new_star_pb = sim.getStar(new_star_key)

      if str(fleet_mdl.key().parent()) == new_star_key:
        old_star_pb = new_star_pb
      else:
        old_star_pb = sim.getStar(fleet_mdl.key().parent(), True)

      empire_key = mdl.Fleet.empire.get_value_for_datastore(fleet_mdl)

      new_fleet_mdl = mdl.Fleet(parent=fleet_mdl.destinationStar)
      new_fleet_mdl.sector = sector_mdl.SectorManager.getSectorKey(new_star_pb.sector_x,
                                                                   new_star_pb.sector_y)
      new_fleet_mdl.empire = empire_key
      new_fleet_mdl.designName = fleet_mdl.designName
      new_fleet_mdl.numShips = fleet_mdl.numShips
      new_fleet_mdl.stance = fleet_mdl.stance

      # new fleet is now idle
      new_fleet_mdl.state = pb.Fleet.IDLE
      new_fleet_mdl.stateStartTime = datetime.now() - timedelta(seconds=1)

      empire = fleet_mdl.empire
      fleet_mdl.delete()
      new_fleet_mdl.put()

      ctrl.clearCached(["fleet:for-empire:%s" % (empire.key()),
                        "star:%s" % (old_star_pb.key),
                        "star:%s" % (new_star_key),
                        "sector:%d,%d" % (new_star_pb.sector_x, new_star_pb.sector_y),
                        "sector:%d,%d" % (old_star_pb.sector_x, old_star_pb.sector_y)])

      # manually add the fleet to the new star's PB
      new_fleet_pb = new_star_pb.fleets.add()
      ctrl.fleetModelToPb(new_fleet_pb, new_fleet_mdl)
      new_fleet_key = new_fleet_pb.key

      # manually remove the fleet from the old star's PB
      for n,fleet_pb in enumerate(old_star_pb.fleets):
        if fleet_pb.key == fleet_key:
          del old_star_pb.fleets[n]
          break

      sim.onFleetArrived(new_fleet_key, new_star_pb.key)
      sim.simulate(new_star_pb.key)
      if new_star_pb.key != old_star_pb.key:
        sim.simulate(old_star_pb.key)
      sim.update()

      # Save a sitrep for this situation
      sitrep_pb = pb.SituationReport()
      sitrep_pb.empire_key = str(empire_key)
      sitrep_pb.report_time = ctrl.dateTimeToEpoch(sim.now)
      sitrep_pb.star_key = new_star_pb.key
      sitrep_pb.planet_index = -1
      sitrep_pb.move_complete_record.fleet_key = new_fleet_pb.key
      sitrep_pb.move_complete_record.fleet_design_id = new_fleet_pb.design_name
      sitrep_pb.move_complete_record.num_ships = new_fleet_pb.num_ships
      for scout_report_pb in sim.scout_report_pbs:
        sitrep_pb.move_complete_record.scout_report_key = scout_report_pb.key

      combat_report_pb = sim.getCombatReport(new_star_pb.key)
      if combat_report_pb and combat_report_pb.rounds:
        # if there's a combat report it means we also entered a battle (possibly), so we might
        # have to update the sit.rep. to cover that. We might also have to generate a sit.rep.
        # for any other empires that are already here...
        round_1 = combat_report_pb.rounds[0]
        for combat_fleet_pb in round_1.fleets:
          if combat_fleet_pb.fleet_key == new_fleet_pb.key:
            sitrep_pb.fleet_under_attack_record.fleet_key = new_fleet_pb.key
            sitrep_pb.fleet_under_attack_record.fleet_design_id = new_fleet_pb.design_name
            sitrep_pb.fleet_under_attack_record.num_ships = new_fleet_pb.num_ships
            sitrep_pb.fleet_under_attack_record.combat_report_key = combat_report_pb.key

      ctl.saveSituationReport(sitrep_pb)


app = webapp.WSGIApplication([("/tasks/empire/build-check", BuildCheckPage),
                              ("/tasks/empire/star-simulate", StarSimulatePage),
                              ("/tasks/empire/fleet/([^/]+)/move-complete", FleetMoveCompletePage)],
                             debug=os.environ["SERVER_SOFTWARE"].startswith("Development"))

