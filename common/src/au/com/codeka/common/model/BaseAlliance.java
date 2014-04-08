package au.com.codeka.common.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.Messages;

public abstract class BaseAlliance {
    protected String mKey;
    protected String mName;
    protected DateTime mTimeCreated;
    protected String mCreatorEmpireKey;
    protected int mNumMembers;
    protected List<BaseAllianceMember> mMembers;
    protected double mBankBalance;
    protected DateTime mDateImageUpdated;

    protected abstract BaseAllianceMember createAllianceMember(Messages.AllianceMember pb);

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

    public void fromProtocolBuffer(Messages.Alliance pb) {
        if (pb.hasKey()) {
            mKey = pb.getKey();
        }
        mName = pb.getName();
        mTimeCreated = new DateTime(pb.getTimeCreated() * 1000, DateTimeZone.UTC);
        if (pb.hasCreatorEmpireKey()) {
            mCreatorEmpireKey = pb.getCreatorEmpireKey();
        }
        if (pb.hasNumMembers()) {
            mNumMembers = pb.getNumMembers();
        }
        mBankBalance = pb.getBankBalance();

        if (pb.getMembersCount() > 0) {
            mMembers = new ArrayList<BaseAllianceMember>();
            for (Messages.AllianceMember member_pb : pb.getMembersList()) {
                mMembers.add(createAllianceMember(member_pb));
            }
        }

        mDateImageUpdated = new DateTime(pb.getDateImageUpdated() * 1000, DateTimeZone.UTC);
    }

    public void toProtocolBuffer(Messages.Alliance.Builder pb) {
        if (mKey != null) {
            pb.setKey(mKey);
        }
        pb.setName(mName);
        pb.setTimeCreated(mTimeCreated.getMillis() / 1000);
        pb.setCreatorEmpireKey(mCreatorEmpireKey);
        pb.setNumMembers(mNumMembers);
        pb.setBankBalance(mBankBalance);
        pb.setDateImageUpdated(mDateImageUpdated.getMillis() / 1000);

        if (mMembers != null) {
            for (BaseAllianceMember member : mMembers) {
                Messages.AllianceMember.Builder member_pb = Messages.AllianceMember.newBuilder();
                member.toProtocolBuffer(member_pb);
                pb.addMembers(member_pb);
            }
        }
    }
}
