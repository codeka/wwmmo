package au.com.codeka.warworlds.model;

import java.net.URI;
import java.net.URISyntaxException;

public class Realm {
    private URI mBaseUrl;
    private String mDisplayName;
    private AuthenticationMethod mAuthenticationMethod;
    private boolean mIsAlpha;

    public enum AuthenticationMethod {
        Default,
        AppEngine,
        LocalAppEngine
    }

    public Realm(String baseUrl, String displayName, AuthenticationMethod authMethod, boolean isAlpha)
            throws URISyntaxException {
        mBaseUrl = new URI(baseUrl);
        mDisplayName = displayName;
        mAuthenticationMethod = authMethod;
        mIsAlpha = isAlpha;
    }

    /**
     * A couple of places rely on knowing whether they're talking to the alpha realm or
     * not to adjust various calculations.
     * @return
     */
    public boolean isAlpha() {
        return mIsAlpha;
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
