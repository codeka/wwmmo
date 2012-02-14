package au.com.codeka.warworlds.api;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        sCookies = new ArrayList<String>();
        RequestManager.configure(baseUri);
    }

    /**
     * Gets the collection of cookies we'll add to all requests (useful for authentication, 
     * or whatever)
     */
    public static List<String> getCookies() {
        return sCookies;
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
    @SuppressWarnings({"unchecked", "deprecation"}) /* not deprecated on Android */
    public static <T> T getProtoBuf(String url, Class<T> protoBuffFactory) {
        TreeMap<String, List<String>> headers = new TreeMap<String, List<String>>();
        if (!sCookies.isEmpty()) {
            headers.put("Cookie", sCookies);
        }
        ArrayList<String> accept = new ArrayList<String>();
        accept.add("application/x-protobuf");
        headers.put("Accept", accept);

        RequestManager.ResultWrapper res = RequestManager.request("GET", url, headers);
        try {
            HttpResponse resp = res.getResponse();
            if (resp.getStatusLine().getStatusCode() != 200) {
                log.warn("API \"{}\" returned {}", url, resp.getStatusLine());
                return null;
            }

            HttpEntity entity = resp.getEntity();
            if (entity != null) {
                T result = null;

                try {
                    Method m = protoBuffFactory.getDeclaredMethod("parseFrom", InputStream.class);
                    result = (T) m.invoke(null, entity.getContent());

                    entity.consumeContent();
                } catch (Exception e) {
                    // any errors can just be ignored, reallu (return null instead)
                    log.error("Error getting protocol buffer!", e);
                }

                return result;
            }
        } finally {
            res.close();
        }

        return null; // TODO -- this is actually an error as well...
    }
}
