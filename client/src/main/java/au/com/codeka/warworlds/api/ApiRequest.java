package au.com.codeka.warworlds.api;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import com.google.protobuf.Message;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.RealmContext;
import au.com.codeka.warworlds.Util;
import au.com.codeka.warworlds.model.Realm;

/** Encapsulates a request to the server. */
public class ApiRequest {
  private static final MediaType PROTOBUF = MediaType.parse("application/x-protobuf");

  private final String url;
  private final String method;
  @Nullable private final Message requestBody;
  private CompleteCallback completeCallback;
  @Nullable private byte[] responseBytes;
  @Nullable private Message responseProto;
  @Nullable private Bitmap responseBitmap;
  @Nullable private String responseString;
  @Nullable Map<String, List<String>> extraHeaders;

  private ApiRequest(String url, String method, @Nullable Message requestBody,
      @Nullable Map<String, List<String>> extraHeaders,
      @Nullable CompleteCallback completeCallback) {
    this.url = url;
    this.method = method;
    this.requestBody = requestBody;
    this.extraHeaders = extraHeaders;
    this.completeCallback = completeCallback;
  }

  /** Builds the OkHttp request for this request. */
  Request buildOkRequest() {
    Realm realm = RealmContext.i.getCurrentRealm();
    Request.Builder builder = new Request.Builder()
        .url(realm.getBaseUrl().resolve(url).toString())
        .method(method, convertRequestBody())
        .addHeader("User-Agent", "wwmmo/" + Util.getVersion());
    if (extraHeaders != null) {
      for (String headerName : extraHeaders.keySet()) {
        for (String headerValue : extraHeaders.get(headerName)) {
          builder.addHeader(headerName, headerValue);
        }
      }
    }
    if (realm.getAuthenticator().isAuthenticated()) {
      builder.addHeader("Cookie", realm.getAuthenticator().getAuthCookie());
    }
    builder.tag(this);
    return builder.build();
  }

  @SuppressWarnings("unchecked")
  public <T extends Message> T body(Class<T> responseClass) {
    if (responseProto == null) {
      try {
        Method m = responseClass.getDeclaredMethod("parseFrom", byte[].class);
        responseProto = (Message) m.invoke(null, new Object[] {responseBytes});
      } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
        // Should never happen.
      }
    }
    return (T) responseProto;
  }

  /**
   * Returns the body of the response, as a String. If the Content-Type isn't text/* then null is
   * returned instead.
   */
  public String bodyString() {
    return responseString;
  }

  /**
   * Returns the body of the response, as a {@link Bitmap}. If the Content-Type isn't image/*, then
   * null is returned instead.
   */
  public Bitmap bodyBitmap() {
    return responseBitmap;
  }

  void handleResponse(Response response) {
    try {
      if (response.body() != null) {
        if (response.body().contentType().type().equals("text")) {
          responseString = response.body().string();
        } else if (response.body().contentType().type().equals("image")) {
          responseBitmap = BitmapFactory.decodeStream(response.body().byteStream());
        } else {
          responseBytes = response.body().bytes();
        }
        response.body().close();
      }
    } catch (IOException e) {
      // TODO: call failure methods
    }

    // Call the callback, if there is one, on the main thread
    if (completeCallback != null) {
      new Handler(Looper.getMainLooper()).post(new Runnable() {
        @Override
        public void run() {
          completeCallback.onRequestComplete(ApiRequest.this);
        }
      });
    }
  }

  private RequestBody convertRequestBody() {
    if (requestBody == null) {
      return null;
    }
    return RequestBody.create(PROTOBUF, requestBody.toByteArray());
  }

  @Override
  public String toString() {
    return String.format("%s %s", method, url);
  }

  public interface CompleteCallback {
    void onRequestComplete(ApiRequest request);
  }

  public static class Builder {
    private String url;
    private String method;
    @Nullable private Message requestBody;
    @Nullable private Map<String, List<String>> extraHeaders;
    @Nullable private CompleteCallback completeCallback;

    public Builder(String url, String method) {
      this.url = url;
      this.method = method;
    }

    public Builder body(Message body) {
      requestBody = body;
      return this;
    }

    public Builder header(String name, String value) {
      if (extraHeaders == null) {
        extraHeaders = new TreeMap<>();
      }
      List<String> values = extraHeaders.get(name);
      if (values == null) {
        values = new ArrayList<>();
        extraHeaders.put(name, values);
      }
      values.add(value);
      return this;
    }

    /**
     * Sets the callback that is called when the request completes. The callback will always be
     * called on the UI thread.
     */
    public Builder completeCallback(CompleteCallback completeCallback) {
      this.completeCallback = completeCallback;
      return this;
    }

    public ApiRequest build() {
      return new ApiRequest(url, method, requestBody, extraHeaders, completeCallback);
    }
  }
}
