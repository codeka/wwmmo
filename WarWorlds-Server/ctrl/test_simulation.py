"""simulation_tse.py: Unit tests for simulation.py."""

from datetime import datetime, timedelta
import unittest

import ctrl
from ctrl import simulation

import import_fixer
import_fixer.FixImports("google", "protobuf")

from protobufs import messages_pb2 as pb
from protobufs import protobuf_json


class MockStarFetcher:
  def __init__(self, stars_json=None):
    self.stars = {}
    if stars_json:
      for star_json in stars_json:
        star_pb = protobuf_json.json2pb(pb.Star(), star_json)
        self.stars[star_pb.key] = star_pb

  def __call__(self, star_key):
    return self.stars[star_key]


class MockCombatReportFetcher:
  def __init__(self, combat_reports_json=None):
    self.combat_reports = {}
    
  def __call__(self, star_key, now):
    return None


def debug_log(msg):
  print msg


def _create_simulation(star_json=None, combat_report_json=None):
  star_fetcher = MockStarFetcher(star_json)
  combat_report_fetcher = MockCombatReportFetcher(combat_report_json)
  return simulation.Simulation(star_fetcher=star_fetcher,
                               combat_report_fetcher=combat_report_fetcher)


class EmpirePresenceTestCase(unittest.TestCase):
  def testEmpirePresenceCreated(self):
    sim = _create_simulation(star_json = ["""
        {"key": "star1",
         "planets": [
           {"index": 0, "planet_type": 9} 
         ],
         "colonies": [
           {"key": "colony1", "empire_key": "empire1", "star_key": "star1", "planet_index": 0,
            "population": 100, "focus_population": 0.25,
            "focus_farming": 0.25, "focus_mining": 0.25, "focus_construction": 0.25}
         ],
         "buildings": [],
         "empires": [],
         "build_requests": [],
         "fleets": [],
         "last_simulation": %d
        }
      """ % (ctrl.dateTimeToEpoch(datetime.now() - timedelta(minutes=1)))])
    sim.simulate("star1")

    star_pb = sim.getStar("star1")
    self.assertIsNotNone(star_pb)
    self.assertEqual(1, len(star_pb.empires))
    self.assertEqual("empire1", star_pb.empires[0].empire_key)


