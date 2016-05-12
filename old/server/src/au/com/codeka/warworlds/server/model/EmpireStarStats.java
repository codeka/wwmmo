package au.com.codeka.warworlds.server.model;

import java.sql.SQLException;

import au.com.codeka.warworlds.server.data.SqlResult;

/** Contains stats about a empire (number of colonies, fleets etc) */
public class EmpireStarStats {
  private int empireID;
  private long numColonies;
  private long numFleets;
  private long numStars;

  public EmpireStarStats(SqlResult result) throws SQLException {
    empireID = result.getInt("empire_id");
    numColonies = result.getLong("num_colonies");
    numFleets = result.getLong("num_fleets");
    numStars = result.getLong("num_stars");
  }

  public int getEmpireID() {
    return empireID;
  }

  public long getNumColonies() {
    return numColonies;
  }

  public long getNumFleets() {
    return numFleets;
  }

  public long getNumStars() {
    return numStars;
  }
}
