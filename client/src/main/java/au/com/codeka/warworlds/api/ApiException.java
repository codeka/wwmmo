package au.com.codeka.warworlds.api;

import com.squareup.okhttp.Response;

import java.net.SocketException;

import au.com.codeka.common.protobuf.Messages;

public class ApiException extends Exception {
    private static final long serialVersionUID = 1L;

    private int mHttpStatusCode;
    private String mHttpStatusMessage;
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
    public static void checkResponse(Response resp) throws ApiException {
        if (!resp.isSuccessful()) {
            ApiException.throwApiException(resp);
        }
    }

    /**
     * Given a {@link Response} that is in error (HTTP status code 400 and above) throws a new
     * {@link ApiException} with details about the error.
     */
    public static void throwApiException(Response resp) throws ApiException {
        ApiException ex = new ApiException();
        Messages.GenericError err = RequestManager.parseResponse(resp, Messages.GenericError.class);
        if (err != null) {
            ex.mServerErrorCode = err.getErrorCode();
            ex.mServerErrorMessage = err.getErrorMessage();
        }

        ex.mHttpStatusCode = resp.code();
        ex.mHttpStatusMessage = resp.message();
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

    public int getHttpStatusCode() {
        return mHttpStatusCode;
    }

    public String getHttpStatusMessage() {
        return mHttpStatusMessage;
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
        if (mHttpStatusMessage != null) {
            str += String.format("%d %s\r\n", mHttpStatusCode, mHttpStatusMessage);
        }
        if (mServerErrorMessage != null) {
            str += "Server Error: " + mServerErrorMessage + "\r\n";
        }
        return str + super.toString();
    }
}
