package au.com.codeka.warworlds.api;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import au.com.codeka.common.Log;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.Util;
import au.com.codeka.warworlds.model.Realm;

public class Authenticator {
    private static final Log log = new Log("Authenticator");
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
        log.info("(re-)authenticating \"%s\" to realm %s...", accountName, realm.getDisplayName());
        String cookie = null;

        try {
            Account[] accts = accountManager.getAccountsByType("com.google");
            for (Account acct : accts) {
                final Account account = acct;
                if (account.name.equals(accountName)) {
                    log.info("Account found, fetching authentication token");

                    final String scope = "oauth2:email";
                    String authToken = getAuthToken(accountManager, account, activity, scope);
                    accountManager.invalidateAuthToken(account.type, authToken);
                    authToken = getAuthToken(accountManager, account, activity, scope);
                    cookie = getCookie(authToken, realm);
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

    /**
     * Makes a request to the server to get a cookie which we can send with each subsequent request.
     */
    public String getCookie(String authToken, Realm realm) throws ApiException {
        String url = realm.getBaseUrl().resolve("login?authToken="+authToken).toString();

        String impersonate = Util.getProperties().getProperty("user.on_behalf_of", null);
        if (impersonate != null) {
            url += "&impersonate="+impersonate;
        }

        RequestManager.ResultWrapper resp = null;
        try {
            resp = RequestManager.request("GET", url);
            int statusCode = resp.getResponse().getStatusLine().getStatusCode();
            if (statusCode != 200) {
                log.warning("Authentication failure: %s", resp.getResponse().getStatusLine());
                ApiException.checkResponse(resp.getResponse());
            }

            String cookie = null;
            HttpEntity entity = resp.getResponse().getEntity();
            if (entity != null) {
                try {
                    InputStream ins = entity.getContent();
                    cookie = new BufferedReader(new InputStreamReader(ins, "utf-8")).readLine();
                } catch (IllegalStateException e) {
                } catch (Exception e) {
                    log.warning("Authentication failure, could got get response body.");
                    return null;
                }
            }

            if (cookie == null) {
                return null;
            }
            return "SESSION="+cookie;
        } finally {
            if (resp != null) {
                resp.close();
            }
        }
    }
}
