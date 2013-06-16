package au.com.codeka.warworlds.server.handlers;

import java.sql.ResultSet;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;

public class RankingHistoryHandler extends RequestHandler {
    @Override
    protected void get() throws RequestException {
        int year = Integer.parseInt(getUrlParameter("year"));
        int month = Integer.parseInt(getUrlParameter("month"));

        String sql = "SELECT empire_id, rank, total_stars, total_colonies, total_buildings," +
                           " total_ships, total_population" +
                    " FROM empire_rank_histories"+
                    " WHERE date >= ?" +
                      " AND date < ?" +
                    " ORDER BY rank, date DESC";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setDateTime(1, new DateTime(year, month, 1, 0, 0, DateTimeZone.UTC));
            stmt.setDateTime(2, new DateTime(year, month, 1, 0, 0, DateTimeZone.UTC).plusMonths(1));
            ResultSet rs = stmt.select();

            int lastRank = 0;
            Messages.EmpireRanks.Builder empire_ranks_pb = Messages.EmpireRanks.newBuilder();
            empire_ranks_pb.setDate(new DateTime(year, month, 1, 0, 0, DateTimeZone.UTC).getMillis() / 1000);
            while (rs.next()) {
                int rank = rs.getInt(2);
                if (rank == lastRank) {
                    continue;
                }
                lastRank = rank;

                Messages.EmpireRank.Builder empire_rank_pb = Messages.EmpireRank.newBuilder();
                empire_rank_pb.setEmpireKey(Integer.toString(rs.getInt(1)));
                empire_rank_pb.setRank(rank);
                empire_rank_pb.setTotalStars(rs.getInt(3));
                empire_rank_pb.setTotalColonies(rs.getInt(4));
                empire_rank_pb.setTotalBuildings(rs.getInt(5));
                empire_rank_pb.setTotalShips(rs.getInt(6));
                empire_rank_pb.setTotalPopulation(rs.getInt(7));
                empire_ranks_pb.addRanks(empire_rank_pb);
            }

            setResponseBody(empire_ranks_pb.build());
        } catch(Exception e) {
            throw new RequestException(e);
        }
    }
}
