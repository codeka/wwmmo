package au.com.codeka.warworlds.api;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.apache.http.HttpClientConnection;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.params.BasicHttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the low-level interface for making requests and so on.
 */
public class RequestManager {
    final static Logger log = LoggerFactory.getLogger(RequestManager.class);
    private static ConnectionPool sConnectionPool;
    private static URI sBaseUri;
    private static List<ResponseReceivedHandler> sResponseReceivedHandlers =
            new ArrayList<ResponseReceivedHandler>();

    private static boolean sVerboseLog = true;

    public static void configure(URI baseUri) {
        boolean ssl = false;
        if (baseUri.getScheme().equalsIgnoreCase("https")) {
            ssl = true;
        } else if (!baseUri.getScheme().equalsIgnoreCase("http")) {
            // should never happen
            log.error("Invalid URI scheme \"{}\", assuming http.", baseUri.getScheme());
        }

        sConnectionPool = new ConnectionPool(ssl, baseUri.getHost(), baseUri.getPort());
        sBaseUri = baseUri;

        log.info("Configured to use base URI: {}", baseUri);
    }

    public static URI getBaseUri() {
        return sBaseUri;
    }

    /**
     * Performs a request with the given method to the given URL. The URL is assumed to be
     * relative to the \c baseUri that was passed in to \c configure().
     * 
     * @param method The HTTP method (GET, POST, etc)
     * @param url The URL, relative to the \c baseUri we were configured with.
     * @return A \c ResultWrapper representing the server's response (if any)
     */
    public static ResultWrapper request(String method, String url) throws ApiException {
        return request(method, url, null, null);
    }

    /**
     * Performs a request with the given method to the given URL. The URL is assumed to be
     * relative to the \c baseUri that was passed in to \c configure().
     * 
     * @param method The HTTP method (GET, POST, etc)
     * @param url The URL, relative to the \c baseUri we were configured with.
     * @param extraHeaders a mapping of additional headers to include in the request (e.g.
     *        cookies, etc)
     * @return A \c ResultWrapper representing the server's response (if any)
     */
    public static ResultWrapper request(String method, String url,
            Map<String, List<String>> extraHeaders) throws ApiException {
        return request(method, url, extraHeaders, null);
    }

    /**
     * Performs a request with the given method to the given URL. The URL is assumed to be
     * relative to the \c baseUri that was passed in to \c configure().
     * 
     * @param method The HTTP method (GET, POST, etc)
     * @param url The URL, relative to the \c baseUri we were configured with.
     * @param extraHeaders a mapping of additional headers to include in the request (e.g.
     *        cookies, etc)
     * @return A \c ResultWrapper representing the server's response (if any)
     */
    public static ResultWrapper request(String method, String url,
            Map<String, List<String>> extraHeaders, HttpEntity body) throws ApiException {
        HttpClientConnection conn = null;

        URI uri = sBaseUri.resolve(url);
        if (sVerboseLog) {
            log.debug("Requesting: {}", uri);
        }

        for(int numAttempts = 0; ; numAttempts++) {
            try {
                // Note: we only allow connections from the pool on the first attempt, if
                // requests fail, we force creating a new connection
                conn = sConnectionPool.getConnection(numAttempts > 0);

                String requestUrl = uri.getPath();
                if (uri.getQuery() != null && uri.getQuery() != "") {
                    requestUrl += "?"+uri.getQuery();
                }
                if (sVerboseLog) {
                    log.debug("requestUrl = "+requestUrl);
                }

                BasicHttpRequest request;
                if (body != null) {
                    BasicHttpEntityEnclosingRequest beer =
                            new BasicHttpEntityEnclosingRequest(method, requestUrl);
                    beer.setEntity(body);
                    request = beer;
                } else {
                    request = new BasicHttpRequest(method, requestUrl);
                }

                String host = uri.getHost();
                if (uri.getPort() > 0 && (
                        (uri.getScheme().equals("http") && uri.getPort() != 80) ||
                        (uri.getScheme().equals("https") && uri.getPort() != 443))) {
                    host += ":"+uri.getPort();
                }
                if (sVerboseLog) {
                    log.debug("Adding host header: "+host);
                }
                request.addHeader("Host", host);

                if (extraHeaders != null) {
                    for(String headerName : extraHeaders.keySet()) {
                        for(String headerValue : extraHeaders.get(headerName)) {
                            request.addHeader(headerName, headerValue);
                        }
                    }
                }
                if (body != null) {
                    request.addHeader(body.getContentType());
                    request.addHeader("Content-Length", Long.toString(body.getContentLength()));
                } else if (method.equalsIgnoreCase("PUT") || method.equalsIgnoreCase("POST")) {
                    request.addHeader("Content-Length", "0");
                }

                conn.sendRequestHeader(request);
                if (body != null) {
                    conn.sendRequestEntity((BasicHttpEntityEnclosingRequest) request);
                }
                conn.flush();

                BasicHttpResponse response = (BasicHttpResponse) conn.receiveResponseHeader();
                conn.receiveResponseEntity(response);

                fireResponseReceivedHandlers(request, response);

                return new ResultWrapper(conn, response);
            } catch (Exception e) {
                if (canRetry(e) && numAttempts == 0) {
                    if (sVerboseLog) {
                        log.warn("Got retryable exception making request to "+url, e);
                    }

                    // Note: the connection doesn't go back in the pool, and we'll close this
                    // one, it's probably no good anyway...
                    try {
                        conn.close();
                    } catch (IOException e1) {
                        // ignore errors
                    }
                } else {
                    if (numAttempts >= 5) {
                        log.error("Got "+numAttempts+" retryable exceptions (giving up) making request to"+url, e);
                    } else {
                        log.error("Got non-retryable exception making request to "+uri, e);
                    }

                    throw new ApiException("Error performing "+method+" "+url, e);
                }
            }
        }
    }

