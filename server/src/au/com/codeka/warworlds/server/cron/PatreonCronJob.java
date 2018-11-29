package au.com.codeka.warworlds.server.cron;

import com.patreon.PatreonOAuth;

import org.joda.time.DateTime;

import java.util.ArrayList;

import au.com.codeka.warworlds.server.Configuration;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.PatreonController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.PatreonInfo;

/**
 * Goes through the patreon table, and makes sure everything's up-to-date.
 */
public class PatreonCronJob extends CronJob {
  @Override
  public void run(String extra) throws Exception {
    ArrayList<PatreonInfo> patreonInfos = new ArrayList<>();
    String sql = "SELECT * from patreon";
    try (SqlStmt stmt = DB.prepare(sql)) {
      SqlResult res = stmt.select();
      while (res.next()) {
        patreonInfos.add(PatreonInfo.from(res));
      }
    } catch (Exception e) {
      throw new RequestException(e);
    }

    Configuration.PatreonConfig patreonConfig = Configuration.i.getPatreon();
    PatreonOAuth oauthClient =
        new PatreonOAuth(
            patreonConfig.getClientId(),
            patreonConfig.getClientSecret(),
            patreonConfig.getRedirectUri());

    for (PatreonInfo patreonInfo : patreonInfos) {
      if (patreonInfo.getTokenExpiryTime().getMillis() < 5000000L) {
        patreonInfo = patreonInfo.newBuilder().tokenExpiryTime(DateTime.now()).build();
      }

      // If the token's going to expire, refresh it.
      if (patreonInfo.getTokenExpiryTime().isBefore(DateTime.now().plusDays(7))) {
        PatreonOAuth.TokensResponse tokens =
            oauthClient.refreshTokens(patreonInfo.getRefreshToken());
        patreonInfo = patreonInfo.newBuilder()
            .accessToken(tokens.getAccessToken())
            .refreshToken(tokens.getRefreshToken())
            .tokenExpiryTime(DateTime.now().plusSeconds(tokens.getExpiresIn()))
            .tokenType(tokens.getTokenType())
            .tokenScope(tokens.getScope())
            .build();
      }

      // Otherwise, just refresh the pledges.
      new PatreonController().updatePatreonInfo(patreonInfo);
    }
  }
}
