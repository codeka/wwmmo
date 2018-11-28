package au.com.codeka.warworlds.server.ctrl;

import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.patreon.PatreonAPI;
import com.patreon.resources.Pledge;
import com.patreon.resources.User;

import java.io.IOException;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.PatreonInfo;

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
    try {
      db.savePatreonInfo(patreonInfo);
    } catch (Exception e) {
      throw new RequestException(e);
    }
  }

  public void updatePatreonInfo(PatreonInfo patreonInfo) throws RequestException {
    log.info("Refreshing Patreon pledges for %d.",patreonInfo.getEmpireId());

    PatreonAPI apiClient = new PatreonAPI(patreonInfo.getAccessToken());
    User user;
    try {
      JSONAPIDocument<User> userJson = apiClient.fetchUser();
      user = userJson.get();
    } catch (IOException e) {
      throw new RequestException(e);
    }

    int maxPledge = 0;
    if (user.getPledges() != null) {
      for (Pledge pledge : user.getPledges()) {
        if (pledge.getAmountCents() > maxPledge) {
          maxPledge = pledge.getAmountCents();
        }
      }
    }

    patreonInfo = patreonInfo.newBuilder()
        .fullName(user.getFullName())
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

    public void savePatreonInfo(PatreonInfo patreonInfo) throws RequestException {
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
      } catch (Exception e) {
        throw new RequestException(e);
      }
    }
  }
}
