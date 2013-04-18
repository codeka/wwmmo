package au.com.codeka.warworlds.server.model;

import au.com.codeka.common.model.BaseAlliance;
import au.com.codeka.common.model.BaseAllianceMember;
import au.com.codeka.common.protobuf.Messages;

public class Alliance extends BaseAlliance {
    @Override
    protected BaseAllianceMember createAllianceMember(Messages.AllianceMember pb) {
        AllianceMember am = new AllianceMember();
        if (pb != null) {
            am.fromProtocolBuffer(pb);
        }
        return am;
    }
}
