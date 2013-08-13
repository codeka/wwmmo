package au.com.codeka.common.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.Messages;

public class BaseAllianceMember {
    protected String mKey;
    protected String mAllianceKey;
    protected String mEmpireKey;
    protected DateTime mTimeJoined;
    protected Rank mRank;

    public String getKey() {
        return mKey;
    }
    public String getAllianceKey() {
        return mAllianceKey;
    }
    public String getEmpireKey() {
        return mEmpireKey;
    }
    public DateTime getTimeJoined() {
        return mTimeJoined;
    }
    public Rank getRank() {
        return mRank;
    }

    public void fromProtocolBuffer(Messages.AllianceMember pb) {
        if (pb.hasKey()) {
            mKey = pb.getKey();
        }
        mAllianceKey = pb.getAllianceKey();
        mEmpireKey = pb.getEmpireKey();
        mTimeJoined = new DateTime(pb.getTimeJoined() * 1000, DateTimeZone.UTC);
        if (pb.hasRank()) {
            mRank = Rank.fromNumber(pb.getRank().getNumber());
        } else {
            mRank = Rank.MEMBER;
        }
    }

    public void toProtocolBuffer(Messages.AllianceMember.Builder pb) {
        if (mKey != null) {
            pb.setKey(mKey);
        }
        pb.setAllianceKey(mAllianceKey);
        pb.setEmpireKey(mEmpireKey);
        pb.setTimeJoined(mTimeJoined.getMillis() / 1000);
        pb.setRank(Messages.AllianceMember.Rank.valueOf(mRank.getNumber()));
    }

    public enum Rank {
        CAPTAIN(0, 10),
        LIEUTENANT(1, 5),
        MEMBER(2, 1);

        private int mNumber;
        private int mNumVotes;

        Rank(int number, int numVotes) {
            mNumber = number;
            mNumVotes = numVotes;
        }

        public int getNumber() {
            return mNumber;
        }
        public int getNumVotes() {
            return mNumVotes;
        }

        public static Rank fromNumber(int number) {
            return Rank.values()[number];
        }
    }
}
