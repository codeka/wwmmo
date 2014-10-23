package au.com.codeka.warworlds.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.eclipse.jdt.annotation.Nullable;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import au.com.codeka.common.Log;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.Util;
import au.com.codeka.warworlds.model.Realm;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

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
     * @param activity
     *            The activity we're current attached to, this can be null in
     *            which case we will not prompt for authorization.
     * @param accountName
     *            The name of the account we're authenticating as.
     * @return A cookie we can use in subsequent calls to the server.
     * @throws ApiException
     */
    public boolean authenticate(@Nullable Activity activity, Realm realm)
            throws ApiException {
        // make sure we don't try to authenticate WHILE WE'RE AUTHENTICATING...
        if (mAuthenticating) {
            return true;
        }
        mAuthenticating = true;

        SharedPreferences prefs = Util.getSharedPreferences();
        final String accountName = prefs.getString("AccountName", null);
        if (accountName == null) {
            mAuthenticating = false;
            throw new ApiException("No account has been selected yet!");
        }

        Context context = App.i;
        log.info("(re-)authenticating \"%s\" to realm %s...", accountName,
                realm.getDisplayName());
        String cookie = null;

        try {
            final String scope = "oauth2:email";
            String authToken = GoogleAuthUtil.getToken(context, accountName, scope);
            if (authToken == null) {
                throw new ApiException("Error getting auth token.");
            }
            cookie = getCookie(authToken, realm);
            log.info("Authentication successful.");
        } catch (UserRecoverableAuthException e) {
            // If it's a 'recoverable' exception, we need to start the given
            // intent and then try
            // again.
            if (activity == null) {
                throw new ApiException("Cannot retry, no activity given.", e);
            }
            Intent intent = e.getIntent();
            activity.startActivityForResult(intent,
                    BaseActivity.AUTH_RECOVERY_REQUEST);
            log.warning("Got UserRecoverableAuthException, TODO");
        } catch (GoogleAuthException e) {
            throw new ApiException(e);
        } catch (IOException e) {
            throw new ApiException(e);
        } finally {
            mAuthenticating = false;
        }
        mAuthCookie = cookie;
        return true;
    }

    /**
     * Makes a request to the server to get a cookie which we can send with each
     * subsequent request.
     */
    public String getCookie(String authToken, Realm realm) throws ApiException {
        String url = realm.getBaseUrl().resolve("login?authToken=" + authToken)
                .toString();

        String impersonate = Util.getProperties().getProperty(
                "user.on_behalf_of", null);
        if (impersonate != null) {
            url += "&impersonate=" + impersonate;
        }

        RequestManager.ResultWrapper resp = null;
        try {
            resp = RequestManager.request("GET", url);
            int statusCode = resp.getResponse().getStatusLine().getStatusCode();
            if (statusCode != 200) {
                log.warning("Authentication failure: %s", resp.getResponse()
                        .getStatusLine());
                ApiException.checkResponse(resp.getResponse());
            }

            String cookie = null;
            HttpEntity entity = resp.getResponse().getEntity();
            if (entity != null) {
                try {
                    InputStream ins = entity.getContent();
                    cookie = new BufferedReader(new InputStreamReader(ins,
                            "utf-8")).readLine();
                } catch (IllegalStateException e) {
                } catch (Exception e) {
                    log.warning("Authentication failure, could got get response body.");
                    return null;
                }
            }

            if (cookie == null) {
                return null;
            }
            return "SESSION=" + cookie;
        } finally {
            if (resp != null) {
                resp.close();
            }
        }
    }
}
