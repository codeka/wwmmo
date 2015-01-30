package au.com.codeka.warworlds.api;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.RealmContext;
import au.com.codeka.warworlds.Util;
import au.com.codeka.warworlds.eventbus.EventBus;
import au.com.codeka.warworlds.model.Realm;

/**
 * Provides the low-level interface for making requests to the API.
 */
public class RequestManager {
  private static final Log log = new Log("RequestManager");
  private static RequestManager i = new RequestManager();
  private boolean DBG = false;

  public static final EventBus eventBus = new EventBus();

  /** If {@code true}, the user we will attempt to impersonate in all our requests. */
  private String impersonateUser;

  /**
   * The last status code we got from the server, don't try to re-authenticate if we get two 403's
   * in a row, for example.
   */
  private int lastRequestStatusCode = 200;

  /** Start impersonating the given user. */
  public void impersonate(String user) {
    impersonateUser = user;
  }

  /**
   * Enqueues the given request to send to the server.
   */
  public void sendRequest(ApiRequest apiRequest) throws ApiException {

  }


  /**
   * Performs a request with the given method to the given URL. The URL is assumed to be
   * relative to the \c baseUri that was passed in to \c configure().
   *
   * @param method       The HTTP method (GET, POST, etc)
   * @param url          The URL, relative to the \c baseUri we were configured with.
   * @param extraHeaders a mapping of additional headers to include in the request (e.g.
   *                     cookies, etc)
   * @return A \c ResultWrapper representing the server's response (if any)
   */
  public static ResultWrapper request(String method, String url,
      Map<String, List<String>> extraHeaders, HttpEntity body) throws ApiException {
    Connection conn = null;

    ConnectionPool cp = getConnectionPool();
    Realm realm = RealmContext.i.getCurrentRealm();
    if (cp == null || realm == null) {
      throw new ApiException("Not yet configured, cannot execute " + method + " " + url);
    }

    if (!realm.getAuthenticator().isAuthenticated()) {
      realm.getAuthenticator().authenticate(null, realm);
    }

    URI uri = realm.getBaseUrl().resolve(url);
    if (sVerboseLog) {
      log.debug("Requesting: %s", uri);
    }

    for (int numAttempts = 0; ; numAttempts++) {
      try {
        // Note: we only allow connections from the pool on the first attempt, if
        // requests fail, we force creating a new connection
        conn = cp.getConnection(numAttempts > 0);

        eventBus.publish(getCurrentState());

        String requestUrl = uri.getPath();
        if (uri.getQuery() != null && uri.getQuery() != "") {
          requestUrl += "?" + uri.getQuery();
        }
        if (sImpersonateUser != null) {
          if (requestUrl.indexOf("?") > 0) {
            requestUrl += "&";
          } else {
            requestUrl += "?";
          }
          requestUrl += "on_behalf_of=" + sImpersonateUser;
        }
        if (sVerboseLog) {
          log.debug("> %s %s", method, requestUrl);
        }

        BasicHttpRequest request;
        if (body != null) {
          BasicHttpEntityEnclosingRequest beer =
              new BasicHttpEntityEnclosingRequest(method, requestUrl);
          beer.setEntity(body);
          request = beer;
        } else {
          request = new BasicHttpRequest(method, requestUrl);
        }

        String host = uri.getHost();
        if (uri.getPort() > 0 && ((uri.getScheme().equals("http") && uri.getPort() != 80) || (
            uri.getScheme().equals("https") && uri.getPort() != 443))) {
          host += ":" + uri.getPort();
        }
        request.addHeader("Host", host);

        request.addHeader("User-Agent", "wwmmo/" + Util.getVersion());

        if (extraHeaders != null) {
          for (String headerName : extraHeaders.keySet()) {
            for (String headerValue : extraHeaders.get(headerName)) {
              request.addHeader(headerName, headerValue);
            }
          }
        }
        if (realm.getAuthenticator().isAuthenticated()) {
          String cookie = realm.getAuthenticator().getAuthCookie();
          if (sVerboseLog) {
            log.debug("Adding session cookie: %s", cookie);
          }
          request.addHeader("Cookie", cookie);
        }
        if (body != null) {
          request.addHeader(body.getContentType());
          request.addHeader("Content-Length", Long.toString(body.getContentLength()));
        } else if (method.equalsIgnoreCase("PUT") || method.equalsIgnoreCase("POST")) {
          request.addHeader("Content-Length", "0");
        }

        BasicHttpResponse response = conn.sendRequest(request, body);
        if (sVerboseLog) {
          log.debug("< %s", response.getStatusLine());
        }
        checkForAuthenticationError(request, response);

        return new ResultWrapper(conn, response);
      } catch (Exception e) {
        if (canRetry(e) && numAttempts == 0) {
          if (sVerboseLog) {
            log.warning("Got retryable exception making request to: %s", url, e);
          }

          // Note: the connection doesn't go back in the pool, and we'll close this
          // one, it's probably no good anyway...
          if (conn != null) {
            conn.close();
            cp.returnConnection(conn);
          }
        } else {
          if (numAttempts >= 5) {
            log.error("Got %d retryable exceptions (giving up) making request to: %s", numAttempts,
                url, e);
          } else {
            log.error("Got non-retryable exception making request to: ", uri, e);
          }

          throw new ApiException("Error performing " + method + " " + url, e);
        }
      }
    }
  }

  /**
   * If we get a 403 (and not on a 'login' URL), we'll reset the authenticated status of the
   * current Authenticator, and try the request again.
   *
   * @throws RequestRetryException
   */
  private static void checkForAuthenticationError(HttpRequest request, HttpResponse response)
      throws ApiException, RequestRetryException {
    // if we get a 403 (and not on a 'login' URL), it means we need to re-authenticate,
    // so do that
    if (response.getStatusLine().getStatusCode() == 403
        && request.getRequestLine().getUri().indexOf("login") < 0) {

      if (sLastRequestStatusCode == 403) {
        // if the last status code we received was 403, then re-authenticating
        // again isn't going to help. This is only useful if, for example, the
        // token has expired.
        return;
      }
      // record the fact that the last status code was 403, so we can fail on the
      // next request if we get another 403 (no point retrying that over and over)
      sLastRequestStatusCode = 403;

      log.info("403 HTTP response code received, attempting to re-authenticate.");
      Realm realm = RealmContext.i.getCurrentRealm();
      realm.getAuthenticator().authenticate(null, realm);

      // throw an exception so that we try the request for a second time.
      throw new RequestRetryException();
    }

    sLastRequestStatusCode = response.getStatusLine().getStatusCode();
  }

  public static RequestManagerStateEvent getCurrentState() {
    RequestManagerStateEvent state = new RequestManagerStateEvent();
    ConnectionPool cp = getConnectionPool();
    if (cp == null) {
      return state;
    }
    state.numInProgressRequests = cp.getNumBusyConnections();
    state.lastUri = cp.getLastUri();
    return state;
  }

  /**
   * Determines whether the given exception is re-tryable or not.
   */
  private static boolean canRetry(Exception e) {
    if (e instanceof RequestRetryException) {
      return true;
    }

    if (e instanceof ConnectException) {
      return false;
    }

    // may be others that we can't, but we'll just rety everything for now
    return true;
  }

  /**
   * This is an event posted to our event bus whenever the state of the {@link RequestManager}
   * changes.
   */
  public static class RequestManagerStateEvent {
    public int numInProgressRequests;
    public String lastUri;
  }
}
