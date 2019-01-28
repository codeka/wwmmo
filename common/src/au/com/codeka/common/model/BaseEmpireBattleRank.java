package au.com.codeka.common.model;

import au.com.codeka.common.protobuf.Messages;

public abstract class BaseEmpireBattleRank {
  protected BaseEmpire mEmpire;
  protected int mShipsDestroyed;
  protected int mPopulationDestroyed;
  protected int mColoniesDestroyed;

  protected abstract BaseEmpire createEmpire();

  public BaseEmpire getEmpire() {
    return mEmpire;
  }

  public int getShipsDestroyed() {
    return mShipsDestroyed;
  }

  public int getPopulationDestroyed() {
    return mPopulationDestroyed;
  }

  public int getColoniesDestroyed() {
    return mColoniesDestroyed;
  }

  public void fromProtocolBuffer(Messages.EmpireBattleRank pb) {
    mEmpire = createEmpire();
    mEmpire.fromProtocolBuffer(pb.getEmpire());
    mShipsDestroyed = pb.getShipsDestroyed();
    mPopulationDestroyed = pb.getPopulationDestroyed();
    mColoniesDestroyed = pb.getColoniesDestroyed();
  }

  public void toProtocolBuffer(Messages.EmpireBattleRank.Builder pb) {
    Messages.Empire.Builder empireBuilder = Messages.Empire.newBuilder();
    mEmpire.toProtocolBuffer(empireBuilder, false);
    pb.setEmpire(empireBuilder.build());

    pb.setShipsDestroyed(mShipsDestroyed);
    pb.setPopulationDestroyed(mPopulationDestroyed);
    pb.setColoniesDestroyed(mColoniesDestroyed);
  }
}
