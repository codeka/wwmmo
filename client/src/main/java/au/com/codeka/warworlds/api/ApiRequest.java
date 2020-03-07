package au.com.codeka.warworlds.api;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.google.protobuf.Message;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

import javax.annotation.Nullable;

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.RealmContext;
import au.com.codeka.warworlds.Util;
import au.com.codeka.warworlds.model.Realm;
import okhttp3.CacheControl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpMethod;

/** Encapsulates a request to the server. */
public class ApiRequest {
  private static final Log log = new Log("ApiRequest");

  private static final MediaType PROTOBUF = MediaType.parse("application/x-protobuf");
  private static final MediaType BYTES = MediaType.parse("application/octet-stream");

  private final Timing timing;
  private final String url;
  private final String method;
  @Nullable private final Message requestBody;
  @Nullable private CompleteCallback completeCallback;
  @Nullable ErrorCallback errorCallback;
  private boolean completeOnAnyThread;
  @Nullable private byte[] responseBytes;
  @Nullable private Message responseProto;
  @Nullable private Bitmap responseBitmap;
  @Nullable private String responseString;
  @Nullable private MediaType responseContentType;
  @Nullable private Messages.GenericError error;
  @Nullable private Throwable exception;
  private boolean skipCache;
  private boolean dontLogExceptions;

  private ApiRequest(String url, String method, @Nullable Message requestBody,
      boolean skipCache, @Nullable CompleteCallback completeCallback,
      @Nullable ErrorCallback errorCallback, boolean completeOnAnyThread,
      boolean dontLogExceptions) {
    this.timing = new Timing();
    this.url = url;
    this.method = method;
    this.requestBody = requestBody;
    this.skipCache = skipCache;
    this.completeCallback = completeCallback;
    this.errorCallback = errorCallback;
    this.completeOnAnyThread = completeOnAnyThread;
    this.dontLogExceptions = dontLogExceptions;
  }

  /** Builds the OkHttp request for this request. */
  Request buildOkRequest() {
    Realm realm = RealmContext.i.getCurrentRealm();
    String urlPlus = url;
    if (urlPlus.indexOf('?') >= 0) {
      urlPlus += "&";
    } else {
      urlPlus += "?";
    }
    urlPlus += "t=" + System.currentTimeMillis();
    Request.Builder builder = new Request.Builder()
        .url(realm.getBaseUrl().resolve(urlPlus).toString())
        .method(method, convertRequestBody())
        .addHeader("User-Agent", "wwmmo/" + Util.getVersion());
    if (skipCache) {
      builder.cacheControl(CacheControl.FORCE_NETWORK);
    }
    if (realm.getAuthenticator().isAuthenticated()) {
      builder.addHeader("Cookie", realm.getAuthenticator().getAuthCookie());
    }
    builder.tag(this);
    return builder.build();
  }

  public Timing getTiming() {
    return timing;
  }

  @Nullable
  @SuppressWarnings("unchecked")
  public <T extends Message> T body(Class<T> responseClass) {
    if (responseProto == null && responseBytes != null) {
      try {
        Method m = responseClass.getDeclaredMethod("parseFrom", byte[].class);
        responseProto = (Message) m.invoke(null, new Object[] {responseBytes});
      } catch (NoSuchMethodException e) {
        log.error("Unexpected error parsing response.", e);
      } catch (InvocationTargetException e) { // These must be split out to support < KITKAT
        log.error("Unexpected error parsing response.", e);
      } catch (IllegalAccessException e) { // These must be split out to support < KITKAT
        log.error("Unexpected error parsing response.", e);
      }
    } else if (responseProto == null) {
      log.info("Got no response body (content-type was: %s), not returning any proto.",
          responseContentType);
    }
    return (T) responseProto;
  }

  public String url() {
    return url;
  }

  /**
   * Returns the body of the response, as a String. If the Content-Type isn't text/* then null is
   * returned instead.
   */
  public String bodyString() {
    return responseString;
  }

  public Messages.GenericError error() {
    return this.error;
  }

  public Throwable exception() {
    return exception;
  }

  public boolean dontLogExceptions() {
    return dontLogExceptions;
  }

  /**
   * Returns the body of the response, as a {@link Bitmap}. If the Content-Type isn't image/*, then
   * null is returned instead.
   */
  public Bitmap bodyBitmap() {
    return responseBitmap;
  }

  void handleResponse(Response response) {
    if (response.code() > 399) {
      handleError(response, null);
    } else {
      try {
        ResponseBody body = response.body();
        if (body != null) {
          responseContentType = body.contentType();
          if (responseContentType != null) {
            if (responseContentType.type().equals("text")) {
              responseString = body.string();
            } else if (responseContentType.type().equals("image")) {
              responseBitmap = BitmapFactory.decodeStream(body.byteStream());
            } else {
              responseBytes = body.bytes();
            }
            body.close();
          }
        }
      } catch (IOException e) {
        log.error("Unexpected error decoding body.", e);
        handleError(response, e);
        return;
      }
    }

    // Call the callback, if there is one, on the main thread
    if (completeCallback != null) {
      if (completeOnAnyThread) {
        runCompleteCallback();
      } else {
        new Handler(Looper.getMainLooper()).post(this::runCompleteCallback);
      }
    }
  }

