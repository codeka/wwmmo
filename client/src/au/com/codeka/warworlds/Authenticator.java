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
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.AppEngineAuthenticator;
import au.com.codeka.warworlds.api.DefaultAuthenticator;
import au.com.codeka.warworlds.api.RequestManager;
import au.com.codeka.warworlds.api.RequestRetryException;
import au.com.codeka.warworlds.model.Realm;
import au.com.codeka.warworlds.model.RealmManager;

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
        sAccountManager = AccountManager.get(context);

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
            public void onResponseReceived(BasicHttpRequest request,
                                           BasicHttpResponse response)
                    throws RequestRetryException {
                // if we get a 403 (and not on a 'login' URL), it means we need to re-authenticate,
                // so do that
                if (response.getStatusLine().getStatusCode() == 403 &&
                    request.getRequestLine().getUri().indexOf("login") < 0) {
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
                    throw new RequestRetryException();
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
     * @return The authCookie we can use in subsequent calls to the server.
     */
    public static String authenticate(Activity activity, String accountName) {
        Realm realm = RealmManager.i.getRealm();
        log.info("(re-)authenticating \""+accountName+"\" to realm "+realm.getDisplayName()+"...");

        Account[] accts = sAccountManager.getAccountsByType("com.google");
        for (Account acct : accts) {
            final Account account = acct;
            if (account.name.equals(accountName)) {
                if (realm.getAuthentciationMethod() == Realm.AuthenticationMethod.Default) {
                    log.info("Account found, fetching authentication token (authmethod = default)");

                    final String scope = "oauth2:https://www.googleapis.com/auth/userinfo.email";
                    String authToken = getAuthToken(account, activity, scope);
                    sAccountManager.invalidateAuthToken(account.type, authToken);
                    authToken = getAuthToken(account, activity, scope);
                    return DefaultAuthenticator.authenticate(authToken, realm);
                } else if (realm.getAuthentciationMethod() == Realm.AuthenticationMethod.LocalAppEngine) {
                    log.info("Account found, setting up with debug auth cookie.");
                    // Use a fake cookie for the dev mode app engine server. The cookie has the
                    // form email:isAdmin:userId (we set the userId to be the same as the email)
                    return "dev_appserver_login="+accountName+":false:"+accountName;
                } else if (realm.getAuthentciationMethod() == Realm.AuthenticationMethod.AppEngine) {
                    log.info("Account found, fetching authentication token (authmethod = AppEngine)");

                    // Get the auth token from the AccountManager and convert it into a cookie 
                    // that's usable by App Engine
                    String authToken = getAuthToken(account, activity, "ah");
                    sAccountManager.invalidateAuthToken(account.type, authToken);
                    authToken = getAuthToken(account, activity, "ah");
                    return AppEngineAuthenticator.authenticate(authToken);
                }
            }
        }

        return null; // no account found!
    }

    private static String getAuthToken(Account account, Activity activity, String scope) {
        if (activity != null) {
            AccountManagerFuture<Bundle>future = sAccountManager.getAuthToken(
                    account, scope, null, activity, null, null);
            return getAuthToken(future);
        } else {
            return getAuthTokenNoActivity(account);
        }
    }

    @SuppressLint("NewApi") // getAuthToken for >= ICE_CREAM_SANDWICH
    @SuppressWarnings("deprecation") // getAuthToken for < ICE_CREAM_SANDWICH
    private static String getAuthTokenNoActivity(Account account) {
        // this version will notify the user of failures, but won't pop up the 
        // authentication page. Useful when running in the background.
        AccountManagerFuture<Bundle> future;

        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            future = sAccountManager.getAuthToken(account, "ah", false,
                                                  null, null);
        } else {
            future = sAccountManager.getAuthToken(account, "ah", null,
                                                  false, null, null);
        }
        return getAuthToken(future);
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
