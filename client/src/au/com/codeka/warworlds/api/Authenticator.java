package au.com.codeka.warworlds.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.Util;
import au.com.codeka.warworlds.model.Realm;

public class Authenticator {
    private Logger log = LoggerFactory.getLogger(Authenticator.class);
    private String mAuthCookie;
    private boolean mAuthenticating;

    public boolean isAuthenticated() {
        return (mAuthCookie != null);
    }

    public void logout() {
        mAuthCookie = null;
    }

    public String getAuthCookie() {
        return mAuthCookie;
    }

    /**
     * Authenticates the given user with the server.
     * 
     * @param activity The activity we're current attached to, this can be null in which case we
     *        will not prompt for authorization.
     * @param accountName The name of the account we're authenticating as.
     * @return A cookie we can use in subsequent calls to the server.
     * @throws ApiException 
     */
    public void authenticate(Activity activity, Realm realm) throws ApiException {
        // make sure we don't try to authenticate WHILE WE'RE AUTHENTICATING...
        if (mAuthenticating) {
            return;
        }
        mAuthenticating = true;

        SharedPreferences prefs = Util.getSharedPreferences();
        final String accountName = prefs.getString("AccountName", null);
        if (accountName == null) {
            mAuthenticating = false;
            throw new ApiException("No account has been selected yet!");
        }

        AccountManager accountManager = AccountManager.get(activity == null ? App.i : activity);
        log.info("(re-)authenticating \""+accountName+"\" to realm "+realm.getDisplayName()+"...");
        String cookie = null;

        try {
            Account[] accts = accountManager.getAccountsByType("com.google");
            for (Account acct : accts) {
                final Account account = acct;
                if (account.name.equals(accountName)) {
                    if (realm.getAuthentciationMethod() == Realm.AuthenticationMethod.Default) {
                        log.info("Account found, fetching authentication token (authmethod = default)");

                        final String scope = "oauth2:email";
                        String authToken = getAuthToken(accountManager, account, activity, scope);
                        accountManager.invalidateAuthToken(account.type, authToken);
                        authToken = getAuthToken(accountManager, account, activity, scope);
                        cookie = DefaultAuthenticator.authenticate(authToken, realm);
                    } else if (realm.getAuthentciationMethod() == Realm.AuthenticationMethod.LocalAppEngine) {
                        log.info("Account found, setting up with debug auth cookie.");
                        // Use a fake cookie for the dev mode app engine server. The cookie has the
                        // form email:isAdmin:userId (we set the userId to be the same as the email)
                        cookie = "dev_appserver_login="+accountName+":false:"+accountName;
                    } else if (realm.getAuthentciationMethod() == Realm.AuthenticationMethod.AppEngine) {
                        log.info("Account found, fetching authentication token (authmethod = AppEngine)");
    
                        // Get the auth token from the AccountManager and convert it into a cookie 
                        // that's usable by App Engine
                        String authToken = getAuthToken(accountManager, account, activity, "ah");
                        accountManager.invalidateAuthToken(account.type, authToken);
                        authToken = getAuthToken(accountManager, account, activity, "ah");
                        cookie = AppEngineAuthenticator.authenticate(authToken);
                    }
                }
            }
        } finally {
            mAuthenticating = false;
        }
        mAuthCookie = cookie;
    }

    private String getAuthToken(AccountManager accountManager, Account account, Activity activity, String scope) {
        if (activity != null) {
            log.info("Fetching auth token with activity");
            AccountManagerFuture<Bundle>future = accountManager.getAuthToken(
                    account, scope, new Bundle(), activity, null, null);
            return getAuthToken(future);
        } else {
            log.info("Fetching auth token withOUT activity");
            return getAuthTokenNoActivity(accountManager, account);
        }
    }

    @SuppressLint("NewApi") // getAuthToken for >= ICE_CREAM_SANDWICH
    @SuppressWarnings("deprecation") // getAuthToken for < ICE_CREAM_SANDWICH
    private String getAuthTokenNoActivity(AccountManager accountManager, Account account) {
        // this version will notify the user of failures, but won't pop up the 
        // authentication page. Useful when running in the background.
        AccountManagerFuture<Bundle> future;

        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            future = accountManager.getAuthToken(account, "ah", false,
                                                 null, null);
        } else {
            future = accountManager.getAuthToken(account, "ah", new Bundle(),
                                                 false, null, null);
        }
        return getAuthToken(future);
    }

    /**
     * Gets the auth token from the given \c AccountmanagerFuture.
     */
    private String getAuthToken(AccountManagerFuture<Bundle> future) {
        try {
            Bundle authTokenBundle = future.getResult();
            if (authTokenBundle == null || authTokenBundle.get(AccountManager.KEY_AUTHTOKEN) == null) {
                return null;
            }

            String authToken = authTokenBundle.get(AccountManager.KEY_AUTHTOKEN).toString();
            return authToken;
        } catch (Exception e) {
            log.error("Error fetching auth token", e);
            return null;
        }
    }

}
