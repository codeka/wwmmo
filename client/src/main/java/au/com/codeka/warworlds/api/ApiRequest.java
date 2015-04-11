package au.com.codeka.warworlds.api;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.google.protobuf.Message;
import com.squareup.okhttp.CacheControl;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import org.apache.http.HttpException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.annotation.Nullable;

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.RealmContext;
import au.com.codeka.warworlds.Util;
import au.com.codeka.warworlds.model.Realm;

/** Encapsulates a request to the server. */
public class ApiRequest {
  private static final Log log = new Log("ApiRequest");

  private static final MediaType PROTOBUF = MediaType.parse("application/x-protobuf");

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

  private ApiRequest(String url, String method, @Nullable Message requestBody,
      boolean skipCache, @Nullable CompleteCallback completeCallback,
      @Nullable ErrorCallback errorCallback,
      boolean completeOnAnyThread) {
    this.timing = new Timing();
    this.url = url;
    this.method = method;
    this.requestBody = requestBody;
    this.skipCache = skipCache;
    this.completeCallback = completeCallback;
    this.errorCallback = errorCallback;
    this.completeOnAnyThread = completeOnAnyThread;
  }

  /** Builds the OkHttp request for this request. */
  Request buildOkRequest() {
    Realm realm = RealmContext.i.getCurrentRealm();
    Request.Builder builder = new Request.Builder()
        .url(realm.getBaseUrl().resolve(url).toString())
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
      } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
        log.error("Unexpected error parsing response.", e);
      }
    } else if (responseProto == null) {
      log.info("Got no response body (content-type was: %s), not returning any proto.",
          responseContentType);
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

  public Messages.GenericError error() {
    return this.error;
  }

  public Throwable exception() {
    return exception;
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
      ResponseBody body = response.body();
      if (body != null) {
        responseContentType = body.contentType();
        if (responseContentType != null) {
          if (responseContentType.type().equals("text")) {
            responseString = body.string();
          } else if (responseContentType.type().equals("image")) {
            responseBitmap = BitmapFactory.decodeStream(response.body().byteStream());
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

    // Call the callback, if there is one, on the main thread
    if (completeCallback != null) {
      if (completeOnAnyThread) {
        completeCallback.onRequestComplete(this);
      } else {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
          @Override
          public void run() {
            completeCallback.onRequestComplete(ApiRequest.this);
          }
        });
      }
    }
  }

  void handleError(Response response, Throwable e) {
    if (response != null && response.body() != null) {
      try {
        responseBytes = response.body().bytes();
        error = body(Messages.GenericError.class);
      } catch (IOException ex) {
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
      new Handler(Looper.getMainLooper()).post(new Runnable() {
        @Override
        public void run() {
          errorCallback.onRequestError(ApiRequest.this, error);
        }
      });
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
          completeOnAnyThread);
    }
  }

  public static class Timing {
    private long startTime;
    private long requestSentTime;
    private long responseReceivedTime;

    public Timing() {
      startTime = SystemClock.elapsedRealtime();
    }

    public void onRequestSent() {
      requestSentTime = SystemClock.elapsedRealtime();
    }

    public void onResponseReceived() {
      responseReceivedTime = SystemClock.elapsedRealtime();
    }

    @Override
    public String toString() {
      return String.format("[queue-time: %dms, server-time: %dms]",
          requestSentTime - startTime, responseReceivedTime - requestSentTime);
    }
  }
}
