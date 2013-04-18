package au.com.codeka.common.model;

import java.util.ArrayList;
import java.util.List;

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
    public List<BaseAllianceMember> getMembers() {
        return mMembers;
    }

    public void fromProtocolBuffer(Messages.Alliance pb) {
        mKey = pb.getKey();
        mName = pb.getName();
        mTimeCreated = new DateTime(pb.getTimeCreated() * 1000, DateTimeZone.UTC);
        mCreatorEmpireKey = pb.getCreatorEmpireKey();
        mNumMembers = pb.getNumMembers();

        if (pb.getMembersCount() > 0) {
            mMembers = new ArrayList<BaseAllianceMember>();
            for (Messages.AllianceMember member_pb : pb.getMembersList()) {
                mMembers.add(createAllianceMember(member_pb));
            }
        }
    }
}
