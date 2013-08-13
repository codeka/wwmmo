package au.com.codeka.warworlds.model;

import au.com.codeka.common.model.BaseAlliance;
import au.com.codeka.common.model.BaseAllianceMember;
import au.com.codeka.common.protobuf.Messages;

public class Alliance extends BaseAlliance {
    private int mID;

    public int getID() {
        return mID;
    }

    @Override
    public void fromProtocolBuffer(Messages.Alliance pb) {
        super.fromProtocolBuffer(pb);
        if (pb.hasKey()) {
            mID = Integer.parseInt(pb.getKey());
        }
    }

    @Override
    protected BaseAllianceMember createAllianceMember(Messages.AllianceMember pb) {
        AllianceMember am = new AllianceMember();
        am.fromProtocolBuffer(pb);
        return am;
    }
}
