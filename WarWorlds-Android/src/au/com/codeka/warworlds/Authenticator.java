package au.com.codeka.warworlds;


import org.apache.http.Header;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import au.com.codeka.warworlds.api.ApiAuthenticator;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.RequestManager;

/**
 * This class works in concert with \c ApiAuthenticator to authenticate the current
 * user with App Engine.
 */
public class Authenticator {
    private static Logger log = LoggerFactory.getLogger(Authenticator.class);
    private static AccountManager sAccountManager = null;
    private static int sLastRequestStatusCode = 200;

    /**
     * This interface should be implemented when you're interested in knowing when
     * the \c authenticate method completes.
     */
    public interface AuthenticationCompleteCallback {
        void onAuthenticationComplete(String authCookie);
    }

    public static void configure(Context context) {
        if (sAccountManager == null) {
            sAccountManager = AccountManager.get(context);
        }

        SharedPreferences prefs = Util.getSharedPreferences(context);
        final String accountName = prefs.getString("AccountName", null);

        RequestManager.addResponseReceivedHandler(new RequestManager.ResponseReceivedHandler() {
            private void dump(AbstractHttpMessage msg) {
                log.info("      DUMP       ");
                if (msg instanceof BasicHttpRequest) {
                    log.info(((BasicHttpRequest) msg).getRequestLine().toString());
                } else if (msg instanceof BasicHttpResponse) {
                    log.info(((BasicHttpResponse) msg).getStatusLine().toString());
                }
                for (Header h : msg.getAllHeaders()) {
                    log.info(h.getName()+": "+h.getValue());
                }
            }

            @Override
            public void onResponseReceived(BasicHttpRequest request, BasicHttpResponse response) {
                // if we get a 403, it means we need to re-authenticate, so do that
                if (response.getStatusLine().getStatusCode() == 403) {
                    dump(request);
                    dump(response);

                    if (sLastRequestStatusCode == 403) {
                        // if the last status code we received was 403, then re-authenticating
                        // again isn't going to help. This is only useful if, for example, the
                        // token has expired.
                        return;
                    }

                    log.info("403 HTTP response code received, attempting to re-authenticate.");

                    String authCookie = Authenticator.authenticate(null, accountName);
                    ApiClient.getCookies().clear();
                    ApiClient.getCookies().add(authCookie);

                    // record the fact that the last status code was 403, so we can fail on the
                    // next request if we get another 403 (no point retrying that over and over)
                    sLastRequestStatusCode = 403;

                    // throw an exception so that the RequestManager knows to try the request
                    // for a second time.
                    throw new RuntimeException(); // TODO: better exception
                }

                sLastRequestStatusCode = response.getStatusLine().getStatusCode();
            }
        });
    }

    /**
     * Authenticates the given user with App Engine.
     * 
     * Note: You CANNOT call this method on the main thread. Do it in a background thread, because
     * it can (will) block on network calls.
     * 
     * @param activity
     * @param accountName
     * @return The authCookie we can use in subsequent calls to App Engine.
     */
    public static String authenticate(Activity activity, String accountName) {
        log.info("(re-)authenticating \""+accountName+"\"...");

        Account[] accts = sAccountManager.getAccountsByType("com.google");
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
                    String authToken = getAuthToken(account, activity);

                    // Ensure the token is not expired by invalidating
                    // it and obtaining a new one
                    sAccountManager.invalidateAuthToken(account.type, authToken);
                    authToken = getAuthToken(account, activity);

                    // Convert the token into a cookie for future use
                    String authCookie = ApiAuthenticator.authenticate(authToken);

                    return authCookie;
                }
            }
        }

        return null; // no account found!
    }

    private static String getAuthToken(Account account, Activity activity) {
        if (activity != null) {
            AccountManagerFuture<Bundle>future = sAccountManager.getAuthToken(
                    account, "ah", null, activity, null, null);
            return getAuthToken(future);
        } else {
            // this version will notify the user of failures, but won't pop up the 
            // authentication page. Useful when running in the background.
            AccountManagerFuture<Bundle>future = sAccountManager.getAuthToken(
                    account, "ah", true, null, null);
            return getAuthToken(future);
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
