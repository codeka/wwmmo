package au.com.codeka.warworlds.server;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.joda.time.DateTime;

/**
 * Represents details about the current session, such as your username, empire_id and so on.
 * @author dean
 *
 */
public class Session {
    private String mCookie;
    private String mActualEmail;
    private String mEmail;
    private DateTime mLoginTime;
    private int mEmpireID;
    private int mAllianceID;
    private boolean mIsAdmin;

    public Session() {
    }
    public Session(ResultSet rs) throws SQLException {
        mCookie = rs.getString("session_cookie");
        mActualEmail = rs.getString("user_email");
        mEmail = mActualEmail;
        mLoginTime = new DateTime(rs.getTimestamp("login_time").getTime());
        mEmpireID = rs.getInt("empire_id");
        mAllianceID = rs.getInt("alliance_id");
        mIsAdmin = rs.getInt("is_admin") == 1;
    }

    public String getCookie() {
        return mCookie;
    }
    public String getEmail() {
        return mEmail;
    }
    public String getActualEmail() {
        return mActualEmail;
    }
    public DateTime getLoginTime() {
        return mLoginTime;
    }
    public int getEmpireID() {
        return mEmpireID;
    }
    public int getAllianceID() {
        return mAllianceID;
    }
    public boolean isAdmin() {
        return mIsAdmin;
    }

    public void impersonate(String email) {
        mEmail = email;
    }
}
