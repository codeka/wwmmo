package au.com.codeka.warworlds.model;

import au.com.codeka.common.model.BaseEmpire;
import au.com.codeka.common.model.BaseEmpireBattleRank;

public class EmpireBattleRank extends BaseEmpireBattleRank {
  @Override
  protected BaseEmpire createEmpire() {
    return new Empire();
  }

  @Override
  public Empire getEmpire() {
    return (Empire) super.getEmpire();
  }
}
