package au.com.codeka.warworlds.server.cron.jobs;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Locale;

import au.com.codeka.common.Log;
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
import au.com.codeka.warworlds.server.utils.PatreonApi;

/**
 * Goes through the patreon table, and makes sure everything's up-to-date.
 */
@CronJob(name = "Patreon", desc = "Makes sure the Patreon associations are up-to-date.")
public class PatreonCronJob extends AbstractCronJob {
  private static final Log log = new Log("PatreonCronJob");

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

    PatreonApi patreonApi = new PatreonApi();

    for (PatreonInfo patreonInfo : patreonInfos) {
      log.info("Updating patreon info for #%d [email=%s] [token_expiry_time=%s] [refresh_token=%s]",
          patreonInfo.getEmpireId(),
          patreonInfo.getEmail(),
          patreonInfo.getTokenExpiryTime(),
          patreonInfo.getRefreshToken());

      if (patreonInfo.getTokenExpiryTime().getMillis() < 5000000L) {
        patreonInfo = patreonInfo.newBuilder().tokenExpiryTime(DateTime.now()).build();
      }

      // If the token's going to expire, refresh it.
      try {
        if (patreonInfo.getTokenExpiryTime().isBefore(DateTime.now().plusDays(7))) {
          log.info(" - access token has expired, attempting to refresh.");
          PatreonApi.TokensResponse tokens =
              patreonApi.refreshTokens(patreonInfo.getRefreshToken());
          patreonInfo = patreonInfo.newBuilder()
              .accessToken(tokens.getAccessToken())
              .refreshToken(tokens.getRefreshToken())
              .tokenExpiryTime(DateTime.now().plusSeconds(tokens.getExpiresIn()))
              .tokenType(tokens.getTokenType())
              .tokenScope(tokens.getScope())
              .build();
        }
      } catch (Exception e) {
        log.warning("Error refreshing access token, cannot continue.", e);
        Empire empire = new EmpireController().getEmpire((int) patreonInfo.getEmpireId());
        String msg = String.format(Locale.ENGLISH,
            "[%d] %s (%s)\n%s",
            patreonInfo.getEmpireId(),
            empire == null ? "??" : empire.getDisplayName(),
            patreonInfo.getEmail(),
            e.getMessage());
        errors.add(msg);
      }

      // Otherwise, just refresh the pledges.
      try {
        new PatreonController().updatePatreonInfo(patreonInfo);
        numSuccessful ++;
      } catch (Exception e) {
        log.warning("Error updating patreon info.", e);
        Empire empire = new EmpireController().getEmpire((int) patreonInfo.getEmpireId());
        String msg = String.format(Locale.ENGLISH,
            "[%d] %s (%s)\n%s",
            patreonInfo.getEmpireId(),
            empire == null ? "??" : empire.getDisplayName(),
            patreonInfo.getEmail(),
            e.getMessage());
        errors.add(msg);
      }
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