    public static void addResponseReceivedHandler(ResponseReceivedHandler handler) {
        sResponseReceivedHandlers.add(handler);
    }

    private static void fireResponseReceivedHandlers(BasicHttpRequest request,
            BasicHttpResponse response) throws RequestRetryException {
        for(ResponseReceivedHandler handler : sResponseReceivedHandlers) {
            handler.onResponseReceived(request, response);
        }
    }

    /**
     * Determines whether the given exception is re-tryable or not.
     */
    private static boolean canRetry(Exception e) {
        if (e instanceof RequestRetryException) {
            return true;
        }

        if (e instanceof ConnectException) {
            return false;
        }

        // may be others that we can't, but we'll just rety everything for now
        return true;
    }

    /**
     * Register this interface to be notified of every HTTP response. You can do this, for example,
     * to check for authentication errors and automatically re-authenticate.
     */
    public interface ResponseReceivedHandler {
        void onResponseReceived(BasicHttpRequest request,
                                BasicHttpResponse response)
                throws RequestRetryException;
    }

    /**
     * Wraps the result of a request that we've made.
     */
    public static class ResultWrapper {
        private HttpResponse mResponse;
        private HttpClientConnection mConnection;

        public ResultWrapper(HttpClientConnection conn, HttpResponse resp) {
            mConnection = conn;
            mResponse = resp;
        }

        public HttpResponse getResponse() {
            return mResponse;
        }

        public void close() {
            // make sure we've finished with the entity...
            try {
                mResponse.getEntity().consumeContent();
            } catch (IOException e) {
                // ignore....
            }

            sConnectionPool.returnConnection(mConnection);
        }
    }

    /**
     * A pool of connections to the server. So we don't have to reconnect over-and-over.
     */
    private static class ConnectionPool {
        private final Logger log = LoggerFactory.getLogger(ConnectionPool.class);
        private Stack<HttpClientConnection> mFreeConnections;
        private List<HttpClientConnection> mBusyConnections;
        private SocketFactory mSocketFactory;
        private String mHost;
        private int mPort;

        public ConnectionPool(boolean ssl, String host, int port) {
            mFreeConnections = new Stack<HttpClientConnection>();
            mBusyConnections = new ArrayList<HttpClientConnection>();
            if (ssl) {
                log.debug("Will create SSL connections");
                mSocketFactory = SSLSocketFactory.getDefault();
            } else {
                mSocketFactory = SocketFactory.getDefault();
            }
            mHost = host;

            mPort = port;
            if (port <= 0) {
                mPort = (ssl ? 443 : 80);
            }
        }

        /**
         * Gets an already-open socket or creates a new one.
         * @throws IOException 
         * @throws UnknownHostException 
         */
        public HttpClientConnection getConnection(boolean forceCreate)
                throws UnknownHostException, IOException {
            HttpClientConnection conn = null;
            if (!forceCreate) {
                synchronized (mFreeConnections) {
                    while (!mFreeConnections.isEmpty()) {
                        conn = mFreeConnections.pop();

                        if (sVerboseLog) {
                            HttpConnectionMetrics metrics = conn.getMetrics();
                            log.debug(String.format("Got connection [%s] from free pool (%d requests," +
                                                    " %d responses, %d bytes sent, %d bytes received).",
                                      conn,
                                      metrics.getRequestCount(), metrics.getResponseCount(),
                                      metrics.getSentBytesCount(), metrics.getReceivedBytesCount()));
                        }
                    }
                }
            } else if (sVerboseLog) {
                log.debug("Didn't look in connection pool: forceCreate = true");
            }

            if (conn == null) {
                conn = createConnection();
            }

            synchronized (mBusyConnections) {
                mBusyConnections.add(conn);
            }

            return conn;
        }

        public void returnConnection(HttpClientConnection conn) {
            synchronized (mBusyConnections) {
                mBusyConnections.remove(conn);
            }

            synchronized (mFreeConnections) {
                mFreeConnections.push(conn);

                if (sVerboseLog) {
                    HttpConnectionMetrics metrics = conn.getMetrics();
                    log.debug(String.format("Returned connection [%s] to free pool (%d requests," +
                                            " %d responses, %d bytes sent, %d bytes received).",
                              conn,
                              metrics.getRequestCount(), metrics.getResponseCount(),
                              metrics.getSentBytesCount(), metrics.getReceivedBytesCount()));
                }
            }
        }

        /**
         * Creates a new connection to the server.
         * @throws IOException 
         * @throws UnknownHostException 
         */
        private HttpClientConnection createConnection() throws UnknownHostException, IOException {
            Socket s = mSocketFactory.createSocket(mHost, mPort);

            BasicHttpParams params = new BasicHttpParams();
            DefaultHttpClientConnection conn = new DefaultHttpClientConnection();
            conn.bind(s, params);

            if (sVerboseLog) {
                log.debug(String.format("Connection [%s] to %s:%d created.",
                                        conn, s.getInetAddress().toString(), mPort));
            }
            return conn;
        }
    }
}
