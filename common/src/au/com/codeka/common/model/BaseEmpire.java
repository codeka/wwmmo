package au.com.codeka.common.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.Messages;

public abstract class BaseEmpire {
    protected String mKey;
    protected String mDisplayName;
    protected String mEmailAddr;
    protected float mCash;
    protected BaseEmpireRank mRank;
    protected BaseStar mHomeStar;
    protected BaseAlliance mAlliance;
    protected DateTime mShieldLastUpdate;
    protected State mState;

    protected abstract BaseEmpireRank createEmpireRank(Messages.EmpireRank pb);
    protected abstract BaseStar createStar(Messages.Star pb);
    protected abstract BaseAlliance createAlliance(Messages.Alliance pb);

    public String getKey() {
        return mKey;
    }
    public String getDisplayName() {
        return mDisplayName;
    }
    public float getCash() {
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
    public String getEmailAddr() {
        return mEmailAddr;
    }
    public void setEmailAddr(String email) {
        mEmailAddr = email;
    }

    public void updateAlliance(BaseAlliance alliance) {
        mAlliance = alliance;
    }

    public void fromProtocolBuffer(Messages.Empire pb) {
        mKey = pb.getKey();
        mDisplayName = pb.getDisplayName();
        mCash = pb.getCash();
        mEmailAddr = pb.getEmail();
        mState = State.fromNumber(pb.getState().getNumber());

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
    }

    public void toProtocolBuffer(Messages.Empire.Builder pb) {
        pb.setKey(mKey);
        pb.setDisplayName(mDisplayName);
        pb.setCash(mCash);
        pb.setEmail(mEmailAddr);
        pb.setState(Messages.Empire.EmpireState.valueOf(mState.getValue()));

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
    }

    public enum State {
        ACTIVE(1),
        BANNED(2);

        private int mValue;

        State(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static State fromNumber(int value) {
            for(State s : State.values()) {
                if (s.getValue() == value) {
                    return s;
                }
            }

            return State.ACTIVE;
        }
    }

}
