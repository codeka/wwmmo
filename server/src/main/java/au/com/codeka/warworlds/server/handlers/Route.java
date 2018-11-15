package au.com.codeka.warworlds.server.handlers;

import com.google.common.base.Preconditions;
import com.google.firebase.database.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

public class Route {
  private Pattern pattern;
  private Class<? extends RequestHandler> handlerClass;
  private String extraOption;

  public Route(@Nonnull String pattern, Class<? extends RequestHandler> handlerClass) {
    this(pattern, handlerClass, null);
  }

  public Route(
      @Nonnull String pattern,
      Class<? extends RequestHandler> handlerClass,
      String extraOption) {
    this.pattern = Pattern.compile(Preconditions.checkNotNull(pattern));
    this.handlerClass = handlerClass;
    this.extraOption = extraOption;
  }

  /**
   * @return The {@link Matcher} that matches the given request, or null if the request doesn't
   * match.
   */
  @Nullable
  public Matcher matches(HttpServletRequest request) {
    String path = request.getPathInfo();
    if (path == null) {
      path = "/";
    }

    Matcher matcher = pattern.matcher(path);
    if (matcher.matches()) {
      return matcher;
    }
    return null;
  }

  public String getExtraOption() {
    return extraOption;
  }

  public RequestHandler createRequestHandler() throws RequestException {
    try {
      return handlerClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RequestException(e);
    }
  }
}
