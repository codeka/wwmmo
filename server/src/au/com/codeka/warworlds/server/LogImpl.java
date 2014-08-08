package au.com.codeka.warworlds.server;

import java.util.logging.Level;
import java.util.logging.Logger;

import au.com.codeka.common.Log;

/**
 * This class works with the Log class to provide a concrete implementation we can use in the
 * server.
 */
public class LogImpl {
    private static final Logger log = Logger.getLogger("wwmmo");

    public static void setup() {
        Log.setImpl(new LogImplImpl());
    }

    private static final Level[] LevelMap = {
        Level.SEVERE,
        Level.WARNING,
        Level.INFO,
        Level.FINE
    };

    private static class LogImplImpl implements Log.LogImpl {
        @Override
        public boolean isLoggable(String tag, int level) {
            return log.isLoggable(LevelMap[level]);
        }

        @Override
        public void write(String tag, int level, String msg) {
            log.log(LevelMap[level], tag + ": " + msg);
        }
    }
}
