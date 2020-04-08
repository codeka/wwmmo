package au.com.codeka.warworlds.server;

import com.google.common.base.Strings;

import javax.servlet.http.HttpServletRequest;

/**
 * This class contains "context" information about the current request, mostly used for error reporting
 * and such.
 */
public class RequestContext {
  public static RequestContext i = new RequestContext();

  private ThreadLocal<Context> context;

  private RequestContext() {
    context = new ThreadLocal<>();
  }

  /**
   * Gets the current context "name", which will be the request URL for request handlers and the
   * event details for event handlers.
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

  public String getIpAddress() {
    String value = getContext().remoteAddr;
    return value == null ? "" : value;
  }

  public int getEmpireId() {
    return getContext().empireID;
  }

  public void setContext(String name) {
    context.set(new Context(name));
  }

  public void setContext(HttpServletRequest request, Session session) {
    String userAgent = request.getHeader("User-Agent");
    if (userAgent == null) {
      userAgent = "";
    }

    int empireId = 0;
    if (session != null) {
      empireId = session.getEmpireID();
    }

    String remoteAddr = request.getRemoteAddr();
    String realIp = request.getHeader("X-Real-IP");
    if (!Strings.isNullOrEmpty(realIp)) {
      remoteAddr = realIp;
    }

    context.set(
        new Context(
            request.getRequestURI(),
            remoteAddr,
            empireId,
            userAgent,
            request.getQueryString()));
  }

  private Context getContext() {
    Context ctx = context.get();
    if (ctx == null) {
      ctx = Context.Empty;
    }
    return ctx;
  }

  private static class Context {
    public String name;
    public String userAgent;
    public String queryString;
    public String remoteAddr;
    public int empireID;

    public static Context Empty = new Context();

    private Context() {
    }

    public Context(String name) {
      this.name = name;
    }

    public Context(
        String name,
        String remoteAddr,
        int empireID,
        String userAgent,
        String queryString) {
      this.name = name;
      this.remoteAddr = remoteAddr;
      this.empireID = empireID;
      this.userAgent = userAgent;
      this.queryString = queryString;
    }
  }
}
