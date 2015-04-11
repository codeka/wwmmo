package au.com.codeka.warworlds.intel;

import javax.servlet.http.HttpServletResponse;

/**
 * This exception is thrown when you want to pass an error back to the client.
 */
public class RequestException extends Exception {
  private static final long serialVersionUID = 1L;

  private int httpErrorCode;

  public RequestException(int httpErrorCode) {
    super(String.format("HTTP Error: %d", httpErrorCode));
    this.httpErrorCode = httpErrorCode;
  }

  public RequestException(int httpErrorCode, String message) {
    super(String.format(message, httpErrorCode));
    this.httpErrorCode = httpErrorCode;
  }

  public RequestException(int httpErrorCode, String message, Throwable innerException) {
    super(String.format(message, httpErrorCode), innerException);
    this.httpErrorCode = httpErrorCode;
  }

  public void populate(HttpServletResponse response) {
    response.setStatus(httpErrorCode);
  }

  public int getHttpErrorCode() {
    return httpErrorCode;
  }
}
