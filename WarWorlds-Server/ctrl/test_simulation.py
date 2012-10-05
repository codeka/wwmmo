"""simulation_tse.py: Unit tests for simulation.py."""

from datetime import datetime, timedelta
import json
import unittest

import ctrl
from ctrl import simulation

import import_fixer
import_fixer.FixImports("google", "protobuf")

from protobufs import warworlds_pb2 as pb
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


class EmpirePresenceTestCase(unittest.TestCase):
  def testEmpirePresenceCreated(self):
    star_fetcher = MockStarFetcher(["""
        {"key": "star1",
         "planets": [
           {"index": 0, "planet_type": 9} 
         ],
         "colonies": [
           {"key": "colony1", "empire_key": "empire1", "star_key": "star1", "planet_index": 0,
            "population": 100, "last_simulation": %d, "focus_population": 0.25,
            "focus_farming": 0.25, "focus_mining": 0.25, "focus_construction": 0.25}
         ],
         "buildings": [],
         "empires": [],
         "build_requests": [],
         "fleets": []
        }
      """ % (ctrl.dateTimeToEpoch(datetime.now() - timedelta(minutes=1)))])
    sim = simulation.Simulation(star_fetcher=star_fetcher)
    sim.simulate("star1")

    star_pb = sim.getStar("star1")
    self.assertIsNotNone(star_pb)
    self.assertEqual(1, len(star_pb.empires))
    self.assertEqual("empire1", star_pb.empires[0].empire_key)


