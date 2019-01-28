package au.com.codeka.warworlds.server.model;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import au.com.codeka.common.model.BaseEmpire;
import au.com.codeka.common.model.BaseEmpireBattleRank;
import au.com.codeka.warworlds.server.data.SqlResult;

public class EmpireBattleRank extends BaseEmpireBattleRank {
  private int mEmpireID;

  public EmpireBattleRank() {
  }

  public EmpireBattleRank(SqlResult res) throws SQLException {
    mEmpireID = res.getInt("empire_id");
    mShipsDestroyed = res.getInt("ships_destroyed");
    mPopulationDestroyed = res.getInt("population_destroyed");
    mColoniesDestroyed = res.getInt("colonies_destroyed");
  }

  public int getEmpireID() {
    return mEmpireID;
  }

  public void updateEmpire(Collection<Empire> empires) {
    for (Empire empire : empires) {
      if (empire.getID() == mEmpireID) {
        mEmpire = empire;
      }
    }
    // if empire is still null, that's an error?
  }

  @Override
  protected BaseEmpire createEmpire() {
    return new Empire();
  }
}
