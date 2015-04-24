package au.com.codeka.warworlds.server;

import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.squareup.wire.Message;
import com.squareup.wire.WireTypeAdapterFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Random;
import java.util.regex.Matcher;

import javax.annotation.Nullable;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.codeka.common.Log;
import au.com.codeka.common.Wire;
import au.com.codeka.warworlds.server.ctrl.AdminController;
import au.com.codeka.warworlds.server.data.SqlStateTranslater;
import au.com.codeka.warworlds.server.model.BackendUser;

/**
 * This is the base class for the game's request handlers. It handles some common tasks such as
 * extracting protocol buffers from the request body, and so on.
 */
public class RequestHandler {
  private final Log log = new Log("RequestHandler");
  private HttpServletRequest request;
  private HttpServletResponse response;
  private Matcher routeMatcher;
  private Session session;
  private String extraOption;

  protected String getUrlParameter(String name) {
    try {
      return routeMatcher.group(name);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  protected String getRealm() {
    return getUrlParameter("realm");
  }

  /** Gets the "extra" option that was passed in the route configuration. */
  protected String getExtraOption() {
    return extraOption;
  }

  public void handle(Matcher routeMatcher, String extraOption, Session session,
      HttpServletRequest request, HttpServletResponse response) {
    this.request = request;
    this.response = response;
    this.routeMatcher = routeMatcher;
    this.extraOption = extraOption;
    this.session = session;

    RequestContext.i.setContext(request);

    // start off with status 200, but the handler might change it
    this.response.setStatus(200);

    RequestException lastException = null;
    for (int retries = 0; retries < 10; retries++) {
      try {
        onBeforeHandle();
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
        Throwable cause = e.getCause();
        if (cause instanceof SQLException && SqlStateTranslater.isRetryable((SQLException) cause)
            && supportsRetryOnDeadlock()) {
          try {
            Thread.sleep(50 + new Random().nextInt(100));
          } catch (InterruptedException e1) {
            // Ignore.
          }
          log.warning("Retrying deadlock.", e);
          lastException = e;
          continue;
        }
        if (e.getHttpErrorCode() < 500) {
          log.warning("Unhandled error in URL: " + request.getRequestURI(), e);
        } else {
          log.info("Request: " + getRequestDebugString(request));
          log.error("Unhandled error in URL: " + request.getRequestURI(), e);
        }
        e.populate(this.response);
        setResponseBody(e.getGenericError());
        return;
      } catch (Throwable e) {
        log.error("Unhandled error!", e);
        this.response.setStatus(500);
        return;
      }
    }

    // if we get here, it's because we exceeded the number of retries.
    if (lastException != null) {
      log.error("Too many retries: " + request.getRequestURI(), lastException);
      lastException.populate(this.response);
      handleException(lastException);
    }
  }

  protected void handleException(RequestException e) {
    setResponseBody(e.getGenericError());
  }

  /**
   * This is called before the get(), put(), etc methods but after the request
   * is set up, ready to go.
   */
  protected void onBeforeHandle() throws RequestException {
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
   * You can override this in subclass to indicate that the request supports automatic
   * retry on deadlock.
   */
  protected boolean supportsRetryOnDeadlock() {
    return false;
  }

  /**
   * Sets the required headers so that the client will know this response can be cached for the
   * given number of hours. The default response includes no caching headers.
   *
   * @param hours Number of hours to cache for.
   * @param etag An optional value to include in the ETag header. This can be any string at all,
   *             and we will hash and base-64 encode it for you.
   */
  protected void setCacheTime(float hours, @Nullable String etag) {
    response.setHeader("Cache-Control", String.format("private, max-age=%d",
        (int)(hours * 3600)));
    if (etag != null) {
      etag = BaseEncoding.base64().encode(
          Hashing.sha1().hashString(etag, Charset.defaultCharset()).asBytes());
      response.setHeader("ETag", String.format("\"%s\"", etag));
    }
  }

  protected void setResponseText(String text) {
    response.setContentType("text/plain");
    response.setCharacterEncoding("utf-8");
    try {
      response.getWriter().write(text);
    } catch (IOException e) {
      // Ignore?
    }
  }

  protected void setResponseJson(JsonObject json) {
    response.setContentType("application/json");
    response.setCharacterEncoding("utf-8");
    try {
      response.getWriter().write(json.toString());
    } catch (IOException e) {
      // Ignore?
    }
  }

  protected void setResponseBody(Message pb) {
    if (pb == null) {
      return;
    }

    if (request.getHeader("Accept") != null) {
      for (String acceptValue : request.getHeader("Accept").split(",")) {
        if (acceptValue.startsWith("text/")) {
          setResponseBodyText(pb);
          return;
        } else if (acceptValue.startsWith("application/json")) {
          setResponseBodyJson(pb);
          return;
        }
      }
    }

    response.setContentType("application/x-protobuf");
    response.setHeader("Content-Type", "application/x-protobuf");
    try {
      response.getOutputStream().write(pb.toByteArray());
    } catch (IOException e) {
      // Ignore.
    }
  }

  private void setResponseBodyText(Message pb) {
    response.setContentType("text/plain");
    response.setCharacterEncoding("utf-8");
    writeJsonBody(pb, true);
  }

  private void setResponseBodyJson(Message pb) {
    response.setContentType("application/json");
    response.setCharacterEncoding("utf-8");
    writeJsonBody(pb, false);
  }

  private void writeJsonBody(Message pb, boolean prettyPrint) {
    try {
      GsonBuilder gsonBuilder = new GsonBuilder()
          .registerTypeAdapterFactory(new WireTypeAdapterFactory(Wire.i));
      if (prettyPrint) {
        gsonBuilder.setPrettyPrinting();
      }
      Gson gson = gsonBuilder.create();

      PrintWriter writer = response.getWriter();
      writer.write(gson.toJson(pb));
      writer.flush();
    } catch (IOException e) {
      // Ignore.
    }
  }

  protected void redirect(String url) {
    response.setStatus(302);
    response.addHeader("Location", url);
  }

  protected HttpServletRequest getRequest() {
    return request;
  }

  protected HttpServletResponse getResponse() {
    return response;
  }

  protected String getRequestUrl() {
    URI requestURI;
    try {
      requestURI = new URI(request.getRequestURL().toString());
    } catch (URISyntaxException e) {
      return null; // should never happen!
    }

    // TODO(dean): is hard-coding the https part for game.war-worlds.com the best way? no...
    if (requestURI.getHost().equals("game.war-worlds.com")) {
      return "https://game.war-worlds.com" + requestURI.getPath();
    } else {
      return requestURI.toString();
    }
  }

  protected Session getSession() throws RequestException {
    if (session == null) {
      throw new RequestException(403);
    }

    return session;
  }

  protected Session getSessionNoError() {
    return session;
  }

  protected boolean isAdmin() throws RequestException {
    return (session != null && session.isAdmin());
  }

  /**
   * Checks whether the current user is in the given role. If the user is not an admin, then they
   * are -- by definition -- not in any roles.
   */
  protected boolean isInRole(BackendUser.Role role) throws RequestException {
    if (session == null || !session.isAdmin()) {
      return false;
    }

    BackendUser backendUser = new AdminController().getBackendUser(session.getActualEmail());
    if (backendUser == null) {
      // should  be impossible if it's really an admin user...
      throw new RequestException(500, "This is impossible.");
    }

    return backendUser.isInRole(role);
  }

  protected <T extends Message> T getRequestBody(Class<T> protoBuffFactory)
      throws RequestException {
    if (request.getHeader("Content-Type").equals("application/json")) {
      return getRequestBodyJson(protoBuffFactory);
    }

    InputStream ins = null;
    try {
      ins = request.getInputStream();
      return Wire.i.parseFrom(ins, protoBuffFactory);
    } catch (IOException e) {
      throw new RequestException(e);
    } finally {
      if (ins != null) {
        try { ins.close(); } catch (IOException e) { /* Ignore. */ }
      }
    }
  }

  private <T> T getRequestBodyJson(Class<T> protoBuffFactory) {
    try {
      Gson gson = new GsonBuilder()
          .registerTypeAdapterFactory(new WireTypeAdapterFactory(Wire.i))
          .create();

      return gson.fromJson(new InputStreamReader(request.getInputStream()), protoBuffFactory);
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Get details about the given request as a string (for debugging).
   */
  private String getRequestDebugString(HttpServletRequest request) {
    return request.getRequestURI()
        + "\nX-Real-IP: " + request.getHeader("X-Real-IP")
        + "\nUser-Agent: " + request.getHeader("User-Agent")
        + "\n";
  }
}
