package au.com.codeka.warworlds.intel;

import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;

import java.nio.charset.Charset;
import java.util.regex.Matcher;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.codeka.common.Log;

/**
 * This is the base class for the game's request handlers. It handles some common tasks such as
 * extracting protocol buffers from the request body, and so on.
 */
public class RequestHandler {
  private final Log log = new Log("RequestHandler");
  private HttpServletRequest request;
  private HttpServletResponse response;
  private Matcher routeMatcher;

  protected String getUrlParameter(String name) {
    try {
      return routeMatcher.group(name);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public void handle(
      Matcher routeMatcher, HttpServletRequest request, HttpServletResponse response) {
    this.request = request;
    this.response = response;
    this.routeMatcher = routeMatcher;

    // start off with status 200, but the handler might change it
    this.response.setStatus(200);

    for (int retries = 0; retries < 10; retries++) {
      try {
        if (request.getMethod().equals("GET")) {
          get();
        } else if (request.getMethod().equals("POST")) {
          post();
        } else if (request.getMethod().equals("PUT")) {
          put();
        } else if (request.getMethod().equals("DELETE")) {
          delete();
        } else {
          throw new RequestException(501);
        }

        return; // break out of the retry loop
      } catch (RequestException e) {
        if (e.getHttpErrorCode() < 500) {
          log.warning("Unhandled error in URL: " + request.getRequestURI(), e);
        } else {
          log.error("Unhandled error in URL: " + request.getRequestURI(), e);
        }
        e.populate(this.response);
        return;
      } catch (Throwable e) {
        log.error("Unhandled error!", e);
        this.response.setStatus(500);
        return;
      }
    }
  }

  protected void get() throws RequestException {
    throw new RequestException(501);
  }

  protected void put() throws RequestException {
    throw new RequestException(501);
  }

  protected void post() throws RequestException {
    throw new RequestException(501);
  }

  protected void delete() throws RequestException {
    throw new RequestException(501);
  }

  /**
   * Sets the required headers so that the client will know this response can be cached for the
   * given number of hours. The default response includes no caching headers.
   *
   * @param hours
   * @param etag An optional value to include in the ETag header. This can be any string at all,
   *             and we will hash and base-64 encode it for you.
   */
  protected void setCacheTime(float hours, @Nullable String etag) {
    response.setHeader("Cache-Control", String.format("private, max-age=%d", (int) (hours * 3600)));
    if (etag != null) {
      etag = BaseEncoding.base64().encode(
          Hashing.sha1().hashString(etag, Charset.defaultCharset()).asBytes());
      response.setHeader("ETag", String.format("\"%s\"", etag));
    }
  }

  protected HttpServletRequest getRequest() {
    return request;
  }

  protected HttpServletResponse getResponse() {
    return response;
  }
}
