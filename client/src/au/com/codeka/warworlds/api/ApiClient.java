package au.com.codeka.warworlds.api;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ByteArrayEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.protobuf.Message;

/**
 * This is the main "client" that accesses the War Worlds API.
 */
public class ApiClient {
    final static Logger log = LoggerFactory.getLogger(ApiClient.class);
    private static ArrayList<String> sCookies;

    /**
     * Configures the client to talk to the given "baseUri". All URLs will then be relative
     * to this URL. Usually, this will be something like https://warworldsmmo.appspot.com/api/v1
     * 
     * @param baseUri The base URI that all APIs calls are made against.
     */
    public static void configure(URI baseUri) {
        log.info("Resetting cookies... configuring baseUri: "+baseUri);
        sCookies = new ArrayList<String>();
        RequestManager.configure(baseUri);
    }

    public static void impersonate(String user) {
        RequestManager.impersonate(user);
    }

    public static URI getBaseUri() {
        return RequestManager.getBaseUri();
    }

    /**
     * Gets the collection of cookies we'll add to all requests (useful for authentication, 
     * or whatever)
     */
    public static List<String> getCookies() {
        return sCookies;
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
    public static <T> T getProtoBuf(String url, Class<T> protoBuffFactory) throws ApiException {
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
    public static <T> T putProtoBuf(String url, Message pb, Class<T> protoBuffFactory)
            throws ApiException {
        return putOrPostProtoBuff("PUT", url, pb, protoBuffFactory);
    }

    /**
     * Uses the "POST" HTTP method to post a protocol buffer at the given URL.
     */
    public static <T> T postProtoBuf(String url, Message pb, Class<T> protoBuffFactory)
            throws ApiException {
        return putOrPostProtoBuff("POST", url, pb, protoBuffFactory);
    }

    private static <T> T putOrPostProtoBuff(String method, String url, Message pb, 
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
    public static <T> T delete(String url, Message pb, Class<T> protoBuffFactory)
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
        if (!sCookies.isEmpty()) {
            headers.put("Cookie", sCookies);
        } else {
            log.warn("Cookies collection is empty, possible error!");
        }
        ArrayList<String> accept = new ArrayList<String>();
        accept.add("application/x-protobuf");
        headers.put("Accept", accept);

        return headers;
    }

    /**
     * Parses the response from a request and returns the protocol buffer returned therein 
     * (if any).
     */
    @SuppressWarnings({"unchecked"})
    public static <T> T parseResponseBody(HttpResponse resp, Class<T> protoBuffFactory) {
        HttpEntity entity = resp.getEntity();
        if (entity != null) {
            T result = null;

            try {
                Method m = protoBuffFactory.getDeclaredMethod("parseFrom", InputStream.class);
                result = (T) m.invoke(null, entity.getContent());

                entity.consumeContent();
            } catch (Exception e) {
                return null;
            }

            return result;
        }

        return null;
    }
}
