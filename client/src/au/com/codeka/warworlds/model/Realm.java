package au.com.codeka.warworlds.model;

import java.net.URI;
import java.net.URISyntaxException;

public class Realm {
    private URI mBaseUrl;
    private String mDisplayName;
    private AuthenticationMethod mAuthenticationMethod;

    public enum AuthenticationMethod {
        Default,
        AppEngine,
        LocalAppEngine
    }

    public Realm(String baseUrl, String displayName, AuthenticationMethod authMethod)
            throws URISyntaxException {
        mBaseUrl = new URI(baseUrl);
        mDisplayName = displayName;
        mAuthenticationMethod = authMethod;
    }

    public URI getBaseUrl() {
        return mBaseUrl;
    }
    public String getDisplayName() {
        return mDisplayName;
    }
    public AuthenticationMethod getAuthentciationMethod() {
        return mAuthenticationMethod;
    }
}
