package au.com.codeka.warworlds.api;


import org.apache.http.HttpClientConnection;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpResponse;

import java.io.IOException;

/** Represents a single {@link HttpClientConnection}, a persistent connection to the server. */
class Connection {
  private final ConnectionPool connectionPool;
  private final HttpClientConnection httpClientConnection;
  private String lastUri;

  public Connection(ConnectionPool connectionPool, HttpClientConnection httpClientConnection) {
    this.connectionPool = connectionPool;
    this.httpClientConnection = httpClientConnection;
    lastUri = "";
  }

  public BasicHttpResponse sendRequest(HttpRequest request, HttpEntity body)
      throws HttpException, IOException {
    lastUri = request.getRequestLine().getUri();

    httpClientConnection.sendRequestHeader(request);
    if (body != null) {
      httpClientConnection.sendRequestEntity((BasicHttpEntityEnclosingRequest) request);
    }
    httpClientConnection.flush();

    BasicHttpResponse response = (BasicHttpResponse) httpClientConnection.receiveResponseHeader();
    httpClientConnection.receiveResponseEntity(response);

    return response;
  }

  public ConnectionPool getConnectionPool() {
    return connectionPool;
  }

  public HttpConnectionMetrics getMetrics() {
    return httpClientConnection.getMetrics();
  }

  public String getLastUri() {
    return lastUri;
  }

  public boolean isOpen() {
    return httpClientConnection.isOpen();
  }

  public void close() {
    try {
      httpClientConnection.close();
    } catch (IOException e) {
    }
  }
}
