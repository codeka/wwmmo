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
    private boolean mAllowInlineNotifications;

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
        mAllowInlineNotifications = (rs.getInt("inline_notifications") == 1);
    }
    public Session(Session copy) {
        mCookie = copy.mCookie;
        mActualEmail = copy.mActualEmail;
        mEmail = mActualEmail;
        mLoginTime = copy.mLoginTime;
        mEmpireID = copy.mEmpireID;
        mAllianceID = copy.mAllianceID;
        mIsAdmin = copy.mIsAdmin;
        mAllowInlineNotifications = copy.mAllowInlineNotifications;
    }
    public Session(String cookie, String email, DateTime loginTime, int empireID, int allianceID, boolean isAdmin) {
        mCookie = cookie;
        mActualEmail = email;
        mEmail = mActualEmail;
        mLoginTime = loginTime;
        mEmpireID = empireID;
        mAllianceID = allianceID;
        mIsAdmin = isAdmin;
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
    public void setEmpireID(int empireID) {
        mEmpireID = empireID;
    }
    public int getAllianceID() {
        return mAllianceID;
    }
    public boolean isAdmin() {
        return mIsAdmin;
    }
    public boolean allowInlineNotifications() {
        return mAllowInlineNotifications;
    }

    public void setAllowInlineNotifications(boolean allow) {
        mAllowInlineNotifications = allow;
    }

    public Session impersonate(String email, int empireID) {
        Session newSession = new Session(this);
        newSession.mEmail = email;
        newSession.mEmpireID = empireID;
        return newSession;
    }
}
