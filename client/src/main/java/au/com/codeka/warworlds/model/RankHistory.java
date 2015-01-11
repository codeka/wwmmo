package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.Messages;

public class RankHistory {
    private DateTime mDate;
    private List<EmpireRank> mRanks;

    public DateTime getDate() {
        return mDate;
    }
    public List<EmpireRank> getRanks() {
        return mRanks;
    }

    public void fromProtocolBuffer(Messages.EmpireRanks pb) {
        mDate = new DateTime(pb.getDate() * 1000, DateTimeZone.UTC);
        mRanks = new ArrayList<EmpireRank>();
        for (Messages.EmpireRank empire_rank_pb : pb.getRanksList()) {
            EmpireRank rank = new EmpireRank();
            rank.fromProtocolBuffer(empire_rank_pb);
            mRanks.add(rank);
        }
    }
}
