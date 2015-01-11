package au.com.codeka.warworlds.api;

import org.apache.http.HttpConnectionMetrics;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.params.BasicHttpParams;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.R;

/** Wraps a pool of connections to the server. */
public class ConnectionPool {
  private static final Log log = new Log("ConnectionPool");
  private boolean verbose;
  private Stack<Connection> mFreeConnections;
  private List<Connection> mBusyConnections;
  private SocketFactory mSocketFactory;
  private String mHost;
  private HostnameVerifier mHostnameVerifier;
  private int mPort;
  private SSLContext mSslContext;

  public ConnectionPool(boolean ssl, String host, int port, boolean verbose) {
    this.verbose = verbose;
    mFreeConnections = new Stack<>();
    mBusyConnections = new ArrayList<>();

    if (ssl) {
      setupRootCa();
    }

    if (ssl) {
      if (mSslContext != null) {
        mSocketFactory = mSslContext.getSocketFactory();
      } else {
        mSocketFactory = SSLSocketFactory.getDefault();
      }
      mHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
    } else {
      mSocketFactory = SocketFactory.getDefault();
    }
    mHost = host;

    mPort = port;
    if (port <= 0) {
      mPort = (ssl ? 443 : 80);
    }
  }

  public int getNumBusyConnections() {
    synchronized(mBusyConnections) {
      return mBusyConnections.size();
    }
  }

  public String getLastUri() {
    synchronized(mBusyConnections) {
      if (mBusyConnections.isEmpty()) {
        return "";
      }

      Connection conn = mBusyConnections.get(0);
      return conn.getLastUri();
    }
  }

  /**
   * Gets an already-open socket or creates a new one.
   * @throws IOException
   * @throws UnknownHostException
   */
  public Connection getConnection(boolean forceCreate)
      throws UnknownHostException, IOException {
    Connection conn = null;
    if (!forceCreate) {
      synchronized (mFreeConnections) {
        if (!mFreeConnections.isEmpty()) {
          conn = mFreeConnections.pop();

          if (verbose) {
            HttpConnectionMetrics metrics = conn.getMetrics();
            log.debug("Got connection [%s] from free pool (%d requests," +
                    " %d responses, %d bytes sent, %d bytes received).",
                conn,
                metrics.getRequestCount(), metrics.getResponseCount(),
                metrics.getSentBytesCount(), metrics.getReceivedBytesCount());
          }
        }
      }
    } else if (verbose) {
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

  public void returnConnection(Connection conn) {
    synchronized (mBusyConnections) {
      mBusyConnections.remove(conn);
    }

    if (!conn.isOpen()) {
      return;
    }

    synchronized (mFreeConnections) {
      mFreeConnections.push(conn);

      if (verbose) {
        HttpConnectionMetrics metrics = conn.getMetrics();
        log.debug("Returned connection [%s] to free pool (%d requests," +
                " %d responses, %d bytes sent, %d bytes received).",
            conn,
            metrics.getRequestCount(), metrics.getResponseCount(),
            metrics.getSentBytesCount(), metrics.getReceivedBytesCount());
      }
    }
  }

  /**
   * Creates a new connection to the server.
   * @throws IOException
   * @throws UnknownHostException
   */
  private Connection createConnection() throws UnknownHostException, IOException {
    Socket s = mSocketFactory.createSocket(mHost, mPort);

    // Verify that the certicate hostname is for war-worlds.com
    // This is due to lack of SNI support in the current SSLSocket.
    if (mHostnameVerifier != null) {
      SSLSocket sslSocket = (SSLSocket) s;
      SSLSession sslSession = sslSocket.getSession();
      if (!mHostnameVerifier.verify(mHost, sslSession)) {
        throw new SSLHandshakeException("Expected " + mHost + ", found "
            + sslSession.getPeerPrincipal());
      }
    }

    BasicHttpParams params = new BasicHttpParams();
    DefaultHttpClientConnection conn = new DefaultHttpClientConnection();
    conn.bind(s, params);

    if (verbose) {
      log.debug("Connection [%s] to %s:%d created.", conn, s.getInetAddress().toString(),
          mPort);
    }
    return new Connection(this, conn);
  }

  /**
   * Loads the Root CA from memory and uses that instead of the system-installed one. This is more
   * secure (because it can't be spoofed if a CA is compromized). It's also compatible with more
   * devices (since not all devices have the RapidSSL CA installed, it seems).
   */
  private void setupRootCa() {
    log.info("Setting up root certificate to our own custom CA");
    try {
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      InputStream ins = App.i.getResources().openRawResource(R.raw.server_keystore);
      Certificate ca;
      try {
        ca = cf.generateCertificate(ins);
      } finally {
        ins.close();
      }

      // Create a KeyStore containing our trusted CAs
      String keyStoreType = KeyStore.getDefaultType();
      KeyStore keyStore = KeyStore.getInstance(keyStoreType);
      keyStore.load(null, null);
      keyStore.setCertificateEntry("ca", ca);

      // Create a TrustManager that trusts the CAs in our KeyStore
      String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
      TrustManagerFactory tmf;
      tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
      tmf.init(keyStore);

      // Create an SSLContext that uses our TrustManager
      mSslContext = SSLContext.getInstance("TLS");
      mSslContext.init(null, tmf.getTrustManagers(), null);
    } catch (Exception e) {
      log.error("Error setting up SSLContext", e);
    }
  }

}
