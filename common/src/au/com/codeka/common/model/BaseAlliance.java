package au.com.codeka.common.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.Alliance;
import au.com.codeka.common.protobuf.AllianceMember;

public abstract class BaseAlliance {
    protected String mKey;
    protected String mName;
    protected DateTime mTimeCreated;
    protected String mCreatorEmpireKey;
    protected int mNumMembers;
    protected List<BaseAllianceMember> mMembers;
    protected double mBankBalance;
    protected DateTime mDateImageUpdated;
    protected Integer mNumPendingRequests;

    protected abstract BaseAllianceMember createAllianceMember(AllianceMember pb);

    public String getKey() {
        return mKey;
    }
    public String getName() {
        return mName;
    }
    public DateTime getTimeCreated() {
        return mTimeCreated;
    }
    public String getCreatorEmpireKey() {
        return mCreatorEmpireKey;
    }
    public int getNumMembers() {
        if (mNumMembers == 0 && mMembers != null) {
            return mMembers.size();
        }
        return mNumMembers;
    }
    public double getBankBalance() {
        return mBankBalance;
    }
    public List<BaseAllianceMember> getMembers() {
        return mMembers;
    }
    public DateTime getDateImageUpdated() {
        return mDateImageUpdated;
    }
    public Integer getNumPendingRequests() {
        return mNumPendingRequests;
    }

    public int getTotalPossibleVotes(Set<Integer> excludingEmpires) {
        int totalVotes = 0;
        for (BaseAllianceMember member : mMembers) {
            int memberEmpireID = Integer.parseInt(member.getEmpireKey());
            if (excludingEmpires.contains(memberEmpireID)) {
                continue;
            }

            totalVotes += member.getRank().getNumVotes();
        }
        return totalVotes;
    }

    public void fromProtocolBuffer(Alliance pb) {
        mKey = pb.key;
        mName = pb.name;
        mTimeCreated = new DateTime(pb.time_created * 1000, DateTimeZone.UTC);
        mCreatorEmpireKey = pb.creator_empire_key;
        if (pb.num_members != null) {
            mNumMembers = pb.num_members;
        }
        mBankBalance = pb.bank_balance;

        if (pb.members.size() > 0) {
            mMembers = new ArrayList<>();
            for (AllianceMember member_pb : pb.members) {
                mMembers.add(createAllianceMember(member_pb));
            }
        }

        mDateImageUpdated = new DateTime(pb.date_image_updated * 1000, DateTimeZone.UTC);

        if (pb.num_pending_requests != null) {
            mNumPendingRequests = pb.num_pending_requests;
        }
    }

    public void toProtocolBuffer(Alliance.Builder pb) {
        pb.key = mKey;
        pb.name = mName;
        pb.time_created = mTimeCreated.getMillis() / 1000;
        pb.creator_empire_key = mCreatorEmpireKey;
        pb.num_members = mNumMembers;
        pb.bank_balance = mBankBalance;
        pb.date_image_updated = mDateImageUpdated.getMillis() / 1000;
        if (mNumPendingRequests != null) {
            pb.num_pending_requests = mNumPendingRequests;
        }

        if (mMembers != null) {
            pb.members = new ArrayList<>();
            for (BaseAllianceMember member : mMembers) {
                AllianceMember.Builder member_pb = new AllianceMember.Builder();
                member.toProtocolBuffer(member_pb);
                pb.members.add(member_pb.build());
            }
        }
    }
}
