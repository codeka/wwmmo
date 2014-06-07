package au.com.codeka.warworlds.server.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/** Wrapper around a {@link ResultSet}. */
public class SqlResult {
    private ResultSet mResultSet;

    SqlResult(ResultSet rs) {
        mResultSet = rs;
    }

    void close() throws SQLException {
        mResultSet.close();
    }

    public boolean next() throws SQLException {
        return mResultSet.next();
    }

    public Integer getInt(int position) throws SQLException {
        int value = mResultSet.getInt(position);
        if (mResultSet.wasNull()) {
            return null;
        }
        return value;
    }

    public Integer getInt(String columnName) throws SQLException {
        int value = mResultSet.getInt(columnName);
        if (mResultSet.wasNull()) {
            return null;
        }
        return value;
    }

    public Long getLong(int position) throws SQLException {
        long value = mResultSet.getLong(position);
        if (mResultSet.wasNull()) {
            return null;
        }
        return value;
    }

    public Long getLong(String columnName) throws SQLException {
        long value = mResultSet.getLong(columnName);
        if (mResultSet.wasNull()) {
            return null;
        }
        return value;
    }

    public Float getFloat(int position) throws SQLException {
        float value = mResultSet.getFloat(position);
        if (mResultSet.wasNull()) {
            return null;
        }
        return value;
    }

    public Float getFloat(String columnName) throws SQLException {
        float value = mResultSet.getFloat(columnName);
        if (mResultSet.wasNull()) {
            return null;
        }
        return value;
    }

    public Double getDouble(int position) throws SQLException {
        double value = mResultSet.getDouble(position);
        if (mResultSet.wasNull()) {
            return null;
        }
        return value;
    }

    public Double getDouble(String columnName) throws SQLException {
        double value = mResultSet.getDouble(columnName);
        if (mResultSet.wasNull()) {
            return null;
        }
        return value;
    }

    public String getString(int position) throws SQLException {
        String value = mResultSet.getString(position);
        if (mResultSet.wasNull()) {
            return null;
        }
        return value;
    }

    public String getString(String columnName) throws SQLException {
        String value = mResultSet.getString(columnName);
        if (mResultSet.wasNull()) {
            return null;
        }
        return value;
    }

    public byte[] getBytes(int position) throws SQLException {
        byte[] value = mResultSet.getBytes(position);
        if (mResultSet.wasNull()) {
            return null;
        }
        return value;
    }

    public byte[] getBytes(String columnName) throws SQLException {
        byte[] value = mResultSet.getBytes(columnName);
        if (mResultSet.wasNull()) {
            return null;
        }
        return value;
    }

    public DateTime getDateTime(int position) throws SQLException {
        Timestamp ts = mResultSet.getTimestamp(position);
        if (mResultSet.wasNull()) {
            return null;
        }
        return new DateTime(ts.getTime(), DateTimeZone.UTC);
    }

    public DateTime getDateTime(String columnName) throws SQLException {
        Timestamp ts = mResultSet.getTimestamp(columnName);
        if (mResultSet.wasNull()) {
            return null;
        }
        return new DateTime(ts.getTime(), DateTimeZone.UTC);
    }
}
