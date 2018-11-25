package au.com.codeka.warworlds.client.net;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.client.util.GameSettings;
import au.com.codeka.warworlds.common.Log;

/**
 * Simple wrapper around {@link HttpURLConnection} that lets us make HTTP requests more easily.
 */
public class HttpRequest {
  private final static Log log = new Log("HttpRequest");

  public enum Method {
    GET,
    POST,
    PUT,
    DELETE
  }

  private final String url;
  private HttpURLConnection conn;
  private IOException exception;
  private byte[] body;

  private HttpRequest(Builder builder) {
    this.url = Preconditions.checkNotNull(builder.url);
    log.info("HTTP %s %s (%d bytes)", builder.method, builder.url,
        builder.body == null ? 0 : builder.body.length);

    try {
      URL url = new URL(builder.url);
      conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod(builder.method.toString());
      if (builder.authenticated) {
        String cookie = GameSettings.i.getString(GameSettings.Key.COOKIE);
        conn.setRequestProperty("COOKIE", cookie);
      }
      for (String key : builder.headers.keySet()) {
        conn.setRequestProperty(key, builder.headers.get(key));
      }
      if (builder.body != null) {
        conn.setRequestProperty("Content-Length", Integer.toString(builder.body.length));
        conn.getOutputStream().write(builder.body);
      }
    } catch (IOException e) {
      log.warning("Error fetching '%s'", builder.url, e);
      this.exception = e;
    }
  }

  public int getResponseCode() {
    if (exception != null) {
      return -1;
    }

    try {
      return conn.getResponseCode();
    } catch(IOException e) {
      log.warning("Error fetching '%s'", url, e);
      exception = e;
      return -1;
    }
  }

  public IOException getException() {
    return exception;
  }

  @Nullable
  public byte[] getBody() {
    if (exception != null || getResponseCode() != 200) {
      return null;
    }

    if (body == null) {
      try {
        body = ByteStreams.toByteArray(conn.getInputStream());
      } catch (IOException e) {
        log.warning("Error fetching '%s'", url, e);
        exception = e;
        return null;
      }
    }

    return body;
  }

  /** Simple wrapper around {@link #getBody()} that casts to a proto message. */
  @SuppressWarnings("unchecked")
  @Nullable
  public <T extends Message> T getBody(Class<T> protoType) {
    byte[] bytes = getBody();
    if (bytes == null) {
      return null;
    }

    try {
      Field adapterField = protoType.getField("ADAPTER");
      ProtoAdapter<T> adapter = (ProtoAdapter<T>) adapterField.get(null);
      return adapter.decode(bytes);
    } catch (IOException e) {
      exception = e;
      log.warning("Error fetching '%s'", url, e);
      return null;
    } catch (Exception e) {
      log.warning("Error fetching '%s'", url, e);
      return null;
    }
  }

  public static class Builder {
    private String url;
    private Method method;
    private HashMap<String, String> headers;
    private byte[] body;
    private boolean authenticated;

    public Builder() {
      method = Method.GET;
      headers = new HashMap<>();
    }

    public Builder url(String url) {
      this.url = url;
      return this;
    }

    public Builder method(Method method) {
      this.method = method;
      return this;
    }

    public Builder header(String name, String value) {
      headers.put(name, value);
      return this;
    }

    public Builder authenticated() {
      this.authenticated = true;
      return this;
    }

    public Builder body(byte[] body) {
      this.body = body;
      return this;
    }

    public HttpRequest build() {
      return new HttpRequest(this);
    }
  }
}
