package au.com.codeka.warworlds.api;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.apache.http.HttpClientConnection;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.BasicHttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the main "client" that accesses the War Worlds API.
 */
public class ApiClient {
    final static Logger log = LoggerFactory.getLogger(ApiClient.class);
    private static ConnectionPool sConnectionPool;
    private static URI sBaseUri;
    private static ArrayList<String> sCookies;

    /**
     * Configures the client to talk to the given "baseUri". All URLs will then be relative
     * to this URL. Usually, this will be something like https://warworldsmmo.appspot.com/api/v1
     * 
     * @param baseUri The base URI that all APIs calls are made against.
     */
    public static void configure(URI baseUri) {
        boolean ssl = false;
        if (baseUri.getScheme().equalsIgnoreCase("https")) {
            ssl = true;
        } else if (!baseUri.getScheme().equalsIgnoreCase("http")) {
            // TODO: invalid scheme!!
            log.error("Invalid URI scheme \"{}\", assuming http.", baseUri.getScheme());
        }

        sConnectionPool = new ConnectionPool(ssl, baseUri.getHost(), baseUri.getPort());
        sBaseUri = baseUri;
        sCookies = new ArrayList<String>();

        log.info("Configured to use base URI: {}", baseUri);
    }

    /**
     * Gets the collection of cookies we'll add to all requests (useful for authentication, 
     * or whatever)
     */
    public static List<String> getCookies() {
        return sCookies;
    }

    /**
     * Fetches a raw protocol buffer from the given URL via a HTTP GET.
     * 
     * \param url The URL of the object to fetch, relative to the server root (so for
     *        example, it might be "/motd" and depending on the other properties set up
     *        in the \c ApiClient, this could resolve to something like
     *        "https://warworldsmmo.appspot.com/api/v1/motd"
     * \param protoBuffFactory the class that we want to fetch, this will also determine
     *        the return value of this method.
     */
    @SuppressWarnings({"unchecked", "deprecation"}) /* not deprecated on Android */
    public static <T> T getProtoBuf(String url, Class<T> protoBuffFactory) {
        ResultWrapper res = request("GET", url);
        try {
            HttpResponse resp = res.getResponse();
            if (resp.getStatusLine().getStatusCode() != 200) {
                log.warn("API \"{}\" returned {}", url, resp.getStatusLine());
                return null;
            }

            HttpEntity entity = resp.getEntity();
            if (entity != null) {
                T result = null;

                try {
                    Method m = protoBuffFactory.getDeclaredMethod("parseFrom", InputStream.class);
                    result = (T) m.invoke(null, entity.getContent());

                    entity.consumeContent();
                } catch (Exception e) {
                    // any errors can just be ignored, reallu (return null instead)
                    log.error("Error getting protocol buffer!", e);
                }

                return result;
            }
        } finally {
            res.close();
        }

        return null; // TODO -- this is actually an error as well...
    }

    /**
     * Performs a request with the given method to the given URL. The URL is assumed to be
     * relative to the \c baseUri that was passed in to \c configure().
     * 
     * TODO: move this to some kind of "low-level" class...
     * 
     * @param method The HTTP method (GET, POST, etc)
     * @param url The URL, relative to the \c baseUri we were configured with.
     * @return A \c ResultWrapper representing the server's response (if any)
     */
    public static ResultWrapper request(String method, String url) {
        HttpClientConnection conn = null;

        URI uri = sBaseUri.resolve(url);
        log.debug("Requesting: {}", uri);

        for(int numAttempts = 0; ; numAttempts++) {
            try {
                // Note: we only allow connections from the pool on the first attempt, if
                // requests fail, we force creating a new connection
                conn = sConnectionPool.getConnection(numAttempts > 0);

                String requestUrl = uri.getPath();
                if (uri.getQuery() != null && uri.getQuery() != "") {
                    requestUrl += "?"+uri.getQuery();
                }
                log.debug("requestUrl = "+requestUrl);
                HttpRequest request = new BasicHttpRequest("GET", requestUrl);
                request.addHeader("Accept", "application/x-protobuf");
                request.addHeader("Host", uri.getHost());
                for(String cookie : sCookies) {
                    request.addHeader("Cookie", cookie);
                }
                conn.sendRequestHeader(request);
                conn.flush();

                HttpResponse response = conn.receiveResponseHeader();
                conn.receiveResponseEntity(response);

                return new ResultWrapper(conn, response);
            } catch (Exception e) {
                if (!canRetry(e)) {
                    log.error("Got non-retryable exception making request to "+uri, e);
                    return null;
                } else if (numAttempts == 0) {
                    log.warn("Got retryable exception making request to "+url, e);

                    // Note: the connection doesn't go back in the pool, and we'll close this
                    // one, it's probably no good anyway...
                    try {
                        conn.close();
                    } catch (IOException e1) {
                        // ignore errors
                    }
                } else if (numAttempts >= 5) {
                    log.error("Got "+numAttempts+" retryable exceptions (giving up) making request to"+url, e);
                    return null;
                }
            }
        }
    }

    /**
     * Determines whether the given exception is re-tryable or not.
     */
    private static boolean canRetry(Exception e) {
        return true; // TODO actually check...
    }

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
                        log.debug("Got connection [{}] from free pool.", conn);
                    }
                }
            } else {
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
                log.debug("Connection [{}] returned to free pool.", conn);
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

            log.debug("Connection [{}] created.", conn);
            return conn;
        }
    }
}
