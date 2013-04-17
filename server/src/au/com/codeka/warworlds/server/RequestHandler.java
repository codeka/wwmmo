package au.com.codeka.warworlds.server;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.protoformat.PbFormatter;

import com.google.protobuf.Message;

/**
 * This is the base class for the game's request handlers. It handles some common tasks such as
 * extracting protocol buffers from the request body, and so on.
 */
public class RequestHandler {
    private final Logger log = LoggerFactory.getLogger(RequestHandler.class);
    private HttpServletRequest mRequest;
    private HttpServletResponse mResponse;

    public void handle(HttpServletRequest request,
                       HttpServletResponse response) {
        mRequest = request;
        mResponse = response;

        Message pb = null;
        try {
            if (request.getMethod().equals("GET")) {
                pb = get();
            } else if (request.getMethod().equals("POST")) {
                pb = post();
            } else if (request.getMethod().equals("PUT")) {
                pb = put();
            } else if (request.getMethod().equals("DELETE")) {
                pb = delete();
            } else {
                throw new RequestException(501);
            }
        } catch(RequestException e) {
            log.error("Unhandled error!", e);
            e.populate(mResponse);
            setResponseBody(e.getGenericError());
            return;
        } catch(Exception e) {
            log.error("Unhandled error!", e);
            mResponse.setStatus(500);
            return;
        }

        mResponse.setStatus(200);
        setResponseBody(pb);
    }

    protected Message get() throws RequestException {
        throw new RequestException(501);
    }

    protected Message put() throws RequestException {
        throw new RequestException(501);
    }

    protected Message post() throws RequestException {
        throw new RequestException(501);
    }

    protected Message delete() throws RequestException {
        throw new RequestException(501);
    }

    private void setResponseBody(Message pb) {
        if (pb == null) {
            return;
        }

        for (String acceptValue : mRequest.getHeader("Accept").split(",")) {
            if (acceptValue.startsWith("text/")) {
                setResponseBodyText(pb);
                return;
            } else if (acceptValue.startsWith("application/json")) {
                setResponseBodyJson(pb);
                return;
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
        mResponse.setHeader("Content-Type", "text/plain");
        try {
            mResponse.getWriter().write(PbFormatter.toJson(pb, true));
        } catch (IOException e) {
        }
    }

    private void setResponseBodyJson(Message pb) {
        mResponse.setContentType("application/json");
        mResponse.setHeader("Content-Type", "application/json");
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

    @SuppressWarnings({"unchecked"})
    protected <T> T getRequestBody(Class<T> protoBuffFactory) {
        T result = null;
        ServletInputStream ins = null;

        try {
            ins = mRequest.getInputStream();
            Method m = protoBuffFactory.getDeclaredMethod("parseFrom", InputStream.class);
            result = (T) m.invoke(null, ins);
        } catch (NoSuchMethodException e) {
        } catch (SecurityException e) {
        } catch (IllegalAccessException e) {
        } catch (IllegalArgumentException e) {
        } catch (InvocationTargetException e) {
        } catch (IOException e) {
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
}
