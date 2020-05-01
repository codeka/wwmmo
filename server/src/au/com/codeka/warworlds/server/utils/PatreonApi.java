package au.com.codeka.warworlds.server.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.Configuration;
import au.com.codeka.warworlds.server.RequestException;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * The "official" java library is totally broken, so I wrote my own simple wrapper.
 *
 * <p>Unfortunately, the API is based on JSONAPI which is utterly horrid. We try our best though.
 */
public class PatreonApi {
  private static final Log log = new Log("PatreonApi");

  private Configuration.PatreonConfig config;
  private OkHttpClient httpClient;

  public static class TokensResponse {
    @Expose @SerializedName("access_token") String accessToken;
    @Expose @SerializedName("refresh_token") String refreshToken;
    @Expose @SerializedName("expires_in") int expiresIn;
    @Expose @SerializedName("scope") String scope;
    @Expose @SerializedName("token_type") String tokenType;

    public String getAccessToken() {
      return accessToken;
    }

    public String getRefreshToken() {
      return refreshToken;
    }

    public int getExpiresIn() {
      return expiresIn;
    }

    public String getScope() {
      return scope;
    }

    public String getTokenType() {
      return tokenType;
    }
  }

  public static class UserPledge {
    private int amountCents;
    private DateTime since;
    private DateTime declinedSince;

    public int getAmountCents() {
      return amountCents;
    }

    public DateTime getSince() {
      return since;
    }

    public DateTime getDeclinedSince() {
      return declinedSince;
    }
  }

  public static class UserResponse {
    private String about;
    private String email;
    private String name;
    private String discordId;
    private String url;
    private String imageUrl;

    private ArrayList<UserPledge> pledges = new ArrayList<>();

    public UserResponse(JsonObject json) {
      JsonObject data = json.getAsJsonObject("data");
      JsonObject attributes = data.getAsJsonObject("attributes");
      about = attributes.get("about").isJsonNull() ? "" : attributes.get("about").getAsString();
      email = attributes.get("email").isJsonNull() ? "" : attributes.get("email").getAsString();
      name = attributes.get("full_name").isJsonNull() ? "" : attributes.get("full_name").getAsString();
      discordId = attributes.get("discord_id").isJsonNull() ? null : attributes.get("discord_id").getAsString();
      url = attributes.get("url").isJsonNull() ? null : attributes.get("url").getAsString();
      imageUrl = attributes.get("image_url").isJsonNull() ? null : attributes.get("image_url").getAsString();

      JsonArray pledges = data.get("relationships").getAsJsonObject().get("pledges")
          .getAsJsonObject().get("data").getAsJsonArray();
      if (json.get("included") == null) {
        // No included data means no pledges.
        return;
      }

      JsonArray included = json.get("included").getAsJsonArray();
      for (int i = 0; i < pledges.size(); i++) {
        String id = pledges.get(i).getAsJsonObject().get("id").getAsString();

        for (int j = 0; j < included.size(); j++) {
          if (!id.equals(included.get(i).getAsJsonObject().get("id").getAsString())) {
            continue;
          }

          UserPledge pledge = new UserPledge();
          JsonObject pledgeAttrs =
              included.get(i).getAsJsonObject().get("attributes").getAsJsonObject();
          pledge.amountCents = pledgeAttrs.get("amount_cents").getAsInt();
          pledge.since = DateTime.parse(pledgeAttrs.get("created_at").getAsString());
          if (pledgeAttrs.get("declined_since") != null &&
              !pledgeAttrs.get("declined_since").isJsonNull()) {
            pledge.declinedSince = DateTime.parse(pledgeAttrs.get("declined_since").getAsString());
          }
          this.pledges.add(pledge);
        }
      }
    }

    public String getAbout() {
      return about;
    }

    public String getEmail() {
      return email;
    }

    public String getName() {
      return name;
    }

    public String getDiscordId() {
      return discordId;
    }

    public String getUrl() {
      return url;
    }

    public String getImageUrl() {
      return imageUrl;
    }

    public List<UserPledge> getPledges() {
      return pledges;
    }
  }

  public PatreonApi() {
    config = Configuration.i.getPatreon();
    httpClient = new OkHttpClient();
  }

  public TokensResponse getTokens(String code) throws RequestException {
    FormBody body = new FormBody.Builder()
        .add("code", code)
        .add("grant_type", "authorization_code")
        .add("client_id", config.getClientId())
        .add("client_secret", config.getClientSecret())
        .add("redirect_uri", config.getRedirectUri())
        .build();
    Request request = new Request.Builder()
        .url("https://www.patreon.com/api/oauth2/token")
        .post(body)
        .build();
    try (Response response = httpClient.newCall(request).execute()) {
      return handleResponse("getTokens", response, TokensResponse.class);
    } catch (IOException e) {
      throw new RequestException(e);
    }
  }

  public TokensResponse refreshTokens(String refreshToken) throws RequestException {
    FormBody body = new FormBody.Builder()
        .add("grant_type", "refresh_token")
        .add("refresh_token", refreshToken)
        .add("client_id", config.getClientId())
        .add("client_secret", config.getClientSecret())
        .build();
    Request request = new Request.Builder()
        .url("https://www.patreon.com/api/oauth2/token")
        .post(body)
        .build();
    try (Response response = httpClient.newCall(request).execute()) {
      return handleResponse("refreshTokens", response, TokensResponse.class);
    } catch (IOException e) {
      throw new RequestException(e);
    }
  }

  public UserResponse fetchUser(String accessToken) throws RequestException {
    Request request = new Request.Builder()
        .url("https://www.patreon.com/api/oauth2/api/current_user")
        .addHeader("Authorization", "Bearer " + accessToken)
        .get()
        .build();
    try (Response response = httpClient.newCall(request).execute()) {
      return new UserResponse(handleResponse("fetchUser", response));
    } catch (IOException e) {
      throw new RequestException(e);
    }
  }

  private <T> T handleResponse(
      String methodName, Response response, Class<T> cls) throws RequestException, IOException {
    ResponseBody respBody = response.body();
    if (response.code() != 200) {
      log.warning("Error from Patreon API: %d %s\n%s",
          response.code(), response.message(), respBody == null ? "" : respBody.string());
      throw new RequestException(
          400,
          String.format("%s returned error: %d %s",
              methodName, response.code(), response.message()));
    }

    String json = respBody == null ? "" : respBody.string();

    Gson gson = new GsonBuilder().create();
    return gson.fromJson(json, cls);
  }

  private JsonObject handleResponse(
      String methodName, Response response) throws RequestException, IOException {
    ResponseBody respBody = response.body();
    if (response.code() != 200) {
      log.warning("Error from Patreon API: %d %s\n%s",
          response.code(), response.message(), respBody == null ? "" : respBody.string());
      throw new RequestException(
          400,
          String.format("%s returned error: %d %s",
              methodName, response.code(), response.message()));
    }

    String json = respBody == null ? "" : respBody.string();

    return JsonParser.parseString(json).getAsJsonObject();
  }
}
