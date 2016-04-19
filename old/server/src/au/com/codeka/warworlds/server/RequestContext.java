package au.com.codeka.warworlds.server;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

/**
 * This class contains "context" information about the current request, mostly used for error reporting
 * and such.
 */
public class RequestContext {
    public static RequestContext i = new RequestContext();

    private HashMap<Long, Context> mContextMap;

    private RequestContext() {
        mContextMap = new HashMap<Long, Context>();
    }

    /**
     * Gets the current context "name", which will be the request URL for request handlers and the event details
     * for event handlers.
     */
    public String getContextName() {
        return getContext().name;
    }

    public String getQueryString() {
        String value = getContext().queryString;
        return value == null ? "" : value;
    }

    public String getUserAgent() {
        String value = getContext().userAgent;
        return value == null ? "" : value;
    }

    public void setContext(String name) {
        long tid = Thread.currentThread().getId();
        mContextMap.put(tid, new Context(name));
    }
    
    public void setContext(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            userAgent = "";
        }

        long tid = Thread.currentThread().getId();
        mContextMap.put(tid, new Context(request.getRequestURI(),
                userAgent, request.getQueryString()));
    }

    private Context getContext() {
        long tid = Thread.currentThread().getId();
        Context ctx = mContextMap.get(tid);
        if (ctx == null) {
            ctx = Context.Empty;
        }
        return ctx;
    }

    private static class Context {
        public String name;
        public String userAgent;
        public String queryString;

        public static Context Empty = new Context();

        private Context() {
        }
        public Context(String name) {
            this.name = name;
        }
        public Context(String name, String userAgent, String queryString) {
            this.name = name;
            this.userAgent = userAgent;
            this.queryString = queryString;
        }
    }
}
