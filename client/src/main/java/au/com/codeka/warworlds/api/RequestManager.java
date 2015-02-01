package au.com.codeka.warworlds.api;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import javax.annotation.Nullable;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.eventbus.EventBus;

/**
 * Provides the low-level interface for making requests to the API.
 */
public class RequestManager {
  public static RequestManager i = new RequestManager();
  public static final EventBus eventBus = new EventBus();

  private static final Log log = new Log("RequestManager");
  private static final boolean DBG = false;

  private static final int MAX_INFLIGHT_REQUESTS = 8;

  private final OkHttpClient httpClient = new OkHttpClient();

  // The last status code we got from the server, don't try to re-authenticate if we get two 403's
  // in a row, for example.
  private int lastRequestStatusCode = 200;

  private final Set<ApiRequest> inFlightRequests = new HashSet<>();

  // We use a stack because the most recent request is likely the most important (since it's usually
  // going to be in response to the most recent UI interaction).
  private final Stack<ApiRequest> waitingRequests = new Stack<>();

  private final Object lock = new Object();

  /**
   * Enqueues the given request to send to the server. The request's callbacks will be called
   * when the request actually completes.
   */
  public void sendRequest(ApiRequest apiRequest) {
    synchronized (lock) {
      if (inFlightRequests.size() < MAX_INFLIGHT_REQUESTS) {
        enqueueRequest(apiRequest);
      } else {
        waitingRequests.push(apiRequest);
      }
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> T parseResponse(Response response, Class<T> protoBuffFactory) {
    try {
      Method m = protoBuffFactory.getDeclaredMethod("parseFrom", InputStream.class);
      return (T) m.invoke(null, response.body().byteStream());
    } catch (Exception e) {
      return null;
    }
  }

  private void handleResponse(ApiRequest request, Response response) {
    requestComplete(request);
    request.handleResponse(response);
  }

  /**
   * Handles a failed request. Either response or the exception will set, depending on whether the
   * error was network-related or API related.
   */
  private void handleFailure(ApiRequest request, @Nullable Response response,
      @Nullable IOException e) {
    requestComplete(request);
    // TODO: handle
  }

  /** Removes the given request from the in-flight collection and potentially enqueues another. */
  private void requestComplete(ApiRequest request) {
    synchronized (lock) {
      inFlightRequests.remove(request);
      if (!waitingRequests.isEmpty()) {
        ApiRequest nextRequest = waitingRequests.pop();
        if (inFlightRequests.size() < MAX_INFLIGHT_REQUESTS) {
          inFlightRequests.add(nextRequest);
          enqueueRequest(nextRequest);
        }
      }
    }
  }

  void enqueueRequest(ApiRequest apiRequest) {
    inFlightRequests.add(apiRequest);
    httpClient.newCall(apiRequest.buildOkRequest()).enqueue(responseCallback);
  }

  Response callRequest(ApiRequest apiRequest) throws IOException {
    return httpClient.newCall(apiRequest.buildOkRequest()).execute();
  }

  private Callback responseCallback = new Callback() {
    @Override
    public void onFailure(Request request, IOException e) {
      handleFailure((ApiRequest) request.tag(), null, e);
    }

    @Override
    public void onResponse(Response response) throws IOException {
      ApiRequest request = (ApiRequest) response.request().tag();
      handleResponse(request, response);
    }
  };

  /**
   * This is an event posted to our event bus whenever the state of the {@link RequestManager}
   * changes.
   */
  public static class RequestManagerStateEvent {
    public int numInProgressRequests;
    public String lastUri;
  }
}
