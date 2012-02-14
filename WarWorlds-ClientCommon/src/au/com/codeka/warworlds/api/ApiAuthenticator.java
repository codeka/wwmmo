package au.com.codeka.warworlds.api;

import org.apache.http.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.api.ApiClient.ResultWrapper;

/**
 * This class manages authentication with the App Engine app.
 */
public class ApiAuthenticator {
    final static Logger log = LoggerFactory.getLogger(ApiAuthenticator.class);

    /**
     * Attempts to authenticate with an AppEngine server, given an authToken (usually taken
     * from the accounts in an Android).
     * 
     * @param authToken The authentication token, as returned by Android's account manager
     * @return An authentication cookie that you can pass to later App Engine requests.
     */
    public static String authenticate(String authToken) {
        // construct a login URL that'll work for us
        String url = "/_ah/login?continue=http://localhost/&auth="+authToken;
        ResultWrapper resp = ApiClient.request("GET", url);
        int statusCode = resp.getResponse().getStatusLine().getStatusCode();
        if (statusCode != 302) {
            log.warn("Authentication failure: {}", resp.getResponse().getStatusLine());
            return null;
        }

        String authCookieValue = null;
        for(Header h : resp.getResponse().getHeaders("Set-Cookie")) {
            for(String nvp: h.getValue().split(";")) {
                String[] nameValue = nvp.split("=", 2);
                if (nameValue.length != 2) {
                    continue;
                }

                if (nameValue[0].trim().equalsIgnoreCase("SACSID")) {
                    authCookieValue = nameValue[1].trim();
                }
            }
        }

        if (authCookieValue == null) {
            log.warn("Authentication failure: no SACSID cookie found");
            return null;
        }

        String authCookie = "SACSID="+authCookieValue;
        log.info("Authenticated: {}", authCookie);

        return authCookie;
    }
}
