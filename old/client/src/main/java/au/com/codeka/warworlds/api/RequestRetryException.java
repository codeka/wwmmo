package au.com.codeka.warworlds.api;

/**
 * You can explicitly throw this exception to retry a \c RequestManager request.
 * The only places this makes sense is when implementing
 * \c RequestManager.ResponseReceivedHandler.
 */
public class RequestRetryException extends Exception {
    private static final long serialVersionUID = 1L;
}
