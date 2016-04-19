package au.com.codeka.common.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.Messages;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

public abstract class BaseScoutReport {
    protected String mKey;
    protected DateTime mReportDate;
    protected String mEmpireKey;
    protected String mStarKey;
    protected BaseStar mStarSnapshot;

    protected abstract BaseStar createStar(Messages.Star pb);

    public String getKey() {
        return mKey;
    }
    public String getEmpireKey() {
        return mEmpireKey;
    }
    public String getStarKey() {
        return mStarKey;
    }
    public DateTime getReportDate() {
        return mReportDate;
    }
    public BaseStar getStarSnapshot() {
        return mStarSnapshot;
    }

    public void fromProtocolBuffer(Messages.ScoutReport pb) {
        mKey = pb.getKey();
        mReportDate = new DateTime(pb.getDate() * 1000, DateTimeZone.UTC);
        mEmpireKey = pb.getEmpireKey();
        mStarKey = pb.getStarKey();
        ByteString starSnapshotSerialized = pb.getStarPb();
        Messages.Star star_pb;
        try {
            star_pb = Messages.Star.parseFrom(starSnapshotSerialized);
            mStarSnapshot = createStar(star_pb);
        } catch (InvalidProtocolBufferException e) {
            // TODO: handle errors?
        }
    }

    public void toProtocolBuffer(Messages.ScoutReport.Builder pb) {
        if (mKey != null) {
            pb.setKey(mKey);
        }
        pb.setDate(mReportDate.getMillis() / 1000);
        pb.setEmpireKey(mEmpireKey);
        pb.setStarKey(mStarKey);

        Messages.Star.Builder star_pb = Messages.Star.newBuilder();
        mStarSnapshot.toProtocolBuffer(star_pb, false);
        pb.setStarPb(ByteString.copyFrom(star_pb.build().toByteArray()));
    }
}
