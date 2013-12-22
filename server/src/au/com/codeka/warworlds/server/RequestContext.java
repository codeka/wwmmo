package au.com.codeka.warworlds.server;

import java.util.HashMap;

/**
 * This class contains "context" information about the current request, mostly used for error reporting
 * and such.
 */
public class RequestContext {
    public static RequestContext i = new RequestContext();

    private HashMap<Long, String> mContextNames;

    private RequestContext() {
        mContextNames = new HashMap<Long, String>();
    }

    /**
     * Gets the current context "name", which will be the request URL for request handlers and the event details
     * for event handlers.
     */
    public String getContextName() {
        long tid = Thread.currentThread().getId();
        return mContextNames.get(tid);
    }

    public void setContextName(String name) {
        long tid = Thread.currentThread().getId();
        mContextNames.put(tid, name);
    }
}
