package au.com.codeka.warworlds.api;

import java.net.SocketException;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;

import au.com.codeka.common.protobuf.Messages;

public class ApiException extends Exception {
    private static final long serialVersionUID = 1L;

    private StatusLine mHttpStatusLine;
    private int mServerErrorCode;
    private String mServerErrorMessage;

    public ApiException() {
    }

    public ApiException(Throwable cause) {
        super(cause);
    }

    public ApiException(String message) {
        super(message);
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Checks the given \c HttpResponse and throws an \c ApiException if needed.
     */
    public static void checkResponse(HttpResponse resp) throws ApiException {
        int statusCode = resp.getStatusLine().getStatusCode();
        if (statusCode < 200 || statusCode > 299) {
            ApiException.throwApiException(resp);
        }
    }

    /**
     * Given an \c HttpResponse that represents an error (i.e. status code > 299) we'll
     * throw an ApiException.
     */
    public static void throwApiException(HttpResponse resp) throws ApiException {
        ApiException ex = new ApiException();

        Messages.GenericError err = ApiClient.parseResponseBody(resp, Messages.GenericError.class);
        if (err != null) {
            ex.mServerErrorCode = err.getErrorCode();
            ex.mServerErrorMessage = err.getErrorMessage();
        }

        ex.mHttpStatusLine = resp.getStatusLine();
        throw ex;
    }

    /** Returns true if this exception was thrown as the result of a network error. */
    public boolean networkError() {
        Throwable cause = this.getCause();
        while (cause != null) {
            if (cause instanceof SocketException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    public StatusLine getHttpStatusLine() {
        return mHttpStatusLine;
    }

    public int getServerErrorCode() {
        return mServerErrorCode;
    }

    public String getServerErrorMessage() {
        return mServerErrorMessage;
    }

    @Override
    public String toString() {
        String str = "";
        if (mHttpStatusLine != null) {
            str += mHttpStatusLine.toString() + "\r\n";
        }
        if (mServerErrorMessage != null) {
            str += "Server Error: " + mServerErrorMessage + "\r\n";
        }
        return str + super.toString();
    }
}
