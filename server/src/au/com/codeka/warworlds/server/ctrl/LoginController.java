package au.com.codeka.warworlds.server.ctrl;

import java.security.SecureRandom;
import java.util.Locale;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.model.BaseEmpire;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.Session;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;

public class LoginController {
    private final Logger log = LoggerFactory.getLogger(LoginController.class);

    private static char[] SESSION_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    private static String[] ADMIN_USERS = {"dean@codeka.com.au"};

    // list of users that are allowed to impersonate others
    private static String[] IMPERSONATE_USERS = {"dean@codeka.com.au", "warworldstest1@gmail.com",
                                                 "warworldstest2@gmail.com", "warworldstest3@gmail.com",
                                                 "warworldstest4@gmail.com"};

    /**
     * Generates a cookie, assuming we've just finihed authenticating as the given email.
     */
    public String generateCookie(String emailAddr, boolean doAdminTest, String impersonateUser)
                                 throws RequestException {
        // generate a random string for the session cookie
        SecureRandom rand = new SecureRandom();
        StringBuilder cookie = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            cookie.append(SESSION_CHARS[rand.nextInt(SESSION_CHARS.length)]);
        }

        boolean isAdmin = false;
        if (doAdminTest) {
            for (String adminUserEmail : ADMIN_USERS) {
                if (adminUserEmail.equals(emailAddr.toLowerCase())) {
                    isAdmin = true;
                }
            }
        }

        if (impersonateUser != null) {
            boolean allowed = false;
            for (String impersonator : IMPERSONATE_USERS) {
                if (impersonator.equals(emailAddr)) {
                    allowed = true;
                }
            }
            if (!allowed) {
                impersonateUser = null;
            }
        }

        int empireID = 0;
        int allianceID = 0;
        boolean banned = false;
        try (SqlStmt stmt = DB.prepare("SELECT id, alliance_id, state FROM empires WHERE user_email = ?")) {
            if (impersonateUser != null) {
                stmt.setString(1, impersonateUser);
            } else {
                stmt.setString(1, emailAddr);
            }
            SqlResult res = stmt.select();
            if (res.next()) {
                empireID = res.getInt(1);
                if (res.getInt(2) == null) {
                    allianceID = 0;
                } else {
                    allianceID = res.getInt(2);
                }
                BaseEmpire.State state = BaseEmpire.State.fromNumber(res.getInt(3));
                if (state == BaseEmpire.State.BANNED) {
                    banned = true;
                }
            }
        } catch (Exception e) {
            throw new RequestException(e);
        }

        if (banned) {
            throw new RequestException(403, Messages.GenericError.ErrorCode.EmpireBanned, "You have been banned for misconduct.");
        }

        Session session = new Session(cookie.toString(), emailAddr, DateTime.now(),
                empireID, allianceID, isAdmin);
        new SessionController().saveSession(session);

        log.info(String.format(Locale.ENGLISH, "Authenticated: email=%s cookie=%s", emailAddr, cookie));
        return cookie.toString();
    }

}
