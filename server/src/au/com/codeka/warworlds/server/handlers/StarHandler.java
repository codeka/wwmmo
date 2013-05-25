package au.com.codeka.warworlds.server.handlers;

import java.util.ArrayList;

import org.joda.time.DateTime;

import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseScoutReport;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.BuildRequest;
import au.com.codeka.warworlds.server.model.Fleet;
import au.com.codeka.warworlds.server.model.ScoutReport;
import au.com.codeka.warworlds.server.model.Star;

/**
 * Handles /realm/.../stars/{id} URL
 */
public class StarHandler extends RequestHandler {
    @Override
    protected void get() throws RequestException {
        int id = Integer.parseInt(getUrlParameter("star_id"));
        Star star = new StarController().getStar(id);
        if (star == null) {
            throw new RequestException(404);
        }

        int myEmpireID = getSession().getEmpireID();
        sanitizeStar(star, myEmpireID);

        Messages.Star.Builder star_pb = Messages.Star.newBuilder();
        star.toProtocolBuffer(star_pb);
        setResponseBody(star_pb.build());
    }

    /**
     * "Sanitizes" a star and removes all info specific to other empires.
     * @param star
     * @param myEmpireID
     */
    public static void sanitizeStar(Star star, int myEmpireID) {
        // if we don't have any fleets here, remove all the others
        boolean ourFleetExists = false;
        for (BaseFleet baseFleet : star.getFleets()) {
            Fleet fleet = (Fleet) baseFleet;
            if (fleet.getEmpireID() == myEmpireID) {
                ourFleetExists = true;
            }
        }
        if (!ourFleetExists) {
            star.getFleets().clear();
        }

        // remove build requests that aren't ours
        if (star.getBuildRequests() != null) {
            ArrayList<BaseBuildRequest> toRemove = new ArrayList<BaseBuildRequest>();
            for (BaseBuildRequest baseBuildRequest : star.getBuildRequests()) {
                BuildRequest buildRequest = (BuildRequest) baseBuildRequest;
                if (buildRequest.getEmpireID() != myEmpireID) {
                    toRemove.add(baseBuildRequest);
                }
            }
            star.getBuildRequests().removeAll(toRemove);
        }

        // remove all scout reports that aren't ours
        if (star.getScoutReports() != null) {
            ArrayList<BaseScoutReport> toRemove = new ArrayList<BaseScoutReport>();
            for (BaseScoutReport baseScoutReport : star.getScoutReports()) {
                ScoutReport scoutReport = (ScoutReport) baseScoutReport;
                if (!scoutReport.getEmpireKey().equals(Integer.toString(myEmpireID))) {
                    toRemove.add(baseScoutReport);
                }
            }
            star.getScoutReports().removeAll(toRemove);
        }
    }

    @Override
    protected void put() throws RequestException {
        Messages.StarRenameRequest star_rename_request_pb = getRequestBody(Messages.StarRenameRequest.class);
        if (!star_rename_request_pb.getStarKey().equals(getUrlParameter("star_id"))) {
            throw new RequestException(404);
        }
        int starID = Integer.parseInt(getUrlParameter("star_id"));

        if (star_rename_request_pb.getNewName().trim().equals("")) {
            throw new RequestException(400);
        }

        String sql = "UPDATE stars SET name = ? WHERE id = ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setString(1, star_rename_request_pb.getNewName().trim());
            stmt.setInt(2, starID);
            stmt.update();
        } catch(Exception e) {
            throw new RequestException(e);
        }

        sql = "INSERT INTO star_renames (star_id, old_name, new_name, purchase_developer_payload," +
                                       " purchase_order_id, purchase_price, purchase_time)" +
             " VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setInt(1, starID);
            stmt.setString(2, star_rename_request_pb.getOldName());
            stmt.setString(3, star_rename_request_pb.getNewName().trim());
            stmt.setString(4, star_rename_request_pb.getPurchaseDeveloperPayload());
            stmt.setString(5, star_rename_request_pb.getPurchaseOrderId());
            stmt.setString(6, star_rename_request_pb.getPurchasePrice());
            stmt.setDateTime(7, DateTime.now());
            stmt.update();
        } catch(Exception e) {
            throw new RequestException(e);
        }

        Star star = new StarController().getStar(starID);
        Messages.Star.Builder star_pb = Messages.Star.newBuilder();
        star.toProtocolBuffer(star_pb);
        setResponseBody(star_pb.build());
    }
}
