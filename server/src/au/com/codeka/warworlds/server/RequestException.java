package au.com.codeka.warworlds.server;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.servlet.http.HttpServletResponse;

import au.com.codeka.common.protobuf.Messages;

/**
 * This exception is thrown when you want to pass an error back to the client.
 */
public class RequestException extends Exception {
  private static final long serialVersionUID = 1L;

  private int httpErrorCode;
  private Messages.GenericError genericError;
  private List<Object> extraObjects;

  public RequestException(int httpErrorCode) {
    super(String.format(Locale.ENGLISH, "HTTP Error: %d", httpErrorCode));
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

  public RequestException(int httpErrorCode, Messages.GenericError.ErrorCode errorCode, String errorMsg) {
    super(errorMsg);

    this.httpErrorCode = httpErrorCode;
    genericError = Messages.GenericError.newBuilder()
        .setErrorCode(errorCode.getNumber())
        .setErrorMessage(errorMsg)
        .build();
  }

  public RequestException(Throwable innerException, Object... extraObjects) {
    super(getExceptionDescription(innerException), innerException);

    RequestException reqExc = findInnerException(innerException, RequestException.class);
    if (reqExc != null) {
      httpErrorCode = reqExc.httpErrorCode;
      genericError = reqExc.genericError;
      this.extraObjects = reqExc.extraObjects;
    } else {
      httpErrorCode = 500;
    }

    if (extraObjects != null) {
      for (Object extraObject : extraObjects) {
        with(extraObject);
      }
    }
  }

  public RequestException with(Object extraObject) {
    if (extraObjects == null) {
      extraObjects = new ArrayList<>();
    }
    extraObjects.add(extraObject);
    return this;
  }

  private static String getExceptionDescription(Throwable e) {
    RequestException reqExc = findInnerException(e, RequestException.class);
    if (reqExc != null) {
      return "HTTP Error: " + reqExc.httpErrorCode;
    }

    SQLException sqlExc = findInnerException(e, SQLException.class);
    if (sqlExc != null) {
      return "SQL Error: " + sqlExc.getErrorCode();
    }

    return e.getMessage();
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
    response.setStatus(httpErrorCode);
  }

  public int getHttpErrorCode() {
    return httpErrorCode;
  }

  public Messages.GenericError getGenericError() {
    if (genericError == null) {
      genericError = Messages.GenericError.newBuilder()
          .setErrorCode(Messages.GenericError.ErrorCode.UnknownError.getNumber())
          .setErrorMessage(getMessage())
          .build();
    }
    return genericError;
  }

  @Override
  public String toString() {
    if (extraObjects != null) {
      StringBuilder sb = new StringBuilder();
      sb.append(super.toString());
      sb.append("\n");

      for (Object extraObject : extraObjects) {
        sb.append("\n");
        sb.append(extraObject);
        sb.append("\n");
      }

      return sb.toString();
    }

    return super.toString();
  }
}
