package au.com.codeka.warworlds.api;

public class ApiException extends Exception {
    private static final long serialVersionUID = 1L;

    public ApiException() {
    }

    public ApiException(Throwable cause) {
        super(cause);
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