class CombatTestCase(unittest.TestCase):
  def testOneFleetVsPassiveFleet(self):
    sim = _create_simulation(star_json = ["""
        {"key": "star1",
         "planets": [
           {"index": 0, "planet_type": 9} 
         ],
         "colonies": [
           {"key": "colony1", "empire_key": "empire1", "star_key": "star1", "planet_index": 0,
            "population": 100, "focus_population": 0.25,
            "focus_farming": 0.25, "focus_mining": 0.25, "focus_construction": 0.25}
         ],
         "buildings": [],
         "empires": [],
         "build_requests": [],
         "fleets": [
           {"key": "fleet1", "empire_key": "empire1", "design_name": "fighter", "num_ships": 10,
            "state": 3, "state_start_time": %d, "star_key": "star1", "target_fleet_key": "fleet2",
            "stance": 3},
           {"key": "fleet2", "empire_key": "empire2", "design_name": "fighter", "num_ships": 10,
            "state": 1, "state_start_time": 0, "star_key": "star1", "stance": 1}
         ],
         "last_simulation": %d
        }
      """ % (ctrl.dateTimeToEpoch(datetime.now() - timedelta(minutes=2)),
             ctrl.dateTimeToEpoch(datetime.now() - timedelta(minutes=3)))])
    sim.simulate("star1")

    star_pb = sim.getStar("star1")
    self.assertEqual(2, len(star_pb.fleets))
    self.assertEqual("fleet1", star_pb.fleets[0].key)
    self.assertEqual(10, star_pb.fleets[0].num_ships)
    self.assertEqual(0, star_pb.fleets[0].time_destroyed)
    self.assertEqual("fleet2", star_pb.fleets[1].key)
    self.assertEqual(0, star_pb.fleets[1].num_ships)
    self.assertNotEqual(0, star_pb.fleets[1].time_destroyed)
    # check that the combat report is correctly populated
    combat_report = sim.getCombatReport("star1")
    self.assertEqual(3, len(combat_report.rounds))
    self.assertEqual(2, len(combat_report.rounds[0].fleets))
    self.assertEqual(1, len(combat_report.rounds[0].fleets_attacked))
    self.assertEqual("fleet1", combat_report.rounds[0].fleets[combat_report.rounds[0].fleets_attacked[0].fleet_index].fleet_key)
    self.assertEqual("fleet2", combat_report.rounds[0].fleets[combat_report.rounds[0].fleets_attacked[0].target_index].fleet_key)
    self.assertEqual(50.0, combat_report.rounds[0].fleets_attacked[0].damage)
    self.assertEqual(1, len(combat_report.rounds[0].fleets_damaged))
    self.assertEqual("fleet2", combat_report.rounds[0].fleets[combat_report.rounds[0].fleets_damaged[0].fleet_index].fleet_key)
    self.assertAlmostEqual(3.33, combat_report.rounds[0].fleets_damaged[0].damage, 2)


  def testFleetOf10vsFleetOf20(self):
    sim = _create_simulation(star_json = ["""
        {"key": "star1",
         "planets": [
           {"index": 0, "planet_type": 9} 
         ],
         "colonies": [
           {"key": "colony1", "empire_key": "empire1", "star_key": "star1", "planet_index": 0,
            "population": 100, "focus_population": 0.25,
            "focus_farming": 0.25, "focus_mining": 0.25, "focus_construction": 0.25}
         ],
         "buildings": [],
         "empires": [],
         "build_requests": [],
         "fleets": [
           {"key": "fleet1", "empire_key": "empire1", "design_name": "fighter", "num_ships": 10,
            "state": 3, "state_start_time": %d, "star_key": "star1", "target_fleet_key": "",
            "stance": 3},
           {"key": "fleet2", "empire_key": "empire2", "design_name": "fighter", "num_ships": 20,
            "state": 3, "state_start_time": %d, "star_key": "star1", "target_fleet_key": "",
            "stance": 3}
         ],
         "last_simulation": %d
        }
      """ % (ctrl.dateTimeToEpoch(datetime.now() - timedelta(minutes=20)),
             ctrl.dateTimeToEpoch(datetime.now() - timedelta(minutes=15)),
             ctrl.dateTimeToEpoch(datetime.now() - timedelta(minutes=15)))])
    sim.simulate("star1")

    star_pb = sim.getStar("star1")
    self.assertEqual(2, len(star_pb.fleets))
    self.assertEqual("fleet1", star_pb.fleets[0].key)
    self.assertEqual(0, star_pb.fleets[0].num_ships)
    self.assertNotEqual(0, star_pb.fleets[0].time_destroyed)
    self.assertEqual("fleet2", star_pb.fleets[1].key)
    self.assertEqual(15, int(star_pb.fleets[1].num_ships))
    self.assertEqual(0, star_pb.fleets[1].time_destroyed)
    # check that the combat report is correctly populated
    combat_report = sim.getCombatReport("star1")
    self.assertEqual(2, len(combat_report.rounds))
    self.assertEqual(2, len(combat_report.rounds[0].fleets))
    self.assertEqual(2, len(combat_report.rounds[0].fleets_joined))
    self.assertEqual(2, len(combat_report.rounds[0].fleets_targetted))
    self.assertEqual(2, len(combat_report.rounds[0].fleets_attacked))
    self.assertEqual(2, len(combat_report.rounds[0].fleets_damaged))
    self.assertEqual(2, len(combat_report.rounds[1].fleets))
    self.assertEqual(0, len(combat_report.rounds[1].fleets_joined))
    self.assertEqual(0, len(combat_report.rounds[1].fleets_targetted))
    self.assertEqual(2, len(combat_report.rounds[1].fleets_attacked))
    self.assertEqual(2, len(combat_report.rounds[1].fleets_damaged))


  def testFleetOf20vsFleetOf10(self):
    sim = _create_simulation(star_json = ["""
        {"key": "star1",
         "planets": [
           {"index": 0, "planet_type": 9} 
         ],
         "colonies": [
           {"key": "colony1", "empire_key": "empire1", "star_key": "star1", "planet_index": 0,
            "population": 100, "focus_population": 0.25,
            "focus_farming": 0.25, "focus_mining": 0.25, "focus_construction": 0.25}
         ],
         "buildings": [],
         "empires": [],
         "build_requests": [],
         "last_simulation": %d,
         "fleets": [
           {"key": "fleet1", "empire_key": "empire1", "design_name": "fighter", "num_ships": 20,
            "state": 3, "state_start_time": %d, "star_key": "star1", "target_fleet_key": "fleet2",
            "stance": 3},
           {"key": "fleet2", "empire_key": "empire2", "design_name": "fighter", "num_ships": 10,
            "state": 3, "state_start_time": %d, "star_key": "star1", "target_fleet_key": "fleet1",
            "stance": 3}
         ]
        }
      """ % (ctrl.dateTimeToEpoch(datetime.now() - timedelta(minutes=20)),
             ctrl.dateTimeToEpoch(datetime.now() - timedelta(minutes=15)),
             ctrl.dateTimeToEpoch(datetime.now() - timedelta(minutes=15)))])
    sim.simulate("star1")

    star_pb = sim.getStar("star1")
    self.assertEqual(2, len(star_pb.fleets))
    self.assertEqual("fleet1", star_pb.fleets[0].key)
    self.assertEqual(15, int(star_pb.fleets[0].num_ships))
    self.assertEqual(0, star_pb.fleets[0].time_destroyed)
    self.assertEqual("fleet2", star_pb.fleets[1].key)
    self.assertEqual(0, star_pb.fleets[1].num_ships)
    self.assertNotEqual(0, star_pb.fleets[1].time_destroyed)


  def testFleetOf10vsFleetOf10(self):
    sim = _create_simulation(star_json = ["""
        {"key": "star1",
         "planets": [
           {"index": 0, "planet_type": 9} 
         ],
         "colonies": [
           {"key": "colony1", "empire_key": "empire1", "star_key": "star1", "planet_index": 0,
            "population": 100, "focus_population": 0.25,
            "focus_farming": 0.25, "focus_mining": 0.25, "focus_construction": 0.25}
         ],
         "last_simulation": %d,
         "buildings": [],
         "empires": [],
         "build_requests": [],
         "fleets": [
           {"key": "fleet1", "empire_key": "empire1", "design_name": "fighter", "num_ships": 10,
            "state": 3, "state_start_time": %d, "star_key": "star1", "target_fleet_key": "fleet2",
            "stance": 3},
           {"key": "fleet2", "empire_key": "empire2", "design_name": "fighter", "num_ships": 10,
            "state": 3, "state_start_time": %d, "star_key": "star1", "target_fleet_key": "fleet1",
            "stance": 3}
         ]
        }
      """ % (ctrl.dateTimeToEpoch(datetime.now() - timedelta(minutes=20)),
             ctrl.dateTimeToEpoch(datetime.now() - timedelta(minutes=15)),
             ctrl.dateTimeToEpoch(datetime.now() - timedelta(minutes=15)))])
    sim.simulate("star1")

    star_pb = sim.getStar("star1")
    self.assertEqual(2, len(star_pb.fleets))
    self.assertEqual("fleet1", star_pb.fleets[0].key)
    self.assertEqual(0, star_pb.fleets[0].num_ships)
    self.assertNotEqual(0, star_pb.fleets[0].time_destroyed)
    self.assertEqual("fleet2", star_pb.fleets[1].key)
    self.assertEqual(0, star_pb.fleets[1].num_ships)
    self.assertNotEqual(0, star_pb.fleets[1].time_destroyed)


  def testFleetOf20VsFleetOf36(self):
    sim = _create_simulation(star_json = ["""
       {"key": "star1",
         "planets": [
           {"index": 0, "planet_type": 9} 
         ],
         "colonies": [
           {"key": "colony1", "empire_key": "empire1", "star_key": "star1", "planet_index": 0,
            "population": 100, "focus_population": 0.25,
            "focus_farming": 0.25, "focus_mining": 0.25, "focus_construction": 0.25}
         ],
         "last_simulation": %d,
         "buildings": [],
         "empires": [],
         "build_requests": [],
         "fleets": [
           {"key": "fleet1", "empire_key": "empire1", "design_name": "fighter", "num_ships": 20,
            "state": 1, "state_start_time": 0, "star_key": "star1", "stance": 3},
           {"key": "fleet2", "empire_key": "empire2", "design_name": "fighter", "num_ships": 36,
            "state": 1, "state_start_time": 0, "star_key": "star1", "stance": 3}
         ]
        }
    """ % (ctrl.dateTimeToEpoch(datetime.now() - timedelta(minutes=20)))])
    sim.now = datetime.now() - timedelta(minutes=15)
    sim.onFleetArrived("fleet2", "star1")
    sim.now = datetime.now() - timedelta(minutes=10)
    sim.simulate("star1")

    star_pb = sim.getStar("star1")
    self.assertEqual(2, len(star_pb.fleets))
    self.assertEqual("fleet1", star_pb.fleets[0].key)
    self.assertEqual(0, star_pb.fleets[0].num_ships)
    self.assertNotEqual(0, star_pb.fleets[0].time_destroyed)
    self.assertEqual("fleet2", star_pb.fleets[1].key)
    self.assertEqual(26, int(star_pb.fleets[1].num_ships))
    self.assertEqual(0, star_pb.fleets[1].time_destroyed)


  def testNewFleetArrivesOnePassiveOneAgressiveExisting(self):
    sim = _create_simulation(star_json = ["""
        {"key": "star1",
         "planets": [
           {"index": 0, "planet_type": 9} 
         ],
         "colonies": [
           {"key": "colony1", "empire_key": "empire1", "star_key": "star1", "planet_index": 0,
            "population": 100, "focus_population": 0.25,
            "focus_farming": 0.25, "focus_mining": 0.25, "focus_construction": 0.25}
         ],
         "last_simulation": %d,
         "buildings": [],
         "empires": [],
         "build_requests": [],
         "fleets": [
           {"key": "fleet1", "empire_key": "empire1", "design_name": "fighter", "num_ships": 10,
            "state": 1, "state_start_time": 0, "star_key": "star1", "stance": 1},
           {"key": "fleet2", "empire_key": "empire1", "design_name": "fighter", "num_ships": 10,
            "state": 1, "state_start_time": 0, "star_key": "star1", "stance": 3},
           {"key": "fleet3", "empire_key": "empire2", "design_name": "fighter", "num_ships": 10,
            "state": 1, "state_start_time": 0, "star_key": "star1", "stance": 1}
         ]
        }
      """ % (ctrl.dateTimeToEpoch(datetime.now() - timedelta(minutes=20)))])
    sim.now = datetime.now() - timedelta(minutes=15)
    sim.onFleetArrived("fleet3", "star1")
    sim.now = datetime.now() - timedelta(minutes=10)
    sim.simulate("star1")

    star_pb = sim.getStar("star1")
    self.assertEqual(3, len(star_pb.fleets))
    self.assertEqual("fleet1", star_pb.fleets[0].key)
    self.assertEqual(10, star_pb.fleets[0].num_ships)
    self.assertEqual(0, star_pb.fleets[0].time_destroyed)
    self.assertEqual("fleet2", star_pb.fleets[1].key)
    self.assertEqual(10, star_pb.fleets[1].num_ships)
    self.assertEqual(0, star_pb.fleets[1].time_destroyed)
    self.assertEqual("fleet3", star_pb.fleets[2].key)
    self.assertEqual(0, star_pb.fleets[2].num_ships)
    self.assertNotEqual(0, star_pb.fleets[2].time_destroyed)


  def testNewFleetArrivesTwoAgressiveExisting(self):
    sim = _create_simulation(star_json = ["""
        {"key": "star1",
         "planets": [
           {"index": 0, "planet_type": 9} 
         ],
         "colonies": [
           {"key": "colony1", "empire_key": "empire1", "star_key": "star1", "planet_index": 0,
            "population": 100, "focus_population": 0.25,
            "focus_farming": 0.25, "focus_mining": 0.25, "focus_construction": 0.25}
         ],
         "last_simulation": %d,
         "buildings": [],
         "empires": [],
         "build_requests": [],
         "fleets": [
           {"key": "fleet1", "empire_key": "empire1", "design_name": "fighter", "num_ships": 10,
            "state": 1, "state_start_time": 0, "star_key": "star1", "stance": 3},
           {"key": "fleet2", "empire_key": "empire1", "design_name": "fighter", "num_ships": 10,
            "state": 1, "state_start_time": 0, "star_key": "star1", "stance": 3},
           {"key": "fleet3", "empire_key": "empire2", "design_name": "fighter", "num_ships": 20,
            "state": 1, "state_start_time": 0, "star_key": "star1", "stance": 1}
         ]
        }
      """ % (ctrl.dateTimeToEpoch(datetime.now() - timedelta(minutes=20)))])
    sim.now = datetime.now() - timedelta(minutes=15)
    sim.onFleetArrived("fleet3", "star1")
    sim.now = datetime.now() - timedelta(minutes=10)
    sim.simulate("star1")

    star_pb = sim.getStar("star1")
    self.assertEqual(3, len(star_pb.fleets))
    self.assertEqual("fleet1", star_pb.fleets[0].key)
    self.assertEqual(10, star_pb.fleets[0].num_ships)
    self.assertEqual(0, star_pb.fleets[0].time_destroyed)
    self.assertEqual("fleet2", star_pb.fleets[1].key)
    self.assertEqual(10, star_pb.fleets[1].num_ships)
    self.assertEqual(0, star_pb.fleets[1].time_destroyed)
    self.assertEqual("fleet3", star_pb.fleets[2].key)
    self.assertEqual(0, star_pb.fleets[2].num_ships)
    self.assertNotEqual(0, star_pb.fleets[2].time_destroyed)


  def testNewFleetArrivesTwoAgressiveOneWith20Existing(self):
    sim = _create_simulation(star_json = ["""
        {"key": "star1",
         "planets": [
           {"index": 0, "planet_type": 9} 
         ],
         "colonies": [
           {"key": "colony1", "empire_key": "empire1", "star_key": "star1", "planet_index": 0,
            "population": 100, "focus_population": 0.25,
            "focus_farming": 0.25, "focus_mining": 0.25, "focus_construction": 0.25}
         ],
         "last_simulation": %d,
         "buildings": [],
         "empires": [],
         "build_requests": [],
         "fleets": [
           {"key": "fleet1", "empire_key": "empire1", "design_name": "fighter", "num_ships": 20,
            "state": 1, "state_start_time": 0, "star_key": "star1", "stance": 3},
           {"key": "fleet2", "empire_key": "empire1", "design_name": "fighter", "num_ships": 10,
            "state": 1, "state_start_time": 0, "star_key": "star1", "stance": 3},
           {"key": "fleet3", "empire_key": "empire2", "design_name": "fighter", "num_ships": 20,
            "state": 1, "state_start_time": 0, "star_key": "star1", "stance": 3}
         ]
        }
      """ % (ctrl.dateTimeToEpoch(datetime.now() - timedelta(minutes=20)))])
    sim.now = datetime.now() - timedelta(minutes=15)
    sim.onFleetArrived("fleet3", "star1")
    sim.now = datetime.now() - timedelta(minutes=10)
    sim.simulate("star1")

    star_pb = sim.getStar("star1")
    self.assertEqual(3, len(star_pb.fleets))
    self.assertEqual("fleet1", star_pb.fleets[0].key)
    self.assertEqual(9, int(star_pb.fleets[0].num_ships))
    self.assertEqual(0, star_pb.fleets[0].time_destroyed)
    self.assertEqual("fleet2", star_pb.fleets[1].key)
    self.assertEqual(10, star_pb.fleets[1].num_ships)
    self.assertEqual(0, star_pb.fleets[1].time_destroyed)
    self.assertEqual("fleet3", star_pb.fleets[2].key)
    self.assertEqual(0, star_pb.fleets[2].num_ships)
    self.assertNotEqual(0, star_pb.fleets[2].time_destroyed)
    combat_report = sim.getCombatReport("star1")
    self.assertEqual(3, len(combat_report.rounds))

