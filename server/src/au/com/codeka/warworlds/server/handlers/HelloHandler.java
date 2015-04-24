package au.com.codeka.warworlds.server.handlers;

import java.util.ArrayList;
import java.util.List;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.protobuf.EmpireBuildingStatistics;
import au.com.codeka.common.protobuf.HelloRequest;
import au.com.codeka.common.protobuf.HelloResponse;
import au.com.codeka.common.protobuf.MessageOfTheDay;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.ctrl.StatisticsController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Colony;
import au.com.codeka.warworlds.server.model.Empire;
import au.com.codeka.warworlds.server.model.EmpireStarStats;
import au.com.codeka.warworlds.server.model.Star;

public class HelloHandler extends RequestHandler {
  private final Log log = new Log("HelloHandler");

  @Override
  protected void get() throws RequestException {
    // this is just used for testing, nothing more...
    if (!getSession().isAdmin()) {
      throw new RequestException(501);
    }

    HelloRequest hello_request_pb = new HelloRequest.Builder()
        .allow_inline_notfications(false)
        .device_build("TEST_DEVICE_BUILD")
        .device_manufacturer("TEST_DEVICE_MANUFACTURER")
        .device_model("TEST_DEVICE_MODEL")
        .device_version("TEST_DEVICE_VERSION")
        .memory_class(0)
        .build();
    processHello(hello_request_pb);
  }

  @Override
  protected void put() throws RequestException {
    processHello(getRequestBody(HelloRequest.class));
  }

  private void processHello(HelloRequest hello_request_pb) throws RequestException {
    HelloResponse hello_response_pb = new HelloResponse();

    // damn, this is why things should never be marked "required" in protobufs!
    hello_response_pb.motd = new MessageOfTheDay.Builder().message("").last_update("").build();

    // fetch the empire we're interested in
    Empire empire = new EmpireController().getEmpire(getSession().getEmpireID());
    if (empire != null) {
      new StatisticsController().registerLogin(getSession().getEmpireID(), hello_request_pb);
      if (empire.getState() == Empire.State.ABANDONED) {
        new EmpireController().markActive(empire);
      }

      // Make sure they haven't been wiped out.
      EmpireStarStats stats = new EmpireController().getEmpireStarStats(getSession().getEmpireID());
      if (stats.getNumColonies() == 0) {
        log.info(
            "Empire #%d [%s] has been wiped out (%d stars, %d colonies, %d fleets), resetting.",
            empire.getID(), empire.getDisplayName(), stats.getNumStars(), stats.getNumColonies(),
            stats.getNumFleets());
        new EmpireController().createEmpire(empire);
        hello_response_pb.was_empire_reset = true;

        String resetReason = new EmpireController().getResetReason(empire.getID());
        if (resetReason != null) {
          hello_response_pb.empire_reset_reason = resetReason;
        }
      } else {
        log.info("Empire #%d [%s] has %d stars, %d colonies, and %d fleets.", empire.getID(),
            empire.getDisplayName(), stats.getNumStars(), stats.getNumColonies(),
            stats.getNumFleets());
      }

      au.com.codeka.common.protobuf.Empire empire_pb = new au.com.codeka.common.protobuf.Empire();
      empire.toProtocolBuffer(empire_pb, true);
      hello_response_pb.empire = empire_pb;

      // set up the initial building statistics
      String sql =
          "SELECT design_id, COUNT(*) FROM buildings WHERE empire_id = ? GROUP BY design_id";
      try (SqlStmt stmt = DB.prepare(sql)) {
        stmt.setInt(1, empire.getID());
        SqlResult res = stmt.select();

        EmpireBuildingStatistics build_stats_pb = new EmpireBuildingStatistics();
        build_stats_pb.counts = new ArrayList<>();
        while (res.next()) {
          String designID = res.getString(1);
          int num = res.getInt(2);

          EmpireBuildingStatistics.DesignCount design_count_pb =
              new EmpireBuildingStatistics.DesignCount();
          design_count_pb.design_id = designID;
          design_count_pb.num_buildings = num;
          build_stats_pb.counts.add(design_count_pb);
        }
        hello_response_pb.building_statistics = build_stats_pb;
      } catch (Exception e) {
        throw new RequestException(e);
      }

      // if we're set to force ignore ads, make sure we pass that along
      hello_response_pb.force_remove_ads = empire.getForceRemoveAds();

      if (hello_request_pb.no_star_list == null || !hello_request_pb.no_star_list) {
        hello_response_pb.star_ids = new ArrayList<>();

        // grab all of the empire's stars (except markers and wormholes) and send across the
        // identifiers
        sql = "SELECT id, name" +
            " FROM stars" +
            " INNER JOIN (SELECT DISTINCT star_id FROM colonies WHERE empire_id = ?" +
            " UNION SELECT DISTINCT star_id FROM fleets WHERE empire_id = ?) as s" +
            " ON s.star_id = stars.id" +
            " WHERE star_type NOT IN (" + Star.Type.Marker.ordinal() + ", " + Star.Type.Wormhole
            .ordinal() + ")" +
            " ORDER BY name ASC";
        try (SqlStmt stmt = DB.prepare(sql)) {
          stmt.setInt(1, empire.getID());
          stmt.setInt(2, empire.getID());
          SqlResult res = stmt.select();

          while (res.next()) {
            hello_response_pb.star_ids.add(res.getLong(1));
          }
        } catch (Exception e) {
          throw new RequestException(e);
        }
      }
    }

    setResponseBody(hello_response_pb);
  }

  /**
   * Goes through the given stars and looks for at least one colony. Returns true if one is
   * found.
   */
  private boolean findColony(ArrayList<Integer> starIDs, int empireID) throws RequestException {
    // do it in groups of 10 stars, since most of the time we'll find it in the first group
    // and there's no point querying all, especially if they have a lot of stars.
    for (int startIndex = 0; startIndex < starIDs.size(); startIndex++) {
      int endIndex = startIndex + 10;
      if (endIndex >= starIDs.size()) {
        endIndex = starIDs.size() - 1;
      }

      List<Integer> sublist = starIDs.subList(startIndex, endIndex);
      List<Star> stars = new StarController().getStars(sublist);
      if (findColony(stars, empireID)) {
        return true;
      }
    }

    return false;
  }

  private boolean findColony(List<Star> stars, int empireID) {
    for (Star star : stars) {
      for (BaseColony baseColony : star.getColonies()) {
        Colony colony = (Colony) baseColony;
        if (colony.getEmpireID() != null && empireID == colony.getEmpireID()) {
          return true;
        }
      }
    }
    return false;
  }
}
