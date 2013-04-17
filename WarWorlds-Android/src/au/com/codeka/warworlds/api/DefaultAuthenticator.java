package au.com.codeka.warworlds.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the "default" authenticator, which basically passes the auth token to our web
 * service, which verifies it and gives us back a session cookie.
 */
public class DefaultAuthenticator {
    final static Logger log = LoggerFactory.getLogger(DefaultAuthenticator.class);

    public static String authenticate(String authToken) {
        String url = "/login?authToken="+authToken;
        RequestManager.ResultWrapper resp = null;
        try {
            resp = RequestManager.request("GET", url);
            int statusCode = resp.getResponse().getStatusLine().getStatusCode();
            if (statusCode != 200) {
                log.warn("Authentication failure: {}", resp.getResponse().getStatusLine());
                return null;
            }

            String cookie = null;
            HttpEntity entity = resp.getResponse().getEntity();
            if (entity != null) {
                try {
                    InputStream ins = entity.getContent();
                    cookie = new BufferedReader(new InputStreamReader(ins, "utf-8")).readLine();
                } catch (IllegalStateException e) {
                } catch (Exception e) {
                    log.warn("Authentication failure, could got get response body.");
                    return null;
                }
            }

            if (cookie == null) {
                return null;
            }
            return "SESSION="+cookie;
        } catch(ApiException e) {
            log.warn("Authentication failure", e);
            return null;
        } finally {
            if (resp != null) {
                resp.close();
            }
        }
    }
}
