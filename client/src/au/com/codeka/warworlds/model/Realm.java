package au.com.codeka.warworlds.model;

import java.net.URI;
import java.net.URISyntaxException;

public class Realm {
    private int mID;
    private URI mBaseUrl;
    private String mDisplayName;
    private String mDescription;
    private AuthenticationMethod mAuthenticationMethod;
    private boolean mIsAlpha;

    public enum AuthenticationMethod {
        Default,
        AppEngine,
        LocalAppEngine
    }

    public Realm(int id, String baseUrl, String displayName, String description,
                 AuthenticationMethod authMethod, boolean isAlpha) throws URISyntaxException {
        mID = id;
        mBaseUrl = new URI(baseUrl);
        mDisplayName = displayName;
        mDescription = description;
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
    public int getID() {
        return mID;
    }
    public URI getBaseUrl() {
        return mBaseUrl;
    }
    public String getDisplayName() {
        return mDisplayName;
    }
    public String getDescription() {
        return mDescription;
    }
    public AuthenticationMethod getAuthentciationMethod() {
        return mAuthenticationMethod;
    }
}
