package au.com.codeka.warworlds.server.model;

import java.sql.SQLException;

import org.joda.time.DateTime;

import au.com.codeka.common.model.BaseScoutReport;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.data.SqlResult;

import com.google.protobuf.InvalidProtocolBufferException;

public class ScoutReport extends BaseScoutReport {
    private int mID;

    public ScoutReport() {
    }
    public ScoutReport(int starID, int empireID, Star star) {
        mStarKey = Integer.toString(starID);
        mEmpireKey = Integer.toString(empireID);
        mReportDate = DateTime.now();
        mStarSnapshot = star;
    }
    public ScoutReport(SqlResult res) throws SQLException {
        try {
            Messages.ScoutReport scout_report_pb = Messages.ScoutReport.parseFrom(
                    res.getBytes("report"));
            fromProtocolBuffer(scout_report_pb);
        } catch(InvalidProtocolBufferException e) {
            // ignore
        }

        mID = res.getInt("id");
        mKey = Integer.toString(mID);
    }

    public int getID() {
        return mID;
    }
    public void setID(int id) {
        mID = id;
        mKey = Integer.toString(id);
    }

    @Override
    protected BaseStar createStar(Messages.Star pb) {
        Star star = new Star();
        if (pb != null) {
            star.fromProtocolBuffer(pb);
        }
        return star;
    }
}
