package au.com.codeka.warworlds.server.handlers;

import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.wire.Message;
import com.squareup.wire.WireTypeAdapterFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Matcher;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.codeka.warworlds.common.Log;

/**
 * This is the base class for the game's request handlers. It handles some common tasks such as
 * extracting protocol buffers from the request body, and so on.
 */
public class RequestHandler {
  private final Log log = new Log("RequestHandler");
  private HttpServletRequest request;
  private HttpServletResponse response;
  private Matcher routeMatcher;
  private String extraOption;

  /** Set up this {@link RequestHandler}, must be called before any other methods. */
  public void setup(
      Matcher routeMatcher,
      String extraOption,
      HttpServletRequest request,
      HttpServletResponse response) {
    this.routeMatcher = routeMatcher;
    this.extraOption = extraOption;
    this.request = request;
    this.response = response;
  }

  protected String getUrlParameter(String name) {
    try {
      return routeMatcher.group(name);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /** Gets the "extra" option that was passed in the route configuration. */
  @Nullable
  protected String getExtraOption() {
    return extraOption;
  }

  public void handle() throws RequestException {
    // start off with status 200, but the handler might change it
    this.response.setStatus(200);

    try {
      if (!onBeforeHandle()) {
        return;
      }

      switch (request.getMethod()) {
        case "GET":
          get();
          break;
        case "POST":
          post();
          break;
        case "PUT":
          put();
          break;
        case "DELETE":
          delete();
          break;
        default:
          throw new RequestException(501);
      }
    } catch (RequestException e) {
      handleException(e);
    } catch (Exception e) {
      log.error("Unexpected exception", e);
      throw e;
    }
  }

  protected void handleException(RequestException e) throws RequestException {
    log.error("Unhandled exception", e);
    throw e;
  }

  /**
   * This is called before the get(), put(), etc methods but after the request is set up, ready to
   * go.
   *
   * @return true if we should continue processing the request, false if not. If you return false
   *     then you should have set response headers, status code and so on already.
   */
  protected boolean onBeforeHandle() {
    return true;
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

  protected void setCacheTime(float hours) {
    setCacheTime(hours, null);
  }

  /**
   * Sets the required headers so that the client will know this response can be cached for the
   * given number of hours. The default response includes no caching headers.
   *
   * @param hours Time, in hours, to cache this response.
   * @param etag An optional value to include in the ETag header. This can be any string at all,
   *             and we will hash and base-64 encode it for you.
   */
  protected void setCacheTime(float hours, @Nullable String etag) {
    response.setHeader("Cache-Control",
        String.format(Locale.US, "private, max-age=%d", (int)(hours * 3600)));
    if (etag != null) {
      etag = BaseEncoding.base64().encode(
          Hashing.sha256().hashString(etag, Charset.defaultCharset()).asBytes());
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

  protected void setResponseJson(Message<?, ?> pb) {
    response.setContentType("application/json");
    response.setCharacterEncoding("utf-8");
    try {
      PrintWriter writer = response.getWriter();
      Gson gson = new GsonBuilder()
          .registerTypeAdapterFactory(new WireTypeAdapterFactory())
          .serializeSpecialFloatingPointValues()
          .disableHtmlEscaping()
          .create();
      String json = gson.toJson(pb);
      // serializeSpecialFloatingPointValues() will insert literal "Infinity" "-Infinity" and "NaN"
      // which is not valid JSON. We'll replace those with nulls in a kind-naive way.
      json = json
          .replaceAll(":Infinity", ":null")
          .replaceAll(":-Infinity", ":null")
          .replaceAll(":NaN", ":null");
      writer.write(json);
      writer.flush();
    } catch (IOException e) {
      // Ignore.
    }
  }

  protected void setResponseGson(Object obj) {
    response.setContentType("application/json");
    response.setCharacterEncoding("utf-8");
    try {
      PrintWriter writer = response.getWriter();
      Gson gson = new GsonBuilder()
          .disableHtmlEscaping()
          .create();
      writer.write(gson.toJson(obj));
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

  @Nullable
  private <T> T getRequestJson(Class<T> protoType) {
    String json;
    try {
      Scanner scanner = new Scanner(request.getInputStream(), request.getCharacterEncoding())
          .useDelimiter("\\A");
      json = scanner.hasNext() ? scanner.next() : "";
    } catch (IOException e) {
      return null;
    }

    return fromJson(json, protoType);
  }

  @Nullable
  protected <T> T fromJson(String json, Class<T> protoType) {
    try {
      Gson gson = new GsonBuilder()
          .registerTypeAdapterFactory(new WireTypeAdapterFactory())
          .disableHtmlEscaping()
          .create();
      return gson.fromJson(json, protoType);
    } catch (Exception e) {
      return null;
    }
  }

  /** Get details about the given request as a string (for debugging). */
  private String getRequestDebugString(HttpServletRequest request) {
    return request.getRequestURI()
        + "\nX-Real-IP: " + request.getHeader("X-Real-IP")
        + "\nUser-Agent: " + request.getHeader("User-Agent")
        + "\n";
  }
}
