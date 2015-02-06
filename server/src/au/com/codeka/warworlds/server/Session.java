package au.com.codeka.warworlds.server;

import java.sql.SQLException;

import org.joda.time.DateTime;

import au.com.codeka.warworlds.server.data.SqlResult;

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
    private Integer mAllianceID;
    private boolean mIsAdmin;

    public Session() {
    }
    public Session(SqlResult res) throws SQLException {
        mCookie = res.getString("session_cookie");
        mActualEmail = res.getString("user_email");
        mEmail = mActualEmail;
        mLoginTime = res.getDateTime("login_time");
        if (res.getInt("empire_id") == null) {
            mEmpireID = 0;
        } else {
            mEmpireID = res.getInt("empire_id");
        }
        mAllianceID = res.getInt("alliance_id");
        mIsAdmin = res.getInt("is_admin") == 1;
    }
    public Session(Session copy) {
        mCookie = copy.mCookie;
        mActualEmail = copy.mActualEmail;
        mEmail = mActualEmail;
        mLoginTime = copy.mLoginTime;
        mEmpireID = copy.mEmpireID;
        mAllianceID = copy.mAllianceID;
        mIsAdmin = copy.mIsAdmin;
    }
    public Session(String cookie, String email, DateTime loginTime, int empireID,
            Integer allianceID, boolean isAdmin) {
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
        if (mAllianceID == null) {
            return 0;
        }
        return mAllianceID;
    }
    public boolean isAdmin() {
        return mIsAdmin;
    }

    public Session impersonate(String email, int empireID) {
        Session newSession = new Session(this);
        newSession.mEmail = email;
        newSession.mEmpireID = empireID;
        return newSession;
    }
}
