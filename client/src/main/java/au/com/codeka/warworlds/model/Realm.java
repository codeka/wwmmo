package au.com.codeka.warworlds.model;

import java.net.URI;
import java.net.URISyntaxException;

import au.com.codeka.warworlds.api.Authenticator;

public class Realm {
    private int mID;
    private URI mBaseUrl;
    private String mDisplayName;
    private String mDescription;
    private Authenticator mAuthenticator;

    public Realm(int id, String baseUrl, String displayName, String description)
            throws URISyntaxException {
        mID = id;
        mBaseUrl = new URI(baseUrl);
        mDisplayName = displayName;
        mDescription = description;
        mAuthenticator = new Authenticator();
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

    public Authenticator getAuthenticator() {
        return mAuthenticator;
    }
}
