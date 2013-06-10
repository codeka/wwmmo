package au.com.codeka.warworlds.api;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.Util;
import au.com.codeka.warworlds.model.Realm;

/**
 * This is the "default" authenticator, which basically passes the auth token to our web
 * service, which verifies it and gives us back a session cookie.
 */
public class DefaultAuthenticator {
    final static Logger log = LoggerFactory.getLogger(DefaultAuthenticator.class);

    public static String authenticate(String authToken, Realm realm) {
        String url = realm.getBaseUrl().resolve("login?authToken="+authToken).toString();

        String impersonate = Util.getProperties().getProperty("user.on_behalf_of", null);
        if (impersonate != null) {
            url += "&impersonate="+impersonate;
        }

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
