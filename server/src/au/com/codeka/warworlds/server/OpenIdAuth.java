package au.com.codeka.warworlds.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.openid4java.OpenIDException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.ax.FetchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for OpenID authentication.
 */
public class OpenIdAuth {
    private static final Logger log = LoggerFactory.getLogger(OpenIdAuth.class);

    private static ConsumerManager mManager;
    private static DiscoveryInformation mDiscoveryInformation;
    static {
        mManager = new ConsumerManager();
    }

    private static DiscoveryInformation getDiscoveryInformation() throws DiscoveryException {
        if (mDiscoveryInformation == null) {
            @SuppressWarnings("rawtypes")
            List discoveries = mManager.discover("https://www.google.com/accounts/o8/id");
            mDiscoveryInformation = mManager.associate(discoveries);
        }
        return mDiscoveryInformation;
    }

    private static String getRequestUrl(HttpServletRequest request) {
        URI requestURI = null;
        try {
            requestURI = new URI(request.getRequestURL().toString());
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

    public static String getAuthenticateUrl(HttpServletRequest request,
                                            String returnUrl) throws RequestException {
        // note: Google is the only provider we support
        try {
            DiscoveryInformation discovered = getDiscoveryInformation();

            // make a request to get the email address
            AuthRequest authRequest = mManager.authenticate(discovered, returnUrl);
            FetchRequest fetch = FetchRequest.createFetchRequest();
            fetch.addAttribute("email",
                    // attribute alias
                    "http://schema.openid.net/contact/email",   // type URI
                    true);                                      // required
            authRequest.addExtension(fetch);

            return authRequest.getDestinationUrl(true);
        } catch (OpenIDException e) {
            throw new RequestException(e);
        }
    }

    public static String getAuthenticatedEmail(HttpServletRequest request) throws RequestException {
        try {
            ParameterList response = new ParameterList(request.getParameterMap());
            DiscoveryInformation discovered = getDiscoveryInformation();

            // extract the receiving URL from the HTTP request
            String requestUrl = getRequestUrl(request);
            String queryString = request.getQueryString();
            if (queryString != null && queryString.length() > 0)
                requestUrl += "?" + request.getQueryString();

            // verify the response; ConsumerManager needs to be the same
            // (static) instance used to place the authentication request
            VerificationResult verification = mManager.verify(requestUrl, response, discovered);

            // examine the verification result and extract the verified identifier
            Identifier verified = verification.getVerifiedId();
            if (verified != null) {
                AuthSuccess authSuccess = (AuthSuccess) verification.getAuthResponse();

                if (authSuccess.hasExtension(AxMessage.OPENID_NS_AX)) {
                    FetchResponse fetchResp = (FetchResponse) authSuccess.getExtension(AxMessage.OPENID_NS_AX);
                    return (String) fetchResp.getAttributeValues("email").get(0);
                }
            } else {
                log.error("No verified!");
            }

            throw new RequestException(403);
        }
        catch (OpenIDException e) {
            throw new RequestException(e);
        }
    }
}
