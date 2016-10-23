package au.com.codeka.warworlds.api;

import android.content.Context;

import com.google.common.collect.Lists;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.CertificatePinner;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.File;
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
  public static final RequestManager i = new RequestManager();
  public static final EventBus eventBus = new EventBus();

  private static final Log log = new Log("RequestManager");
  private static final boolean DBG = true;

  private static final int MAX_INFLIGHT_REQUESTS = 8;
  private static final int MAX_CACHE_SIZE = 50 * 1024 * 1024; // 50MB

  private final OkHttpClient httpClient;
  private final Set<ApiRequest> inFlightRequests = new HashSet<>();

  // We use a stack because the most recent request is likely the most important (since it's usually
  // going to be in response to the most recent UI interaction).
  private final Stack<ApiRequest> waitingRequests = new Stack<>();

  private final Object lock = new Object();

  private RequestManager() {
    httpClient = new OkHttpClient();

    CertificatePinner certificatePinner = new CertificatePinner.Builder()
        // LetsEncrypt's root certificate:
        .add("game.war-worlds.com", "sha1/2ptSqHcRadMTGKVn4dybH0S1s1w=")
        // RapidSSL's certificate:
        .add("game.war-worlds.com", "sha1/7pk8GWNOfZdQYlUboW2S4aXDNls=")
        // The below are obsolete as of May, 2016:
        .add("game.war-worlds.com", "sha1/DQYffq7Bm+6ChL1D0VUBHAd3k6g=")
        .add("game.war-worlds.com", "sha1/o5OZxATDsgmwgcIfIWIneMJ0jkw=")
        .build();
    httpClient.setCertificatePinner(certificatePinner);

    httpClient.getDispatcher().setMaxRequests(MAX_INFLIGHT_REQUESTS);
    httpClient.getDispatcher().setMaxRequestsPerHost(MAX_INFLIGHT_REQUESTS);
  }

  /** Sets up the request manager, once we've got a context. Initializes the cache and so on. */
  public void setup(Context context) {
    if (httpClient.getCache() == null) {
      try {
        File cacheDir = new File(context.getCacheDir(), "api");
        if (!cacheDir.mkdirs()) {
          // Ignore, directory (probably) already exists
        }
        httpClient.setCache(new Cache(cacheDir, MAX_CACHE_SIZE));
      } catch (IOException e) {
        log.error("Error setting up HTTP cache.", e);
      }
    }
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
        waitingRequests.push(apiRequest);
      }
      updateState();
    }
  }

  public boolean sendRequestSync(ApiRequest apiRequest) {
    if (DBG) log.info(">> %s", apiRequest);
    apiRequest.getTiming().onRequestSent();
    try {
      Response resp = httpClient.newCall(apiRequest.buildOkRequest()).execute();
      apiRequest.handleResponse(resp);
      return true;
    } catch (IOException e) {
      log.error("Error in sendRequestSync.", e);
      return false;
    }
  }

  public void addInterceptor(Interceptor interceptor) {
    httpClient.interceptors().add(interceptor);
  }

  public void removeInterceptor(Interceptor interceptor) {
    httpClient.interceptors().remove(interceptor);
  }

  private void handleResponse(ApiRequest request, Response response) {
    requestComplete(request, response);
    if (!response.isSuccessful()) {
      handleFailure(request, response, null);
    } else {
      request.handleResponse(response);
    }
  }

  /**
   * Handles a failed request. Either response or the exception will set, depending on whether the
   * error was network-related or API related.
   */
  private void handleFailure(ApiRequest request, @Nullable Response response,
      @Nullable IOException e) {
    requestComplete(request, response);
    if (e != null) {
      log.error("Error in request: %s", request, e);
    } else if (response != null) {
      log.warning("Error in response: %d %s", response.code(), response.message());
    } else {
      throw new IllegalStateException("One of response or e should be non-null.");
    }

    request.handleError(response, e);
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
  }

  void enqueueRequest(ApiRequest apiRequest) {
    inFlightRequests.add(apiRequest);
    if (DBG) log.info(">> %s", apiRequest);
    apiRequest.getTiming().onRequestSent();
    httpClient.newCall(apiRequest.buildOkRequest()).enqueue(responseCallback);
    updateState();
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
}
