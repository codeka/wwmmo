package au.com.codeka.warworlds.server.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;

import org.joda.time.DateTime;

/**
 * This is a wrapper around a \c PreparedStatement, for ease-of-use.
 */
public class SqlStmt implements AutoCloseable {
    private static Calendar sUTC;

    static {
        sUTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    }

    private Connection mConn;
    private PreparedStatement mStmt;

    public SqlStmt(Connection conn, PreparedStatement stmt) {
        mConn = conn;
        mStmt = stmt;
    }

    public void setInt(int position, int value) throws SQLException {
        mStmt.setInt(position, value);
    }
    public void setString(int position, String value) throws SQLException {
        mStmt.setString(position, value);
    }
    public void setDateTime(int position, DateTime value) throws SQLException {
        mStmt.setTimestamp(position, new Timestamp(value.getMillis()), sUTC);
    }

    /**
     * Execute an 'update' query. That is, anything but "SELECT".
     */
    public int update() throws SQLException {
        return mStmt.executeUpdate();
    }

    @Override
    public void close() throws Exception {
        mStmt.close();
        mConn.close();
    }
}