class CombatTestCase(unittest.TestCase):
  def testOneFleetVsPassiveFleet(self):
    star_fetcher = MockStarFetcher(["""
        {"key": "star1",
         "planets": [
           {"index": 0, "planet_type": 9} 
         ],
         "colonies": [
           {"key": "colony1", "empire_key": "empire1", "star_key": "star1", "planet_index": 0,
            "population": 100, "last_simulation": %d, "focus_population": 0.25,
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
         ]
        }
      """ % (ctrl.dateTimeToEpoch(datetime.now() - timedelta(minutes=1)),
             ctrl.dateTimeToEpoch(datetime.now()))])
    sim = simulation.Simulation(star_fetcher=star_fetcher)
    sim.simulate("star1")

    star_pb = sim.getStar("star1")
    self.assertEqual(2, len(star_pb.fleets))
    self.assertEqual("fleet1", star_pb.fleets[0].key)
    self.assertEqual(10, star_pb.fleets[0].num_ships)
    self.assertEqual(0, star_pb.fleets[0].time_destroyed)
    self.assertEqual("fleet2", star_pb.fleets[1].key)
    self.assertEqual(10, star_pb.fleets[1].num_ships)
    self.assertNotEqual(0, star_pb.fleets[1].time_destroyed)

  def testFleetOf10vsFleetOf20(self):
    star_fetcher = MockStarFetcher(["""
        {"key": "star1",
         "planets": [
           {"index": 0, "planet_type": 9} 
         ],
         "colonies": [
           {"key": "colony1", "empire_key": "empire1", "star_key": "star1", "planet_index": 0,
            "population": 100, "last_simulation": %d, "focus_population": 0.25,
            "focus_farming": 0.25, "focus_mining": 0.25, "focus_construction": 0.25}
         ],
         "buildings": [],
         "empires": [],
         "build_requests": [],
         "fleets": [
           {"key": "fleet1", "empire_key": "empire1", "design_name": "fighter", "num_ships": 10,
            "state": 3, "state_start_time": %d, "star_key": "star1", "target_fleet_key": "fleet2",
            "stance": 3},
           {"key": "fleet2", "empire_key": "empire2", "design_name": "fighter", "num_ships": 20,
            "state": 3, "state_start_time": %d, "star_key": "star1", "target_fleet_key": "fleet1",
            "stance": 3}
         ]
        }
      """ % (ctrl.dateTimeToEpoch(datetime.now() - timedelta(minutes=1)),
             ctrl.dateTimeToEpoch(datetime.now()),
             ctrl.dateTimeToEpoch(datetime.now()))])
    sim = simulation.Simulation(star_fetcher=star_fetcher)
    sim.simulate("star1")

    star_pb = sim.getStar("star1")
    self.assertEqual(2, len(star_pb.fleets))
    self.assertEqual("fleet1", star_pb.fleets[0].key)
    self.assertEqual(10, star_pb.fleets[0].num_ships)
    self.assertNotEqual(0, star_pb.fleets[0].time_destroyed)
    self.assertEqual("fleet2", star_pb.fleets[1].key)
    self.assertEqual(20, star_pb.fleets[1].num_ships)
    self.assertEqual(0, star_pb.fleets[1].time_destroyed)

  def testFleetOf20vsFleetOf10(self):
    star_fetcher = MockStarFetcher(["""
        {"key": "star1",
         "planets": [
           {"index": 0, "planet_type": 9} 
         ],
         "colonies": [
           {"key": "colony1", "empire_key": "empire1", "star_key": "star1", "planet_index": 0,
            "population": 100, "last_simulation": %d, "focus_population": 0.25,
            "focus_farming": 0.25, "focus_mining": 0.25, "focus_construction": 0.25}
         ],
         "buildings": [],
         "empires": [],
         "build_requests": [],
         "fleets": [
           {"key": "fleet1", "empire_key": "empire1", "design_name": "fighter", "num_ships": 20,
            "state": 3, "state_start_time": %d, "star_key": "star1", "target_fleet_key": "fleet2",
            "stance": 3},
           {"key": "fleet2", "empire_key": "empire2", "design_name": "fighter", "num_ships": 10,
            "state": 3, "state_start_time": %d, "star_key": "star1", "target_fleet_key": "fleet1",
            "stance": 3}
         ]
        }
      """ % (ctrl.dateTimeToEpoch(datetime.now() - timedelta(minutes=1)),
             ctrl.dateTimeToEpoch(datetime.now()),
             ctrl.dateTimeToEpoch(datetime.now()))])
    sim = simulation.Simulation(star_fetcher=star_fetcher)
    sim.simulate("star1")

    star_pb = sim.getStar("star1")
    self.assertEqual(2, len(star_pb.fleets))
    self.assertEqual("fleet1", star_pb.fleets[0].key)
    self.assertEqual(20, star_pb.fleets[0].num_ships)
    self.assertEqual(0, star_pb.fleets[0].time_destroyed)
    self.assertEqual("fleet2", star_pb.fleets[1].key)
    self.assertEqual(10, star_pb.fleets[1].num_ships)
    self.assertNotEqual(0, star_pb.fleets[1].time_destroyed)


  def testFleetOf10vsFleetOf10(self):
    star_fetcher = MockStarFetcher(["""
        {"key": "star1",
         "planets": [
           {"index": 0, "planet_type": 9} 
         ],
         "colonies": [
           {"key": "colony1", "empire_key": "empire1", "star_key": "star1", "planet_index": 0,
            "population": 100, "last_simulation": %d, "focus_population": 0.25,
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
            "state": 3, "state_start_time": %d, "star_key": "star1", "target_fleet_key": "fleet1",
            "stance": 3}
         ]
        }
      """ % (ctrl.dateTimeToEpoch(datetime.now() - timedelta(minutes=1)),
             ctrl.dateTimeToEpoch(datetime.now()),
             ctrl.dateTimeToEpoch(datetime.now()))])
    sim = simulation.Simulation(star_fetcher=star_fetcher)
    sim.simulate("star1")

    star_pb = sim.getStar("star1")
    self.assertEqual(2, len(star_pb.fleets))
    self.assertEqual("fleet1", star_pb.fleets[0].key)
    self.assertEqual(10, star_pb.fleets[0].num_ships)
    self.assertNotEqual(0, star_pb.fleets[0].time_destroyed)
    self.assertEqual("fleet2", star_pb.fleets[1].key)
    self.assertEqual(10, star_pb.fleets[1].num_ships)
    self.assertNotEqual(0, star_pb.fleets[1].time_destroyed)
