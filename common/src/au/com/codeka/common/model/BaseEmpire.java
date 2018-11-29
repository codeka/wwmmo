package au.com.codeka.common.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.Messages;

public abstract class BaseEmpire {
  protected String mKey;
  protected String mDisplayName;
  protected String mEmailAddr;
  protected double mCash;
  protected BaseEmpireRank mRank;
  protected BaseStar mHomeStar;
  protected BaseAlliance mAlliance;
  protected DateTime mShieldLastUpdate;
  protected State mState;
  protected Double mTaxCollectedPerHour;
  protected DateTime mLastSeen;
  protected PatreonLevel mPatreonLevel;

  protected abstract BaseEmpireRank createEmpireRank(Messages.EmpireRank pb);

  protected abstract BaseStar createStar(Messages.Star pb);

  protected abstract BaseAlliance createAlliance(Messages.Alliance pb);

  public String getKey() {
    return mKey;
  }

  public String getDisplayName() {
    return mDisplayName;
  }

  public double getCash() {
    return mCash;
  }

  public BaseEmpireRank getRank() {
    return mRank;
  }

  public BaseStar getHomeStar() {
    return mHomeStar;
  }

  public BaseAlliance getAlliance() {
    return mAlliance;
  }

  public DateTime getShieldLastUpdate() {
    return mShieldLastUpdate;
  }

  public State getState() {
    return mState;
  }

  public String getEmailAddr() {
    return mEmailAddr;
  }

  public void setEmailAddr(String email) {
    mEmailAddr = email;
  }

  public Double getTaxCollectedPerHour() {
    return mTaxCollectedPerHour;
  }

  public DateTime getLastSeen() {
    return mLastSeen;
  }

  public PatreonLevel getPatreonLevel() {
    return mPatreonLevel;
  }

  public void updateAlliance(BaseAlliance alliance) {
    mAlliance = alliance;
  }

  public void fromProtocolBuffer(Messages.Empire pb) {
    mKey = pb.getKey();
    mDisplayName = pb.getDisplayName();
    mCash = pb.getCash();
    if (pb.hasCash64()) {
      mCash = pb.getCash64();
    }
    mEmailAddr = pb.getEmail();
    mState = State.fromNumber(pb.getState().getNumber());
    if (pb.hasLastSeen()) {
      mLastSeen = new DateTime(pb.getLastSeen() * 1000, DateTimeZone.UTC);
    }

    if (pb.getRank() != null && pb.getRank().getEmpireKey() != null &&
        pb.getRank().getEmpireKey().length() > 0) {
      mRank = createEmpireRank(pb.getRank());
    }

    if (pb.getHomeStar() != null && pb.getHomeStar().getKey() != null &&
        pb.getHomeStar().getKey().length() > 0) {
      mHomeStar = createStar(pb.getHomeStar());
    }

    if (pb.getAlliance() != null && pb.getAlliance().getKey() != null &&
        pb.getAlliance().getKey().length() > 0) {
      mAlliance = createAlliance(pb.getAlliance());
    }

    if (pb.hasShieldImageLastUpdate()) {
      mShieldLastUpdate = new DateTime(pb.getShieldImageLastUpdate() * 1000, DateTimeZone.UTC);
    }

    if (pb.hasTaxesCollectedPerHour()) {
      mTaxCollectedPerHour = pb.getTaxesCollectedPerHour();
    }

    if (pb.hasPatreonLevel()) {
      mPatreonLevel = PatreonLevel.fromNumber(pb.getPatreonLevel().getNumber());
    }
  }

  public void toProtocolBuffer(Messages.Empire.Builder pb, boolean isTrusted) {
    pb.setKey(mKey);
    pb.setDisplayName(mDisplayName);
    pb.setCash((float) mCash);
    pb.setCash64(mCash);
    if (isTrusted) {
      pb.setEmail(mEmailAddr);
    } else {
      pb.setEmail("");
    }
    if (mState == State.ABANDONED) {
      pb.setState(Messages.Empire.EmpireState.forNumber(State.ACTIVE.getValue()));
    } else {
      pb.setState(Messages.Empire.EmpireState.forNumber(mState.getValue()));
    }
    if (mLastSeen != null) {
      pb.setLastSeen(mLastSeen.getMillis() / 1000);
    }

    if (mHomeStar != null) {
      Messages.Star.Builder star_pb = Messages.Star.newBuilder();
      mHomeStar.toProtocolBuffer(star_pb, true);
      pb.setHomeStar(star_pb);
    }

    if (mAlliance != null) {
      Messages.Alliance.Builder alliance_pb = Messages.Alliance.newBuilder();
      mAlliance.toProtocolBuffer(alliance_pb);
      pb.setAlliance(alliance_pb);
    }

    if (mRank != null) {
      Messages.EmpireRank.Builder empire_rank_pb = Messages.EmpireRank.newBuilder();
      mRank.toProtocolBuffer(empire_rank_pb);
      pb.setRank(empire_rank_pb);
    }

    if (mShieldLastUpdate != null) {
      pb.setShieldImageLastUpdate(mShieldLastUpdate.getMillis() / 1000);
    }

    if (mTaxCollectedPerHour != null) {
      pb.setTaxesCollectedPerHour(mTaxCollectedPerHour);
    }

    pb.setPatreonLevel(Messages.Empire.PatreonLevel.forNumber(mPatreonLevel.getValue()));
  }

  public enum State {
    ACTIVE(1),
    BANNED(2),
    ABANDONED(3);

    private int mValue;

    State(int value) {
      mValue = value;
    }

    public int getValue() {
      return mValue;
    }

    public static State fromNumber(int value) {
      for (State s : State.values()) {
        if (s.getValue() == value) {
          return s;
        }
      }

      return State.ACTIVE;
    }
  }

  public enum PatreonLevel {
    NONE(0),
    FAN(1),
    PATRON(2),
    EMPIRE(3);

    private int value;

    PatreonLevel(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }

    public static PatreonLevel fromNumber(int value) {
      for (PatreonLevel level : PatreonLevel.values()) {
        if (level.getValue() == value) {
          return level;
        }
      }

      return PatreonLevel.NONE;
    }

    public static PatreonLevel fromPledge(int pledgeCents) {
      if (pledgeCents < 100) {
        return PatreonLevel.NONE;
      }
      if (pledgeCents < 500) {
        return PatreonLevel.FAN;
      }
      if (pledgeCents < 1000) {
        return PatreonLevel.PATRON;
      }
      return PatreonLevel.EMPIRE;
    }
  }
}
