package au.com.codeka.warworlds.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.Messages;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

public class ScoutReport {
    private String mKey;
    private DateTime mReportDate;
    private String mEmpireKey;
    private String mStarKey;
    private Star mStarSnapshot;

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
    public Star getStarSnapshot() {
        return mStarSnapshot;
    }

    public static ScoutReport fromProtocolBuffer(Messages.ScoutReport pb) {
        ScoutReport scoutReport = new ScoutReport();
        scoutReport.mKey = pb.getKey();
        scoutReport.mReportDate = new DateTime(pb.getDate() * 1000, DateTimeZone.UTC);
        scoutReport.mEmpireKey = pb.getEmpireKey();
        scoutReport.mStarKey = pb.getStarKey();
        ByteString starSnapshotSerialized = pb.getStarPb();
        Messages.Star starPb;
        try {
            starPb = Messages.Star.parseFrom(starSnapshotSerialized);
            scoutReport.mStarSnapshot = new Star();
            scoutReport.mStarSnapshot.fromProtocolBuffer(starPb);
        } catch (InvalidProtocolBufferException e) {
            // TODO: handle errors?
        }
        return scoutReport;
    }
}
