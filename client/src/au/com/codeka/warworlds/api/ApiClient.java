package au.com.codeka.warworlds.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ByteArrayEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import au.com.codeka.common.model.Model;
import au.com.codeka.common.model.Notification;
import au.com.codeka.common.model.NotificationWrapper;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.Notifications;

import com.squareup.wire.ByteString;
import com.squareup.wire.Message;

/**
 * This is the main "client" that accesses the War Worlds API.
 */
public class ApiClient {
    final static Logger log = LoggerFactory.getLogger(ApiClient.class);

    public static void impersonate(String user) {
        RequestManager.impersonate(user);
    }

    /**
     * Fetches an XML document from the given URL.
     */
    public static Document getXml(String url) throws ApiException {
        Map<String, List<String>> headers = getHeaders();
        headers.get("Accept").add("text/xml"); // we also accept XML, obviously...

        RequestManager.ResultWrapper res = RequestManager.request("GET", url, headers);

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setValidating(false);

        try {
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            return builder.parse(res.getResponse().getEntity().getContent());
        } catch (ParserConfigurationException e) {
            throw new ApiException(e);
        } catch (IllegalStateException e) {
            throw new ApiException(e);
        } catch (SAXException e) {
            throw new ApiException(e);
        } catch (IOException e) {
            throw new ApiException(e);
        } finally {
            res.close();
        }
    }

    /**
     * Fetches a raw protocol buffer from the given URL via a HTTP GET.
     * 
     * \param url The URL of the object to fetch, relative to the server root (so for
     *        example, it might be "/motd" and depending on the other properties set up
     *        in the \c ApiClient, this could resolve to something like
     *        "https://warworldsmmo.appspot.com/api/v1/motd"
     * \param protoBuffFactory the class that we want to fetch, this will also determine
     *        the return value of this method.
     */
    public static <T extends Message> T getProtoBuf(String url, Class<T> protoBuffFactory) throws ApiException {
        Map<String, List<String>> headers = getHeaders();

        RequestManager.ResultWrapper res = RequestManager.request("GET", url, headers);
        try {
            HttpResponse resp = res.getResponse();
            ApiException.checkResponse(resp);

            return parseResponseBody(resp, protoBuffFactory);
        } finally {
            res.close();
        }
    }

    /**
     * Uses the "PUT" HTTP method to put a protocol buffer at the given URL. This is useful when
     * you don't expect a response (other than "201", success)
     */
    public static boolean putProtoBuf(String url, Message pb) throws ApiException {
        return putOrPostProtoBuf("PUT", url, pb);
    }

    /**
     * Uses the "POST" HTTP method to post a protocol buffer at the given URL. This is useful when
     * you don't expect a response (other than "200", success)
     */
    public static boolean postProtoBuf(String url, Message pb) throws ApiException {
        return putOrPostProtoBuf("POST", url, pb);
    }

    /**
     * Uses the "PUT" or "POST" HTTP method to put or post a protocol buffer at the given URL.
     * This is useful when you don't expect a response (other than "2xx", success)
     */
    private static boolean putOrPostProtoBuf(String method, String url, Message pb)
            throws ApiException {
        Map<String, List<String>> headers = getHeaders();

        ByteArrayEntity body = null;
        if (pb != null) {
            body = new ByteArrayEntity(pb.toByteArray());
            body.setContentType("application/x-protobuf");
        }

        RequestManager.ResultWrapper res = RequestManager.request(method, url, headers, body);
        try {
            HttpResponse resp = res.getResponse();
            ApiException.checkResponse(resp);

            return true;
        } finally {
            res.close();
        }
    }

    /**
     * Uses the "PUT" HTTP method to put a protocol buffer at the given URL.
     */
    public static <T extends Message> T putProtoBuf(String url, Message pb, Class<T> protoBuffFactory)
            throws ApiException {
        return putOrPostProtoBuff("PUT", url, pb, protoBuffFactory);
    }

