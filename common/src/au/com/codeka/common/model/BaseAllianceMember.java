package au.com.codeka.common.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.Messages;

public class BaseAllianceMember {
    protected String mKey;
    protected String mAllianceKey;
    protected String mEmpireKey;
    protected DateTime mTimeJoined;

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

    public void fromProtocolBuffer(Messages.AllianceMember pb) {
        mKey = pb.getKey();
        mAllianceKey = pb.getAllianceKey();
        mEmpireKey = pb.getEmpireKey();
        mTimeJoined = new DateTime(pb.getTimeJoined() * 1000, DateTimeZone.UTC);
    }
}
