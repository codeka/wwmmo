package au.com.codeka.common.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.Message;
import au.com.codeka.common.protobuf.ScoutReport;
import au.com.codeka.common.protobuf.Star;
import okio.ByteString;

import java.io.IOException;

public abstract class BaseScoutReport {
    protected String mKey;
    protected DateTime mReportDate;
    protected String mEmpireKey;
    protected String mStarKey;
    protected BaseStar mStarSnapshot;

    protected abstract BaseStar createStar(Star pb);

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

    public void fromProtocolBuffer(ScoutReport pb) {
        mKey = pb.key;
        mReportDate = new DateTime(pb.date * 1000, DateTimeZone.UTC);
        mEmpireKey = pb.empire_key;
        mStarKey = pb.star_key;
        Star star_pb;
        try {
            star_pb = Message.wire.parseFrom(pb.star_pb.toByteArray(), Star.class);
            mStarSnapshot = createStar(star_pb);
        } catch (IOException e) {
            // Ignore.
        }
    }

    public void toProtocolBuffer(ScoutReport.Builder pb) {
        pb.key = mKey;
        pb.date = mReportDate.getMillis() / 1000;
        pb.empire_key = mEmpireKey;
        pb.star_key = mStarKey;

        Star.Builder star_pb = new Star.Builder();
        mStarSnapshot.toProtocolBuffer(star_pb, false);
        pb.star_pb = ByteString.of(star_pb.build().toByteArray());
    }
}
