package au.com.codeka.planetrender;

public class TemplateException extends Exception {
    private static final long serialVersionUID = 1L;

    public TemplateException() {
    }

    public TemplateException(Throwable cause) {
        super(cause);
    }

    public TemplateException(String message) {
        super(message);
    }

    public TemplateException(String message, Throwable cause) {
        super(message, cause);
    }
}
