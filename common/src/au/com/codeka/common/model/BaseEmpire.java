package au.com.codeka.common.model;

import au.com.codeka.common.protobuf.Messages;

public abstract class BaseEmpire {
    protected String mKey;
    protected String mDisplayName;
    protected String mEmailAddr;
    protected float mCash;
    protected BaseEmpireRank mRank;
    protected BaseStar mHomeStar;
    protected BaseAlliance mAlliance;

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
    }

    public void toProtocolBuffer(Messages.Empire.Builder pb) {
        pb.setKey(mKey);
        pb.setDisplayName(mDisplayName);
        pb.setCash(mCash);
        pb.setEmail(mEmailAddr);
        pb.setState(Messages.Empire.EmpireState.INITIAL);

        if (mHomeStar != null) {
            Messages.Star.Builder star_pb = Messages.Star.newBuilder();
            mHomeStar.toProtocolBuffer(star_pb);
            pb.setHomeStar(star_pb);
        }
    }
}
