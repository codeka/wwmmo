package au.com.codeka.warworlds;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
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
     * @param context
     * @param activity
     * @param accountName
     * @param onComplete A callback that receives the authentication cookie that we'll want
     *        to include in every App Engine request.
     */
    public static void authenticate(Context context, final Activity activity,
            final String accountName, final AuthenticationCompleteCallback callback) {
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

                    callback.onAuthenticationComplete(authCookie);
                } else {
                    log.info("Account found, fetching authentication token...");

                    // Get the auth token from the AccountManager and convert it into a cookie 
                    // that's usable by App Engine
                    mgr.getAuthToken(account, "ah", null, activity,
                            new AccountManagerCallback<Bundle>() {
                        public void run(AccountManagerFuture<Bundle> future) {
                            String authToken = getAuthToken(future);

                            // Ensure the token is not expired by invalidating
                            // it and obtaining a new one
                            mgr.invalidateAuthToken(account.type, authToken);
                            mgr.getAuthToken(account, "ah", null, activity,
                                    new AccountManagerCallback<Bundle>() {
                                public void run(final AccountManagerFuture<Bundle> future) {
                                    final String newAuthToken = getAuthToken(future);

                                    // can't call getAuthCookie() on the main thread
                                    new AsyncTask<Void, Void, Void>() {
                                        @Override
                                        protected Void doInBackground(Void... arg0) {
                                            try {
                                                // Convert the token into a cookie for future use
                                                String authCookie = ApiAuthenticator.authenticate(
                                                        newAuthToken);

                                                callback.onAuthenticationComplete(authCookie);
                                            } catch(Exception e) {
                                                // todo?
                                            }
                                            return null;
                                        }
                                    }.execute();
                                }
                            }, null);
                        }
                    }, null);
                }
                break;
            }
        }
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
