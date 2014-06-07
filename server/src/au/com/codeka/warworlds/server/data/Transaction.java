package au.com.codeka.warworlds.server.data;

import java.sql.Connection;
import java.sql.SQLException;

public class Transaction implements AutoCloseable {
    private Connection mConnection;
    private boolean mWasCommitted;

    public Transaction(Connection conn) throws SQLException {
        mConnection = conn;
        mConnection.setAutoCommit(false);
    }

    public SqlStmt prepare(String sql) throws SQLException {
        return new SqlStmt(mConnection, sql, mConnection.prepareStatement(sql), false);
    }

    public SqlStmt prepare(String sql, int autoGenerateKeys) throws SQLException {
        return new SqlStmt(mConnection, sql, mConnection.prepareStatement(sql, autoGenerateKeys), false);
    }

    public void commit() throws SQLException {
        mConnection.commit();
        mWasCommitted = true;
    }

    public void rollback() throws SQLException {
        mConnection.rollback();
        mWasCommitted = true;
    }

    @Override
    public void close() throws Exception {
        if (!mWasCommitted) {
            mConnection.rollback();
        }
        mConnection.setAutoCommit(true);
        mConnection.close();
    }
}
