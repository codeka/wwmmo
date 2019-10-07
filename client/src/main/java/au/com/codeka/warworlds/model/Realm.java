package au.com.codeka.warworlds.model;

import java.net.URI;
import java.net.URISyntaxException;

import au.com.codeka.warworlds.GlobalOptions;
import au.com.codeka.warworlds.api.Authenticator;

public class Realm {
  private int id;
  private URI originalBaseUrl;
  private URI baseUrl;
  private String displayName;
  private String description;
  private Authenticator authenticator;

  public Realm(int id, String baseUrl, String displayName, String description)
      throws URISyntaxException {
    this.id = id;
    this.originalBaseUrl = this.baseUrl = new URI(baseUrl);
    this.displayName = displayName;
    this.description = description;
    authenticator = new Authenticator();
  }

  public int getID() {
    return id;
  }

  public URI getBaseUrl() {
    return baseUrl;
  }

  public void update() {
    boolean nonSecureServerConnection = new GlobalOptions().nonSecureServerConnection();

    String url = originalBaseUrl.toString();
    if (nonSecureServerConnection) {
      url = url.replace("https:", "http:");
    }
    try {
      baseUrl = new URI(url);
    } catch (URISyntaxException e) {
      baseUrl = originalBaseUrl;
    }
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDescription() {
    return description;
  }

  public Authenticator getAuthenticator() {
    return authenticator;
  }
}
