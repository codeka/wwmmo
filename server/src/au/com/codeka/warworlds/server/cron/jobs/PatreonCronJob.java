package au.com.codeka.warworlds.server.cron.jobs;

import com.patreon.PatreonOAuth;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Locale;

import au.com.codeka.warworlds.server.Configuration;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.cron.AbstractCronJob;
import au.com.codeka.warworlds.server.cron.CronJob;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.ctrl.PatreonController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Empire;
import au.com.codeka.warworlds.server.model.PatreonInfo;

/**
 * Goes through the patreon table, and makes sure everything's up-to-date.
 */
@CronJob(name = "Patreon", desc = "Makes sure the Patreon associations are up-to-date.")
public class PatreonCronJob extends AbstractCronJob {
  @Override
  public String run(String extra) throws Exception {
    ArrayList<String> errors = new ArrayList<>();
    int numSuccessful = 0;

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
      try {
        new PatreonController().updatePatreonInfo(patreonInfo);
      } catch (RequestException e) {

        Empire empire = new EmpireController().getEmpire((int) patreonInfo.getEmpireId());
        String msg = String.format(Locale.ENGLISH,
            "[%d] %s (%s)\n%s",
            patreonInfo.getEmpireId(),
            empire.getDisplayName(),
            patreonInfo.getEmail(),
            e.getMessage());
        errors.add(msg);
      }

      numSuccessful ++;
    }

    if (errors.size() == 0) {
      return String.format(Locale.ENGLISH, "%d patreon infos\nsuccessfully updated.", numSuccessful);
    } else {
      return String.format(Locale.ENGLISH,
          "%d successfully updated, %d errors.\n\n%s",
          numSuccessful,
          errors.size(),
          String.join("\n\n", errors));
    }
  }
}
