package au.com.codeka.warworlds.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.squareup.okhttp.Response;

import java.io.IOException;

import javax.annotation.Nullable;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.Util;
import au.com.codeka.warworlds.model.Realm;

public class Authenticator {
  private static final Log log = new Log("Authenticator");
  private String authCookie;
  private boolean authenticating;

  public boolean isAuthenticated() {
    return (authCookie != null);
  }

  public void logout() {
    authCookie = null;
  }

  public String getAuthCookie() {
    return authCookie;
  }

  /**
   * Authenticates the given user with the server.
   *
   * @param activity The activity we're current attached to, this can be null in which case we will
   *                 not prompt for authorization.
   * @param realm The {@link Realm} we're connecting to.
   * @return A cookie we can use in subsequent calls to the server.
   * @throws ApiException
   */
  public boolean authenticate(@Nullable Activity activity, Realm realm) throws ApiException {
    // make sure we don't try to authenticate WHILE WE'RE AUTHENTICATING...
    if (authenticating) {
      return true;
    }
    authenticating = true;

    SharedPreferences prefs = Util.getSharedPreferences();
    final String accountName = prefs.getString("AccountName", null);
    if (accountName == null) {
      authenticating = false;
      throw new ApiException("No account has been selected yet!");
    }

    Context context = App.i;
    log.info("(re-)authenticating \"%s\" to realm %s...", accountName, realm.getDisplayName());
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
      // If it's a 'recoverable' exception, we need to start the given intent and then try again.
      if (activity == null) {
        throw new ApiException("Cannot retry, no activity given.", e);
      }
      Intent intent = e.getIntent();
      activity.startActivityForResult(intent, BaseActivity.AUTH_RECOVERY_REQUEST);
      log.warning("Got UserRecoverableAuthException, TODO");
    } catch (GoogleAuthException e) {
      throw new ApiException(e);
    } catch (IOException e) {
      throw new ApiException(e);
    } finally {
      authenticating = false;
    }
    authCookie = cookie;
    return true;
  }

  /**
   * Makes a request to the server to get a cookie which we can send with each subsequent request.
   */
  public String getCookie(String authToken, Realm realm) throws ApiException {
    String url = realm.getBaseUrl().resolve("login?authToken=" + authToken).toString();

    String impersonate = Util.getProperties().getProperty("user.on_behalf_of", null);
    if (impersonate != null) {
      url += "&impersonate=" + impersonate;
    }

    ApiRequest request = new ApiRequest(url, ApiRequest.GET, null, null);
    try {
      Response resp = RequestManager.i.callRequest(request);
      if (!resp.isSuccessful()) {
        log.warning("Authentication failure: %s", resp.message());
        ApiException.checkResponse(resp);
      }

      String cookie = resp.body().string();
      if (cookie == null) {
        return null;
      }
      return "SESSION=" + cookie;
    } catch (IOException e) {
      throw new ApiException(e);
    }
  }
}
