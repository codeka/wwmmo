package au.com.codeka.common.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.AllianceMember;

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
    public Integer getEmpireID() {
        return Integer.parseInt(mEmpireKey);
    }
    public DateTime getTimeJoined() {
        return mTimeJoined;
    }
    public Rank getRank() {
        return mRank;
    }

    public void fromProtocolBuffer(AllianceMember pb) {
        mKey = pb.key;
        mAllianceKey = pb.alliance_key;
        mEmpireKey = pb.empire_key;
        mTimeJoined = new DateTime(pb.time_joined * 1000, DateTimeZone.UTC);
        if (pb.rank != null) {
            mRank = Rank.fromNumber(pb.rank.getValue());
        } else {
            mRank = Rank.MEMBER;
        }
    }

    public void toProtocolBuffer(AllianceMember pb) {
        pb.key = mKey;
        pb.alliance_key = mAllianceKey;
        pb.empire_key = mEmpireKey;
        pb.time_joined = mTimeJoined.getMillis() / 1000;
        pb.rank = AllianceMember.Rank.valueOf(mRank.toString());
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
