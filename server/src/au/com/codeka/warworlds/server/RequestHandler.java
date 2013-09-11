package au.com.codeka.warworlds.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jregex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.common.protoformat.PbFormatter;
import au.com.codeka.warworlds.server.ctrl.NotificationController;
import au.com.codeka.warworlds.server.ctrl.SessionController;

import com.google.protobuf.Message;
import com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException;

/**
 * This is the base class for the game's request handlers. It handles some common tasks such as
 * extracting protocol buffers from the request body, and so on.
 */
public class RequestHandler {
    private final Logger log = LoggerFactory.getLogger(RequestHandler.class);
    private HttpServletRequest mRequest;
    private HttpServletResponse mResponse;
    private Matcher mRouteMatcher;
    private Session mSession;
    private String mExtraOption;

    protected String getUrlParameter(String name) {
        try {
            if (mRouteMatcher.isCaptured(name)) {
                return mRouteMatcher.group(name);
            }
        } catch(IllegalArgumentException e) {
        }
        return null;
    }

    protected String getRealm() {
        return getUrlParameter("realm");
    }

    protected String getExtraOption() {
        return mExtraOption;
    }

    public void handle(Matcher matcher, String extraOption, HttpServletRequest request,
                       HttpServletResponse response) {
        mRequest = request;
        mResponse = response;
        mRouteMatcher = matcher;
        mExtraOption = extraOption;

        // start off with status 200, but the handler might change it
        mResponse.setStatus(200);

        RequestException lastException = null;
        for (int retries = 0; retries < 10; retries++) {
            try {
                if (request.getMethod().equals("GET")) {
                    get();
                } else if (request.getMethod().equals("POST")) {
                    post();
                } else if (request.getMethod().equals("PUT")) {
                    put();
                } else if (request.getMethod().equals("DELETE")) {
                    delete();
                } else {
                    throw new RequestException(501);
                }

                return; // break out of the retry loop
            } catch(RequestException e) {
                Throwable cause = e.getCause();
                if (cause instanceof MySQLTransactionRollbackException && supportsRetryOnDeadlock()) {
                    try {
                        Thread.sleep(50 + new Random().nextInt(100));
                    } catch (InterruptedException e1) {
                    }
                    log.warn("Retrying deadlock.", e);
                    lastException = e;
                    continue;
                }
                if (e.getHttpErrorCode() < 500) {
                    log.warn("Unhandled error in URL: "+request.getRequestURI(), e);
                } else {
                    log.error("Unhandled error in URL: "+request.getRequestURI(), e);
                }
                e.populate(mResponse);
                setResponseBody(e.getGenericError());
                return;
            } catch(Exception e) {
                log.error("Unhandled error!", e);
                mResponse.setStatus(500);
                return;
            }
        }

        // if we get here, it's because we exceeded the number of retries.
        log.error("Too many retries: "+request.getRequestURI(), lastException);
        lastException.populate(mResponse);
        setResponseBody(lastException.getGenericError());
    }

    protected void get() throws RequestException {
        throw new RequestException(501);
    }

    protected void put() throws RequestException {
        throw new RequestException(501);
    }

    protected void post() throws RequestException {
        throw new RequestException(501);
    }

    protected void delete() throws RequestException {
        throw new RequestException(501);
    }

    /**
     * You can override this in subclass to indicate that the request supports automatic
     * retry on deadlock.
     */
    protected boolean supportsRetryOnDeadlock() {
        return false;
    }

