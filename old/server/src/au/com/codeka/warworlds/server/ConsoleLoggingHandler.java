package au.com.codeka.warworlds.server;

import java.util.logging.ConsoleHandler;

/** A {@link ConsoleHandler} that logs to System.out, rather than System.err. */
public class ConsoleLoggingHandler extends ConsoleHandler {
    public ConsoleLoggingHandler() {
        setOutputStream(System.out);
    }
}