    /**
     * Uses the "POST" HTTP method to post a protocol buffer at the given URL.
     */
    public static <T extends Message> T postProtoBuf(String url, Message pb, Class<T> protoBuffFactory)
            throws ApiException {
        return putOrPostProtoBuff("POST", url, pb, protoBuffFactory);
    }

    private static <T extends Message> T putOrPostProtoBuff(String method, String url, Message pb, 
            Class<T> protoBuffFactory) throws ApiException {
        Map<String, List<String>> headers = getHeaders();

        ByteArrayEntity body = null;
        if (pb != null) {
            body = new ByteArrayEntity(pb.toByteArray());
            body.setContentType("application/x-protobuf");
        }

        RequestManager.ResultWrapper res = RequestManager.request(method, url, headers, body);
        try {
            HttpResponse resp = res.getResponse();
            ApiException.checkResponse(resp);

            return parseResponseBody(resp, protoBuffFactory);
        } finally {
            res.close();
        }
    }

    /**
     * Sends a HTTP 'DELETE' to the given URL.
     */
    public static void delete(String url) throws ApiException {
        Map<String, List<String>> headers = getHeaders();

        RequestManager.ResultWrapper res = RequestManager.request("DELETE", url, headers);
        try {
            HttpResponse resp = res.getResponse();
            ApiException.checkResponse(resp);
        } finally {
            res.close();
        }
    }

    /**
     * Sends a HTTP 'DELETE' to the given URL.
     */
    public static <T extends Message> T delete(String url, Message pb, Class<T> protoBuffFactory)
            throws ApiException {
        return putOrPostProtoBuff("DELETE", url, pb, protoBuffFactory);
    }

    /**
     * Sends a HTTP 'DELETE' to the given URL.
     */
    public static void delete(String url, Message pb)
            throws ApiException {
        putOrPostProtoBuf("DELETE", url, pb);
    }

    /**
     * Gets the headers that we'll add to all of our requests.
     */
    private static Map<String, List<String>> getHeaders() {
        TreeMap<String, List<String>> headers = new TreeMap<String, List<String>>();

        ArrayList<String> accept = new ArrayList<String>();
        accept.add("application/x-protobuf");
        headers.put("Accept", accept);

        return headers;
    }

    /**
     * Parses the response from a request and returns the protocol buffer returned therein 
     * (if any).
     */
    
    public static <T extends Message> T parseResponseBody(HttpResponse resp, Class<T> protoBuffFactory) {
        HttpEntity entity = resp.getEntity();
        if (entity != null) {
            T result = null;

            boolean isNotificationWrapper = false;
            Header notificationWrapperHeader = resp.getFirstHeader("X-Notification-Wrapper");
            if (notificationWrapperHeader != null) {
                isNotificationWrapper = notificationWrapperHeader.getValue().equals("1");
            }

            if (isNotificationWrapper) {
                NotificationWrapper pb = extractBody(entity, NotificationWrapper.class);
                for (Notification notification_pb : pb.notifications) {
                    log.info("got inline-notification: "+notification_pb.name);
                    Notifications.displayNotfication(App.i, notification_pb.name, notification_pb.value);
                }

                if (protoBuffFactory != null) {
                    result = extractBody(pb.original_message, protoBuffFactory);
                }
            } else if (protoBuffFactory != null) {
                result = extractBody(entity, protoBuffFactory);
            }

            return result;
        }

        return null;
    }

    private static <T extends Message> T extractBody(HttpEntity entity, Class<T> protoBuffFactory)  {
        try {
            return (T) Model.wire.parseFrom(entity.getContent(), protoBuffFactory);
        } catch (IOException e) {
            log.error("Error parsing response: "+protoBuffFactory.getName(), e);
            return null;
        }
    }

    private static <T extends Message> T extractBody(ByteString bs, Class<T> protoBuffFactory) {
        try {
            return (T) Model.wire.parseFrom(bs.toByteArray(), protoBuffFactory);
        } catch (IOException e) {
            log.error("Error parsing response: "+protoBuffFactory.getName(), e);
            return null;
        }
    }
}
