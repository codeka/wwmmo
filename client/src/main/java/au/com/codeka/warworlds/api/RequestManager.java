package au.com.codeka.warworlds.api;

import android.content.Context;
import android.os.Handler;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.eventbus.EventBus;

import au.com.codeka.warworlds.metrics.MetricsManager;
import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Connection;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Provides the low-level interface for making requests to the API.
 */
public class RequestManager {
  public static final RequestManager i = new RequestManager();
  public static final EventBus eventBus = new EventBus();

  private static final Log log = new Log("RequestManager");
  private static final boolean DBG = true;

  private static final int MAX_INFLIGHT_REQUESTS = 8;
  private static final int MAX_CACHE_SIZE = 50 * 1024 * 1024; // 50MB
  private static final int MAX_IDLE_CONNECTIONS = 8;
  private static final long CONNECTION_POOL_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(30);

  private OkHttpClient httpClient;
  private final Set<ApiRequest> inFlightRequests = new HashSet<>();

  // We use a stack because the most recent request is likely the most important (since it's usually
  // going to be in response to the most recent UI interaction).
  private final Stack<ApiRequest> waitingRequests = new Stack<>();

  private final Handler handler = new Handler();

  private final Object lock = new Object();

  private RequestManager() {
  }

  /** Sets up the request manager, once we've got a context. Initializes the cache and so on. */
  public void setup(Context context) {
    File cacheDir = new File(context.getCacheDir(), "api");
    if (!cacheDir.mkdirs()) {
      // Ignore, directory (probably) already exists
    }

    httpClient = new OkHttpClient.Builder()
        .addInterceptor(new LoggingInterceptor())
        .connectTimeout(5, TimeUnit.SECONDS)
        .cache(new Cache(cacheDir, MAX_CACHE_SIZE))
        .connectionPool(
            new ConnectionPool(
                MAX_IDLE_CONNECTIONS, CONNECTION_POOL_TIMEOUT_MS, TimeUnit.MILLISECONDS))
        .build();
  }

  public RequestManagerState getCurrentState() {
    return calculateState();
  }

  /**
   * Enqueues the given request to send to the server. The request's callbacks will be called
   * when the request actually completes.
   */
  public void sendRequest(ApiRequest apiRequest) {
    synchronized (lock) {
      if (inFlightRequests.size() < MAX_INFLIGHT_REQUESTS) {
        enqueueRequest(apiRequest);
      } else {
        log.info("Too many in-flight requests, waiting.");
        waitingRequests.push(apiRequest);
      }
      updateState();
    }
  }

  public Response sendRequestSync(ApiRequest apiRequest) {
    if (DBG) log.info(">> %s", apiRequest);
    apiRequest.getTiming().onRequestSent();
    try {
      Response resp = httpClient.newCall(apiRequest.buildOkRequest()).execute();
      if (DBG) log.info("<< %s", resp);
      apiRequest.handleResponse(resp);
      return resp;
    } catch (IOException e) {
      apiRequest.handleError(null, e);
      return null;
    }
  }

  private void handleResponse(ApiRequest request, Response response) {
    if (!response.isSuccessful()) {
      handleFailure(request, response, null);
    } else {
      request.handleResponse(response);
      requestComplete(request, response);
    }
  }

  /**
   * Handles a failed request. Either response or the exception will set, depending on whether the
   * error was network-related or API related.
   */
  private void handleFailure(ApiRequest request, @Nullable Response response,
      @Nullable IOException e) {
    request.handleError(response, e);

    if (e != null) {
      log.error("Error in request: %s", request, e);
      requestComplete(request, response);
    } else if (response != null) {
      log.warning("Error in response: %d %s", response.code(), response.message());

      if (response.code() == 429) {
        // If we've been rate-limited, instead of handing that off to the ApiRequest, we'll retry
        // it internally (hence, keeping this request in the in-flight queue, and slowing down
        // requests, until we can get back under the rate-limit.
        if (DBG) log.info("-- %s 429 rate-limited timing=%s", request, request.getTiming());

        if (request.getTiming().getQueueTime() > 30000) {
          // Queued for too long, give up.
          requestComplete(request, response);
          return;
        }

        // This will delay for the same time it's been queued already, which is a kind of
        // exponential backoff.
        handler.postDelayed(() -> enqueueRequest(request), request.getTiming().getQueueTime());
        return;
      }

      requestComplete(request, response);
    } else {
      throw new IllegalStateException("One of response or e should be non-null.");
    }
  }

  /** Removes the given request from the in-flight collection and potentially enqueues another. */
  private void requestComplete(ApiRequest request, @Nullable Response response) {
    synchronized (lock) {
      request.getTiming().onResponseReceived();
      if (DBG) log.info("<< %s %d %s timing=%s", request, response == null ? 0 : response.code(),
          response == null ? "<network-error>" : response.message(), request.getTiming());
      inFlightRequests.remove(request);
      if (!waitingRequests.isEmpty()) {
        ApiRequest nextRequest = waitingRequests.pop();
        if (inFlightRequests.size() < MAX_INFLIGHT_REQUESTS) {
          inFlightRequests.add(nextRequest);
          enqueueRequest(nextRequest);
        }
      }
      updateState();
    }

    MetricsManager.i.onApiRequestComplete(request);
  }

  void enqueueRequest(ApiRequest apiRequest) {
    inFlightRequests.add(apiRequest);
    apiRequest.getTiming().onRequestSent();
    httpClient.newCall(apiRequest.buildOkRequest()).enqueue(responseCallback);
    updateState();
  }

  private Callback responseCallback = new Callback() {
    @Override
    public void onFailure(Call call, IOException e) {
      handleFailure((ApiRequest) call.request().tag(), null, e);
    }

    @Override
    public void onResponse(Call call, Response response) {
      ApiRequest request = (ApiRequest) call.request().tag();
      handleResponse(request, response);
    }
  };

  private void updateState() {
    eventBus.publish(calculateState());
  }

  private RequestManagerState calculateState() {
    int numInflightRequests;

    synchronized (lock) {
      numInflightRequests = inFlightRequests.size();
      for (ApiRequest request : inFlightRequests) {
        if (request.url().contains("/notifications")) {
          numInflightRequests--;
        }
      }
    }

    return new RequestManagerState(numInflightRequests);
  }

  private static class LoggingInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
      Request request = chain.request();
      if (!DBG) {
        return chain.proceed(request);
      }

      RequestBody requestBody = request.body();
      Connection connection = chain.connection();
      log.info("--> %s %s %s (%d bytes)",
          request.method(),
          request.url(),
          (connection != null ? connection.protocol() : ""),
          (requestBody != null ? requestBody.contentLength() : 0));

      long startNs = System.nanoTime();
      Response response;
      try {
        response = chain.proceed(request);
      } catch (Exception e) {
        log.error("<-- HTTP FAILED: " + e);
        throw e;
      }
      long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

      ResponseBody responseBody = response.body();
      long contentLength = responseBody.contentLength();
      log.info("<-- %d %s %s (%d ms, %d bytes)",
          response.code(),
          (response.message().isEmpty() ? "" : ' ' + response.message()),
          response.request().url(),
          tookMs,
          contentLength != -1 ? contentLength : 0);
      return response;
    }
  }
}