    protected void setResponseBody(Message pb) {
        if (pb == null) {
            return;
        }

        if (getSessionNoError() != null && getSessionNoError().allowInlineNotifications()) {
            int empireID = getSessionNoError().getEmpireID();
            List<Map<String, String>> notifications = new NotificationController().getRecentNotifications(empireID);
            if (notifications.size() > 0) {
                Messages.NotificationWrapper.Builder notification_wrapper_pb = Messages.NotificationWrapper.newBuilder();
                for (Map<String, String> notification : notifications) {
                    for (String key : notification.keySet()) {
                        String value = notification.get(key);
                        Messages.Notification notification_pb = Messages.Notification.newBuilder()
                                .setName(key)
                                .setValue(value)
                                .build();
                        notification_wrapper_pb.addNotifications(notification_pb);
                    }
                }
                notification_wrapper_pb.setOriginalMessage(pb.toByteString());
                pb = notification_wrapper_pb.build();

                // add a header so the client can know it's a notification wrapper
                mResponse.setHeader("X-Notification-Wrapper", "1");
            }
        }

        if (mRequest.getHeader("Accept") != null) {
            for (String acceptValue : mRequest.getHeader("Accept").split(",")) {
                if (acceptValue.startsWith("text/")) {
                    setResponseBodyText(pb);
                    return;
                } else if (acceptValue.startsWith("application/json")) {
                    setResponseBodyJson(pb);
                    return;
                }
            }
        }

        mResponse.setContentType("application/x-protobuf");
        mResponse.setHeader("Content-Type", "application/x-protobuf");
        try {
            pb.writeTo(mResponse.getOutputStream());
        } catch (IOException e) {
        }
    }

    private void setResponseBodyText(Message pb) {
        mResponse.setContentType("text/plain");
        mResponse.setCharacterEncoding("utf-8");
        try {
            mResponse.getWriter().write(PbFormatter.toJson(pb));
        } catch (IOException e) {
        }
    }

    private void setResponseBodyJson(Message pb) {
        mResponse.setContentType("application/json");
        mResponse.setCharacterEncoding("utf-8");
        try {
            mResponse.getWriter().write(PbFormatter.toJson(pb));
        } catch (IOException e) {
        }
    }

    protected HttpServletRequest getRequest() {
        return mRequest;
    }
    protected HttpServletResponse getResponse() {
        return mResponse;
    }

    protected String getRequestUrl() {
        URI requestURI = null;
        try {
            requestURI = new URI(mRequest.getRequestURL().toString());
        } catch (URISyntaxException e) {
            return null; // should never happen!
        }

        // TODO(deanh): is hard-coding the https part for game.war-worlds.com the best way? no...
        if (requestURI.getHost().equals("game.war-worlds.com")) {
            return "https://game.war-worlds.com"+requestURI.getPath();
        } else {
            return requestURI.toString();
        }
    }

    protected Session getSession() throws RequestException {
        if (mSession == null) {
            String impersonate = getRequest().getParameter("on_behalf_of");

            String sessionCookieValue = "";
            for (Cookie cookie : mRequest.getCookies()) {
                if (cookie.getName().equals("SESSION")) {
                    sessionCookieValue = cookie.getValue();
                    mSession = new SessionController().getSession(sessionCookieValue, impersonate);
                }
            }

            if (mSession == null) {
                throw new RequestException(403);
            }
        }

        return mSession;
    }

    protected Session getSessionNoError() {
        try {
            return getSession();
        } catch(RequestException e) {
            return null;
        }
    }

    @SuppressWarnings({"unchecked"})
    protected <T> T getRequestBody(Class<T> protoBuffFactory) {
        if (mRequest.getHeader("Content-Type").equals("application/json")) {
            return getRequestBodyJson(protoBuffFactory);
        }

        T result = null;
        ServletInputStream ins = null;

        try {
            ins = mRequest.getInputStream();
            Method m = protoBuffFactory.getDeclaredMethod("parseFrom", InputStream.class);
            result = (T) m.invoke(null, ins);
        } catch (Exception e) {
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException e) {
                }
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private <T> T getRequestBodyJson(Class<T> protoBuffFactory) {
        String json = null;

        InputStream ins;
        try {
            ins = mRequest.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(ins));

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line+" ");
            }

            json = sb.toString();
        } catch (Exception e) {
            return null;
        }

        try {
            Method m = protoBuffFactory.getDeclaredMethod("newBuilder");
            Message.Builder builder = (Message.Builder) m.invoke(null);

            PbFormatter.fromJson(json, builder);
            return (T) builder.build();
        } catch (Exception e) {
            return null;
        }
    }
}
