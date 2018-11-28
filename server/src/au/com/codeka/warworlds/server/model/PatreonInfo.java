package au.com.codeka.warworlds.server.model;

import org.joda.time.DateTime;

import java.sql.SQLException;

import au.com.codeka.warworlds.server.data.SqlResult;

public class PatreonInfo {
  private final long id;
  private final long empireId;
  private final String accessToken;
  private final String refreshToken;
  private final String tokenType;
  private final String tokenScope;
  private final DateTime tokenExpiryTime;
  private final String patreonUrl;
  private final String fullName;
  private final String discordId;
  private final String about;
  private final String imageUrl;
  private final String email;
  private final int maxPledge;

  private PatreonInfo(Builder builder) {
    this.id = builder.id;
    this.empireId = builder.empireId;
    this.accessToken = builder.accessToken;
    this.refreshToken = builder.refreshToken;
    this.tokenType = builder.tokenType;
    this.tokenScope = builder.tokenScope;
    this.tokenExpiryTime = builder.tokenExpiryTime;
    this.patreonUrl = builder.patreonUrl;
    this.fullName = builder.fullName;
    this.discordId = builder.discordId;
    this.about = builder.about;
    this.imageUrl = builder.imageUrl;
    this.email = builder.email;
    this.maxPledge = builder.maxPledge;
  }

  public static PatreonInfo from(SqlResult res) throws SQLException {
    return builder()
        .id(res.getLong("id"))
        .empireId(res.getLong("empire_id"))
        .accessToken(res.getString("access_token"))
        .refreshToken(res.getString("refresh_token"))
        .tokenType(res.getString("token_type"))
        .tokenScope(res.getString("token_scope"))
        .tokenExpiryTime(new DateTime(res.getLong("token_expiry_time")))
        .patreonUrl(res.getString("patreon_url"))
        .fullName(res.getString("full_name"))
        .discordId(res.getString("discord_id"))
        .about(res.getString("about"))
        .imageUrl(res.getString("image_url"))
        .email(res.getString("email"))
        .maxPledge(res.getInt("max_pledge"))
        .build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder newBuilder() {
    return new Builder(this);
  }

  public long getId() {
    return id;
  }

  public long getEmpireId() {
    return empireId;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public String getTokenType() {
    return tokenType;
  }

  public String getTokenScope() {
    return tokenScope;
  }

  public DateTime getTokenExpiryTime() {
    return tokenExpiryTime;
  }

  public String getPatreonUrl() {
    return patreonUrl;
  }

  public String getFullName() {
    return fullName;
  }

  public String getDiscordId() {
    return discordId;
  }

  public String getAbout() {
    return about;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public String getEmail() {
    return email;
  }

  public int getMaxPledge() {
    return maxPledge;
  }

  public static class Builder {
    private long id;
    private long empireId;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private String tokenScope;
    private DateTime tokenExpiryTime;
    private String patreonUrl;
    private String fullName;
    private String discordId;
    private String about;
    private String imageUrl;
    private String email;
    private int maxPledge;

    private Builder() {
    }

    private Builder(PatreonInfo patreonInfo) {
      this.id = patreonInfo.id;
      this.empireId = patreonInfo.empireId;
      this.accessToken = patreonInfo.accessToken;
      this.refreshToken = patreonInfo.refreshToken;
      this.tokenType = patreonInfo.tokenType;
      this.tokenScope = patreonInfo.tokenScope;
      this.tokenExpiryTime = patreonInfo.tokenExpiryTime;
      this.patreonUrl = patreonInfo.patreonUrl;
      this.fullName = patreonInfo.fullName;
      this.discordId = patreonInfo.discordId;
      this.about = patreonInfo.about;
      this.imageUrl = patreonInfo.imageUrl;
      this.email = patreonInfo.email;
      this.maxPledge = patreonInfo.maxPledge;
    }

    Builder id(long id) {
      this.id = id;
      return this;
    }

    public Builder empireId(long empireId) {
      this.empireId = empireId;
      return this;
    }

    public Builder accessToken(String accessToken) {
      this.accessToken = accessToken;
      return this;
    }

    public Builder refreshToken(String refreshToken) {
      this.refreshToken = refreshToken;
      return this;
    }

    public Builder tokenType(String tokenType) {
      this.tokenType = tokenType;
      return this;
    }

    public Builder tokenScope(String tokenScope) {
      this.tokenScope = tokenScope;
      return this;
    }

    public Builder tokenExpiryTime(DateTime tokenExpiryTime) {
      this.tokenExpiryTime = tokenExpiryTime;
      return this;
    }

    public Builder patreonUrl(String patreonUrl) {
      this.patreonUrl = patreonUrl;
      return this;
    }

    public Builder fullName(String fullName) {
      this.fullName = fullName;
      return this;
    }

    public Builder discordId(String discordId) {
      this.discordId = discordId;
      return this;
    }

    public Builder about(String about) {
      this.about = about;
      return this;
    }

    public Builder imageUrl(String imageUrl) {
      this.imageUrl = imageUrl;
      return this;
    }

    public Builder email(String email) {
      this.email = email;
      return this;
    }

    public Builder maxPledge(int maxPledge) {
      this.maxPledge = maxPledge;
      return this;
    }

    public PatreonInfo build() {
      return new PatreonInfo(this);
    }
  }
}