  private void runCompleteCallback() {
    if (completeCallback == null) {
      return;
    }
    try {
      completeCallback.onRequestComplete(this);
    } catch (Exception e) {
      log.error("Error in complete callback.", e);
    }
  }

  void handleError(@Nullable Response response, Throwable e) {
    if (response != null && response.body() != null) {
      try {
        responseBytes = response.body().bytes();
        error = body(Messages.GenericError.class);
        log.warning("Got error: %s", error);
      } catch (Exception ex) {
        log.warning("Error parsing response.", ex);
        if (e == null) {
          error = convertToGenericError(ex);
        }
      }
    }
    if (response != null && response.code() == 403) {
      error = Messages.GenericError.newBuilder()
          .setErrorCode(Messages.GenericError.ErrorCode.AuthenticationError.getNumber())
          .setErrorMessage(response.message())
          .build();
    }
    if (error == null && e != null) {
      error = convertToGenericError(e);
    }
    exception = e;
    if (errorCallback != null) {
      new Handler(Looper.getMainLooper()).post(
          () -> errorCallback.onRequestError(ApiRequest.this, error));
    }
  }

  private Messages.GenericError convertToGenericError(Throwable e) {
    Messages.GenericError.ErrorCode errorCode = Messages.GenericError.ErrorCode.UnknownError;
    if (e instanceof IOException) {
      errorCode = Messages.GenericError.ErrorCode.NetworkError;
    }
    return Messages.GenericError.newBuilder()
        .setErrorCode(errorCode.getNumber())
        .setErrorMessage(e.getMessage())
        .build();
  }

  private RequestBody convertRequestBody() {
    if (requestBody == null) {
      if (HttpMethod.requiresRequestBody(method)) {
        // A POST request must have a body, so we'll just give it an empty one.
        return RequestBody.create(BYTES, new byte[0]);
      } else {
        return null;
      }
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

  public interface ErrorCallback {
    void onRequestError(ApiRequest request, Messages.GenericError error);
  }

  public static class Builder {
    private String url;
    private String method;
    @Nullable private Message requestBody;
    private boolean skipCache;
    @Nullable private CompleteCallback completeCallback;
    @Nullable private ErrorCallback errorCallback;
    private boolean completeOnAnyThread;
    private boolean dontLogExceptions;

    public Builder(String url, String method) {
      this.url = url;
      this.method = method;
    }

    public Builder(Uri uri, String method) {
      this.url = uri.toString();
      this.method = method;
    }

    public Builder body(Message body) {
      requestBody = body;
      return this;
    }

    public Builder skipCache(boolean skipCache) {
      this.skipCache = skipCache;
      return this;
    }

    /**
     * Sets whether or not to log exceptions. Default is to log them.
     */
    public Builder dontLogExceptions(boolean dontLog) {
      this.dontLogExceptions = dontLog;
      return this;
    }

    /**
     * Sets the callback that is called when the request completes. The callback will be called on
     * the UI thread, unless you set {@link #completeOnAnyThread} to true.
     */
    public Builder completeCallback(CompleteCallback completeCallback) {
      this.completeCallback = completeCallback;
      return this;
    }

    /**
     * Sets the callback that is called when the request completes with an error. The normal
     * {@link CompleteCallback} will not be called in this case. Regardless of what you have set
     * for {@link #completeOnAnyThread}, this will always be called on the UI thread.
     */
    public Builder errorCallback(ErrorCallback errorCallback) {
      this.errorCallback = errorCallback;
      return this;
    }

    /**
     * By setting this to true, you're indicating that you don't care what thread the complete
     * callback is called on. If this is false, then the callback is called on the UI thread.
     */
    public Builder completeOnAnyThread(boolean completeOnAnyThread) {
      this.completeOnAnyThread = completeOnAnyThread;
      return this;
    }

    public ApiRequest build() {
      return new ApiRequest(url, method, requestBody, skipCache, completeCallback, errorCallback,
          completeOnAnyThread, dontLogExceptions);
    }
  }

  public static class Timing {
    private long startTime;
    private long requestSentTime;
    private long responseReceivedTime;

    public Timing() {
      startTime = SystemClock.elapsedRealtime();
      requestSentTime = startTime;
      responseReceivedTime = startTime;
    }

    public void onRequestSent() {
      requestSentTime = SystemClock.elapsedRealtime();
    }

    public void onResponseReceived() {
      responseReceivedTime = SystemClock.elapsedRealtime();
    }

    public long getQueueTime() {
      return requestSentTime - startTime;
    }

    @Override
    public String toString() {
      return String.format(Locale.US, "[queue-time: %dms, server-time: %dms]",
          requestSentTime - startTime, responseReceivedTime - requestSentTime);
    }
  }
}
