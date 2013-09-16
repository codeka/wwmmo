package au.com.codeka.warworlds.server;

import javax.servlet.http.HttpServletResponse;

import au.com.codeka.common.protobuf.Messages;

/**
 * This exception is thrown when you want to pass an error back to the client.
 */
public class RequestException extends Exception {
    private static final long serialVersionUID = 1L;

    private int mHttpErrorCode;
    private Messages.GenericError mGenericError;

    public RequestException(int httpErrorCode) {
        super(String.format("HTTP Error: %d", httpErrorCode));
        mHttpErrorCode = httpErrorCode;
    }

    public RequestException(int httpErrorCode, String message) {
        super(String.format(message, httpErrorCode));
        mHttpErrorCode = httpErrorCode;
    }

    public RequestException(int httpErrorCode, String message, Throwable innerException) {
        super(String.format(message, httpErrorCode), innerException);
        mHttpErrorCode = httpErrorCode;
    }

    public RequestException(Throwable innerException) {
        super((innerException instanceof RequestException) 
                ? "HTTP Error: "+((RequestException) innerException).mHttpErrorCode
                : "HTTP Error: 500", innerException);

        if (innerException instanceof RequestException) {
            RequestException innerRequestException = (RequestException) innerException;
            mHttpErrorCode = innerRequestException.mHttpErrorCode;
            mGenericError = innerRequestException.mGenericError;
        } else {
            mHttpErrorCode = 500;
        }
    }

    public RequestException(int httpErrorCode, Messages.GenericError.ErrorCode errorCode, String errorMsg) {
        super(errorMsg);

        mHttpErrorCode = httpErrorCode;
        mGenericError = Messages.GenericError.newBuilder()
                                .setErrorCode(errorCode.getNumber())
                                .setErrorMessage(errorMsg)
                                .build();
    }

    public void populate(HttpServletResponse response) {
        response.setStatus(mHttpErrorCode);
    }

    public int getHttpErrorCode() {
        return mHttpErrorCode;
    }

    public Messages.GenericError getGenericError() {
        if (mGenericError == null) {
            mGenericError = Messages.GenericError.newBuilder()
                    .setErrorCode(Messages.GenericError.ErrorCode.UnknownError.getNumber())
                    .setErrorMessage(getMessage())
                    .build();
        }
        return mGenericError;
    }
}
