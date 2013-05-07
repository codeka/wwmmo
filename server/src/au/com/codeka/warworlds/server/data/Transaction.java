package au.com.codeka.warworlds.server.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Transaction implements AutoCloseable {
    private final Logger log = LoggerFactory.getLogger(Transaction.class);
    private Connection mConnection;
    private ArrayList<SqlStmt> mStatements;

    public Transaction(Connection conn) throws SQLException {
        mConnection = conn;
        mConnection.setAutoCommit(false);
        mStatements = new ArrayList<SqlStmt>();
    }

    public SqlStmt prepare(String sql) throws SQLException {
        return new SqlStmt(mConnection, sql, mConnection.prepareStatement(sql), false);
    }

    public SqlStmt prepare(String sql, int autoGenerateKeys) throws SQLException {
        return new SqlStmt(mConnection, sql, mConnection.prepareStatement(sql, autoGenerateKeys), false);
    }

    public void commit() throws SQLException {
        mConnection.commit();
    }

    public void rollback() throws SQLException {
        mConnection.rollback();
    }

    @Override
    public void close() throws Exception {
        for (SqlStmt stmt : mStatements) {
            stmt.close();
        }
        mConnection.setAutoCommit(true);
        mConnection.close();
    }
}
