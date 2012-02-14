package au.com.codeka.warworlds;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import au.com.codeka.warworlds.api.ApiAuthenticator;

/**
 * This class works in concert with \c ApiAuthenticator to authenticate the current
 * user with App Engine.
 */
public class Authenticator {
    private static Logger log = LoggerFactory.getLogger(Authenticator.class);

    /**
     * This interface should be implemented when you're interested in knowing when
     * the \c authenticate method completes.
     */
    public interface AuthenticationCompleteCallback {
        void onAuthenticationComplete(String authCookie);
    }

    /**
     * Authenticates the given user with App Engine.
     * 
     * Note: You CANNOT call this method on the main thread. Do it in a background thread, because
     * it can (will) block on network calls.
     * 
     * @param context
     * @param activity
     * @param accountName
     * @return The authCookie we can use in subsequent calls to App Engine.
     */
    public static String authenticate(Context context, Activity activity, String accountName) {
        final AccountManager mgr = AccountManager.get(context);

        log.info("(re-)authenticating \""+accountName+"\"...");

        Account[] accts = mgr.getAccountsByType("com.google");
        for (Account acct : accts) {
            final Account account = acct;
            if (account.name.equals(accountName)) {
                if (Util.isDebug()) {
                    log.info("Account found, setting up with debug auth cookie.");
                    // Use a fake cookie for the dev mode app engine server. The cookie has the
                    // form email:isAdmin:userId (we set the userId to be the same as the email)
                    String authCookie = "dev_appserver_login="+accountName+":false:"+accountName;

                    return authCookie;
                } else {
                    log.info("Account found, fetching authentication token...");

                    // Get the auth token from the AccountManager and convert it into a cookie 
                    // that's usable by App Engine
                    AccountManagerFuture<Bundle>future = mgr.getAuthToken(account, "ah", null,
                            activity, null, null);
                    String authToken = getAuthToken(future);

                    // Ensure the token is not expired by invalidating
                    // it and obtaining a new one
                    mgr.invalidateAuthToken(account.type, authToken);
                    future = mgr.getAuthToken(account, "ah", null, activity, null, null);
                    authToken = getAuthToken(future);

                    // Convert the token into a cookie for future use
                    String authCookie = ApiAuthenticator.authenticate(authToken);

                    return authCookie;
                }
            }
        }

        return null; // no account found!
    }

    /**
     * Gets the auth token from the given \c AccountmanagerFuture.
     */
    private static String getAuthToken(AccountManagerFuture<Bundle> future) {
        try {
            Bundle authTokenBundle = future.getResult();
            String authToken = authTokenBundle.get(AccountManager.KEY_AUTHTOKEN).toString();
            return authToken;
        } catch (Exception e) {
            log.warn("Got Exception " + e);
            return null;
        }
    }
}
