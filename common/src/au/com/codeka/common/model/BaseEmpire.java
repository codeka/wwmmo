package au.com.codeka.common.model;

import com.google.common.base.Strings;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.Alliance;
import au.com.codeka.common.protobuf.Empire;
import au.com.codeka.common.protobuf.EmpireRank;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.common.protobuf.Star;

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
    protected Double mTaxCollectedPerHour;
    protected DateTime mLastSeen;

    protected abstract BaseEmpireRank createEmpireRank(EmpireRank pb);
    protected abstract BaseStar createStar(Star pb);
    protected abstract BaseAlliance createAlliance(Alliance pb);

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

    public void updateAlliance(BaseAlliance alliance) {
        mAlliance = alliance;
    }

    public void fromProtocolBuffer(Empire pb) {
        mKey = pb.key;
        mDisplayName = pb.display_name;
        mCash = pb.cash;
        mEmailAddr = pb.email;
        mState = State.fromNumber(pb.state.getValue());
        if (pb.last_seen != null) {
            mLastSeen = new DateTime(pb.last_seen * 1000, DateTimeZone.UTC);
        }

        if (pb.rank != null && !Strings.isNullOrEmpty(pb.rank.empire_key)) {
            mRank = createEmpireRank(pb.rank);
        }

        if (pb.home_star != null && !Strings.isNullOrEmpty(pb.home_star.key)) {
            mHomeStar = createStar(pb.home_star);
        }

        if (pb.alliance != null && !Strings.isNullOrEmpty(pb.alliance.key)) {
            mAlliance = createAlliance(pb.alliance);
        }

        if (pb.shield_image_last_update != null) {
            mShieldLastUpdate = new DateTime(pb.shield_image_last_update * 1000, DateTimeZone.UTC);
        }

        mTaxCollectedPerHour = pb.taxes_collected_per_hour;
    }

    public void toProtocolBuffer(Empire.Builder pb, boolean isTrusted) {
        pb.key = mKey;
        pb.display_name = mDisplayName;
        pb.cash = mCash;
        if (isTrusted) {
            pb.email = mEmailAddr;
        } else {
            pb.email = "";
        }
        if (mState == State.ABANDONED) {
            pb.state = Empire.EmpireState.valueOf(State.ACTIVE.toString());
        } else {
            pb.state = Empire.EmpireState.valueOf(mState.toString());
        }
        if (mLastSeen != null) {
            pb.last_seen = mLastSeen.getMillis() / 1000;
        }

        if (mHomeStar != null) {
            Star.Builder star_pb = new Star.Builder();
            mHomeStar.toProtocolBuffer(star_pb, true);
            pb.home_star = star_pb.build();
        }

        if (mAlliance != null) {
            Alliance.Builder alliance_pb = new Alliance.Builder();
            mAlliance.toProtocolBuffer(alliance_pb);
            pb.alliance = alliance_pb.build();
        }

        if (mRank != null) {
            EmpireRank.Builder empire_rank_pb = new EmpireRank.Builder();
            mRank.toProtocolBuffer(empire_rank_pb);
            pb.rank = empire_rank_pb.build();
        }

        if (mShieldLastUpdate != null) {
            pb.shield_image_last_update = mShieldLastUpdate.getMillis() / 1000;
        }

        pb.taxes_collected_per_hour = mTaxCollectedPerHour;
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
            for(State s : State.values()) {
                if (s.getValue() == value) {
                    return s;
                }
            }

            return State.ACTIVE;
        }
    }

}
