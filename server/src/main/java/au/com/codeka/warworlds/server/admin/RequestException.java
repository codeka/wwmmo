package au.com.codeka.warworlds.server.admin;

import java.util.Locale;

import javax.servlet.http.HttpServletResponse;

/**
 * This exception is thrown when you want to pass an error back to the client.
 */
public class RequestException extends Exception {
  private static final long serialVersionUID = 1L;

  private int errorCode;

  public RequestException(int httpErrorCode) {
    super(String.format(Locale.US, "HTTP Error: %d", httpErrorCode));
    errorCode = httpErrorCode;
  }

  public RequestException(int httpErrorCode, String message) {
    super(String.format(message, httpErrorCode));
    errorCode = httpErrorCode;
  }

  public RequestException(int httpErrorCode, String message, Throwable innerException) {
    super(String.format(message, httpErrorCode), innerException);
    errorCode = httpErrorCode;
  }

  public RequestException(Throwable innerException) {
    super(getExceptionDescription(innerException), innerException);

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    RequestException reqExc = findInnerException(innerException, RequestException.class);
    if (reqExc != null) {
      errorCode = reqExc.errorCode;
    } else {
      errorCode = 500;
    }
  }

  private static String getExceptionDescription(Throwable e) {
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    RequestException reqExc = findInnerException(e, RequestException.class);

    if (reqExc != null) {
      return "HTTP Error: " + reqExc.errorCode;
    }

    return "Unknown Exception";
  }

  @SuppressWarnings("unchecked")
  private static <T extends Exception> T findInnerException(Throwable e, Class<T> exceptionType) {
    while (e != null) {
      if (e.getClass().equals(exceptionType)) {
        return (T) e;
      }
      e = e.getCause();
    }
    return null;
  }

  public void populate(HttpServletResponse response) {
    response.setStatus(errorCode);
  }

  public int getErrorCode() {
    return errorCode;
  }
}
