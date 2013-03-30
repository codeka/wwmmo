"""statistics.py: Contains the logic for generating and using real-time statistics."""


import ctrl

from model import statistics as stats_mdl

from protobufs import messages_pb2 as pb

def getStandingQuery(key_name, ProtoBuffClass):
  cache_key = "SQ:"+key_name
  values = ctrl.getCached([cache_key], ProtoBuffClass)
  if cache_key in values:
    return values[cache_key]

  standing_query_mdl = stats_mdl.StandingQuery.get_by_key_name(key_name)
  if standing_query_mdl:
    value = ProtoBuffClass()
    value.ParseFromString(standing_query_mdl.protobuf)
    ctrl.setCached({cache_key: value})
    return value
  return None


def updateStandingQuery(key_name, protobuf):
  standing_query_mdl = stats_mdl.StandingQuery.get_by_key_name(key_name)
  if not standing_query_mdl:
    standing_query_mdl = stats_mdl.StandingQuery(key_name=key_name)
    standing_query_mdl.groupName = "building-statistics"
  standing_query_mdl.protobuf = protobuf.SerializeToString()
  standing_query_mdl.put()

  cache_key = "SQ:"+key_name
  ctrl.setCached({cache_key: protobuf})


def onBuildingConstructed(empire_key, design_id, count):
  """This is called to update the building-statistics standing query."""
  key_name = "building-statistics:%s" % empire_key
  empire_building_statistics_pb = getStandingQuery(key_name, pb.EmpireBuildingStatistics)
  if not empire_building_statistics_pb:
    empire_building_statistics_pb = pb.EmpireBuildingStatistics()

  found_design = False
  for design_count_pb in empire_building_statistics_pb.counts:
    if design_count_pb.design_id == design_id:
      design_count_pb.num_buildings += count
      found_design = True
      break
  if not found_design:
    design_count_pb = empire_building_statistics_pb.counts.add()
    design_count_pb.design_id = design_id
    design_count_pb.num_buildings = count

  updateStandingQuery(key_name, empire_building_statistics_pb)

