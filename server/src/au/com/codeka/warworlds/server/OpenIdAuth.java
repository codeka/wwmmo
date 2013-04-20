package au.com.codeka.warworlds.server;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;

import org.expressme.openid.Association;
import org.expressme.openid.Authentication;
import org.expressme.openid.Endpoint;
import org.expressme.openid.OpenIdManager;

/**
 * Helper class for OpenID authentication.
 */
public class OpenIdAuth {
    private static OpenIdManager sManager;
    private static Endpoint sEndPoint;
    private static Association sAssociation;

    public static String getAuthenticateUrl(HttpServletRequest request) {
        if (sManager == null) {
            sManager = new OpenIdManager();
            URI requestURI = null;
            try {
                requestURI = new URI(request.getRequestURL().toString());
            } catch (URISyntaxException e) {
                return null; // should never happen!
            }
            sManager.setReturnTo(requestURI.resolve("/login").toString());
            sManager.setRealm(requestURI.resolve("/").toString());

            sEndPoint = sManager.lookupEndpoint("Google");
            sAssociation = sManager.lookupAssociation(sEndPoint);
        }

        return sManager.getAuthenticationUrl(sEndPoint, sAssociation);
    }

    public static String getAuthenticatedEmail(HttpServletRequest request) {
        Authentication auth = sManager.getAuthentication(request, sAssociation.getRawMacKey(), "ext1");
        return auth.getEmail();
    }
}
