package au.com.codeka.warworlds.api;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;

import warworlds.Warworlds.GenericError;

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

        GenericError err = ApiClient.parseResponseBody(resp, GenericError.class);
        if (err != null) {
            ex.mServerErrorCode = err.getErrorCode();
            ex.mServerErrorMessage = err.getErrorMessage();
        }

        ex.mHttpStatusLine = resp.getStatusLine();
        throw ex;
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
}
