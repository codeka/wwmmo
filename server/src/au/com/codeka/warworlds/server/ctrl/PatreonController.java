package au.com.codeka.warworlds.server.ctrl;

import javax.annotation.Nullable;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.PatreonInfo;
import au.com.codeka.warworlds.server.utils.PatreonApi;

public class PatreonController {
  private static final Log log = new Log("PatreonController");
  private DataBase db;

  public PatreonController() {
    db = new DataBase();
  }

  public PatreonController(Transaction trans) {
    db = new DataBase(trans);
  }

  public void savePatreonInfo(PatreonInfo patreonInfo) throws RequestException {
    db.savePatreonInfo(patreonInfo);
  }

  @Nullable
  public PatreonInfo getPatreonInfo(long empireID) throws RequestException {
    return db.getPatreonInfo(empireID);
  }

  public void updatePatreonInfo(PatreonInfo patreonInfo) throws RequestException {
    log.info("Refreshing Patreon pledges for %d.", patreonInfo.getEmpireId());

    PatreonApi api = new PatreonApi();
    PatreonApi.UserResponse user = api.fetchUser(patreonInfo.getAccessToken());

    int maxPledge = 0;
    for (PatreonApi.UserPledge pledge : user.getPledges()) {
      if (pledge.getAmountCents() > maxPledge) {
        maxPledge = pledge.getAmountCents();
      }
    }

    patreonInfo = patreonInfo.newBuilder()
        .fullName(user.getName())
        .about(user.getAbout())
        .discordId(user.getDiscordId())
        .patreonUrl(user.getUrl())
        .email(user.getEmail())
        .imageUrl(user.getImageUrl())
        .maxPledge(maxPledge)
        .build();
    savePatreonInfo(patreonInfo);
  }

  private static class DataBase extends BaseDataBase {
    public DataBase() {
      super();
    }

    public DataBase(Transaction trans) {
      super(trans);
    }

    void savePatreonInfo(PatreonInfo patreonInfo) throws RequestException {
      String sql;
      if (patreonInfo.getId() == 0) {
        sql = "INSERT INTO patreon (empire_id, access_token, refresh_token, token_type," +
            " token_scope, token_expiry_time, patreon_url, full_name, discord_id, about," +
            " image_url, email, max_pledge) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
      } else {
        sql = "UPDATE patreon SET" +
            " empire_id = ?," +
            " access_token = ?," +
            " refresh_token = ?," +
            " token_type = ?," +
            " token_scope = ?," +
            " token_expiry_time = ?," +
            " patreon_url = ?," +
            " full_name = ?," +
            " discord_id = ?," +
            " about = ?," +
            " image_url = ?," +
            " email = ?," +
            " max_pledge = ?" +
            " WHERE id = ?";
      }
      try (SqlStmt stmt = DB.prepare(sql)) {
        stmt.setLong(1, patreonInfo.getEmpireId());
        stmt.setString(2, patreonInfo.getAccessToken());
        stmt.setString(3, patreonInfo.getRefreshToken());
        stmt.setString(4, patreonInfo.getTokenType());
        stmt.setString(5, patreonInfo.getTokenScope());
        stmt.setLong(6, patreonInfo.getTokenExpiryTime().getMillis());
        stmt.setString(7, patreonInfo.getPatreonUrl());
        stmt.setString(8, patreonInfo.getFullName());
        stmt.setString(9, patreonInfo.getDiscordId());
        stmt.setString(10, patreonInfo.getAbout());
        stmt.setString(11, patreonInfo.getImageUrl());
        stmt.setString(12, patreonInfo.getEmail());
        stmt.setInt(13, patreonInfo.getMaxPledge());
        if (patreonInfo.getId() != 0) {
          stmt.setLong(14, patreonInfo.getId());
        }
        stmt.update();
      } catch (Exception e) {
        throw new RequestException(e);
      }
    }

    @Nullable
    PatreonInfo getPatreonInfo(long empireID) throws RequestException {
      String sql = "SELECT * from patreon WHERE empire_id = ?";
      try (SqlStmt stmt = DB.prepare(sql)) {
        stmt.setLong(1, empireID);
        SqlResult res = stmt.select();
        if (res.next()) {
          return PatreonInfo.from(res);
        }
      } catch (Exception e) {
        throw new RequestException(e);
      }

      return null;
    }
  }
}
